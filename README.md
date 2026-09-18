# Mendix Multi-Broker AMQP Connector

A config-driven, bidirectional AMQP 1.0 connector for Mendix that talks to
**Solace Cloud**, **Solace PubSub+ self-managed** software brokers, and
**Apache ActiveMQ** through one code path, because all three speak the same
AMQP 1.0 wire protocol. Switching broker is a configuration change (pick a
`BrokerType` and fill in host/credentials), not a code change.

This has not been built/run yet in this repo - the steps below are exactly
what to do next, in order.

## Project layout

```
mend-a-broker/
├── connector/       Pure-Java AMQP client library (no Mendix dependency)
├── demo/            Standalone runnable demo (CLI); defaults to a local Solace
│                    PubSub+ broker running in Docker (see step 4 below)
├── mendix-module/   Mendix 11.12 LTS module scaffold: Java actions ready to
│                    paste into Studio Pro, plus the exact domain
│                    model/enumeration/microflow/page spec to build there
├── docs/            Architecture notes and a Marketplace submission checklist
├── .github/workflows/  CI: builds and smoke-tests on every push
├── .gitignore
├── LICENSE          Unlicense
└── pom.xml          Maven parent (builds connector + demo together)
```

---

## Step by step: getting this running

### 1. Get the code

If you're reading this from a clone you already have, skip to step 2.
Otherwise:

```bash
git clone https://github.com/taatuut/mend-a-broker.git
cd mend-a-broker
```

### 2. Install prerequisites (skip anything you already have)

- **Java 17 or newer** - check with `java -version`.
- **Maven 3.6+** - check with `mvn -version`.
- Both are free; on macOS `brew install openjdk@17 maven`, on Windows use the
  Temurin installer + the Maven zip from `maven.apache.org`, on Linux use
  your package manager (`apt install openjdk-17-jdk maven` or similar).

### 3. Build everything

```bash
mvn -DskipTests package
```

This compiles `connector/` and `demo/`, downloading Apache Qpid JMS and
(for the demo only) an embedded ActiveMQ Artemis broker from Maven Central.
First run takes a minute or two while dependencies download; later runs are
fast.

If this fails with a network/proxy error, see "Troubleshooting" below.

### 4. Start a local Solace broker (Docker) and run the demo

The demo needs a real broker to talk to. The quickest one to stand up
locally is a Solace PubSub+ software broker in Docker:

```bash
docker run -d --name mend-a-broker-solace \
  -p 8080:8080 -p 5672:5672 -p 5550:5550 \
  --shm-size=1g \
  --env username_admin_globalaccesslevel=admin \
  --env username_admin_password=admin \
  solace/solace-pubsub-standard

# wait for it to report healthy (takes 10-60s on first run)
until curl -sf http://localhost:5550/health-check/direct-active > /dev/null; do
  echo "waiting for broker..."; sleep 3
done
echo "broker is up"
```

Broker Manager UI (optional, for poking around): http://localhost:8080
(admin/admin). No further setup is needed - the broker's default Message
VPN ships with a `default`/`default` client-username/password that's
already open, which is what `demo/src/main/resources/broker-solace-docker-local.properties`
(used automatically below) points at.

If you already have a Solace broker running locally some other way (for
example via a `single-click-solace-demo.sh`-style script) and it's
listening on host port 5672, you can skip the `docker run` above and reuse
that instead - just make sure nothing else is bound to port 5672 first.

Now run the demo with no arguments:

```bash
java -jar demo/target/amqp-broker-demo.jar
```

You should see it connect, subscribe to a demo queue and topic, send
messages to both, and finish with:

```
=== RESULT: queue round-trip SUCCEEDED, topic round-trip SUCCEEDED ===
```

That proves the connector's send/receive logic works against a real broker,
independent of Mendix.

When you're done:

```bash
docker stop mend-a-broker-solace && docker rm mend-a-broker-solace
```

### Why the embedded ActiveMQ demo mode is disabled

This demo used to default to a zero-external-infrastructure embedded
ActiveMQ Artemis broker instead of the Docker Solace broker above. That
mode is temporarily disabled: as of the latest Artemis release (2.56.0),
starting an embedded broker on JDK 24+ crashes on startup with
`UnsupportedOperationException: getSubject is not supported`, thrown while
Artemis builds its JMX management MBean, because the deprecated
`java.security.Subject`/`AccessControlContext` APIs it still calls were
removed from the JDK. Apache's own tracking:
[ARTEMIS-5374](https://issues.apache.org/jira/browse/ARTEMIS-5374) (closed
as fixed, but the fix didn't fully hold) and
[ARTEMIS-5538](https://issues.apache.org/jira/browse/ARTEMIS-5538) (closed
as a duplicate, with a maintainer comment noting "Support on Java 24 is a
known issue" - i.e. still genuinely broken as of this writing).

This does **not** affect the connector or the Mendix module at all - neither
ever starts an Artemis server, they only ever connect out to whatever
broker you configure. It only affected the demo's own zero-setup
convenience mode, which is why switching the demo's default to a real
(Dockerized) Solace broker sidesteps the bug entirely, rather than working
around it.

The disabled code hasn't been deleted, just commented out, so it's easy to
bring back later:
- `demo/src/main/java/com/solace/mendix/amqp/demo/EmbeddedBrokerLauncher.java`
  - the whole class body is wrapped in a block comment.
- `demo/pom.xml` - the `artemis-server` / `artemis-amqp-protocol`
  dependencies are commented out.
- `demo/src/main/java/com/solace/mendix/amqp/demo/DemoApp.java` no longer
  references either of the above; re-wiring it back in (uncomment, restore
  the `useEmbedded` branch) is a small, self-contained change once Artemis
  genuinely fixes JDK 24+ support.

### 5. Point it at a real broker (Solace Cloud, Solace self-managed, or ActiveMQ)

Copy the template that matches the broker you have, fill in the real values,
and pass the file as an argument:

```bash
cp demo/src/main/resources/broker-solace-cloud.properties.example my-broker.properties
# edit my-broker.properties with your real host / username / password
java -jar demo/target/amqp-broker-demo.jar my-broker.properties
```

Templates are provided for all three:
`broker-solace-cloud.properties.example`,
`broker-solace-selfmanaged.properties.example`,
`broker-activemq-external.properties.example` (all under
`demo/src/main/resources/`). Each file has comments explaining exactly which
value to find where (e.g. Solace Cloud Console -> your service -> Connect ->
AMQP tab).

**Never commit a filled-in `.properties` file with real credentials** -
`.gitignore` already excludes plain `*.properties` files (the templates end
in `.properties.example`, so they're kept, and never name a real file
`my-broker.properties` inside a tracked path - keep it at the repo root or
somewhere `.gitignore` already covers).

### 6. Build the jar the Mendix module needs

```bash
cd connector
mvn -DskipTests package
cp target/amqp-broker-connector-1.0.0-with-dependencies.jar ../mendix-module/userlib/
cd ..
```

### 7. Assemble the Mendix module in Studio Pro

Open **`mendix-module/README-STUDIO-PRO-SETUP.md`** and follow it top to
bottom - it walks through creating the module, the domain model and
enumerations (spec in `mendix-module/README-DOMAIN-MODEL.md`), pasting the
six provided Java actions in, and wiring up demo microflows and pages. Budget
30-45 minutes the first time.

### 8. Before submitting to Mendix Marketplace

Work through **`docs/MARKETPLACE-CHECKLIST.md`**: no hard-coded credentials
in the exported module, the `Password` attribute marked confidential,
license notices for the bundled Apache-licensed libraries, listing
description/icon/screenshots, and testing the bidirectional round trip
against all three broker types before publishing.

### Troubleshooting

- **`mvn package` fails with a 403/connection error reaching
  `repo.maven.apache.org`**: you're on a network that blocks Maven Central
  (common on locked-down corporate VPNs/sandboxes). Try a different network,
  or ask your network/IT team to allow `repo.maven.apache.org` and
  `repo1.maven.org`, or point Maven at an internal Artifactory/Nexus mirror
  your org already allows (add a `<mirror>` in `~/.m2/settings.xml`).
- **`NoClassDefFoundError` for `org.apache.qpid...` inside Studio Pro**: you
  copied the plain jar instead of the `-with-dependencies` (shaded) one, or
  didn't run `mvn package` (only `package` triggers the shading step, not
  `compile`).
- **Looking for the old embedded-broker demo mode?** It's disabled for now -
  see "Why the embedded ActiveMQ demo mode is disabled" under step 4 above.
- Broker-specific connection issues (TLS, VPN/username format, ports) are
  covered in the comments of each `*.properties.example` file and in the
  "Troubleshooting" section of `mendix-module/README-STUDIO-PRO-SETUP.md`.

---

## Continuous integration

`.github/workflows/build-and-smoke-test.yml` builds both Maven modules,
starts a Solace PubSub+ broker as a GitHub Actions service container, and
runs the demo against it as a smoke test on every push and pull request, so
a change that breaks send/receive fails a check instead of shipping.
There's also an optional second job, off by default, that runs the same
demo against a real Solace Cloud service using GitHub Actions secrets
(`SOLACE_CLOUD_HOST` / `_USERNAME` / `_PASSWORD`) - flip it on by setting the
repo variable `RUN_REAL_BROKER_TESTS` to `true` once you have a Solace Cloud
trial/dev service to dedicate to CI. Consider also enabling Dependabot for
this repo, since Qpid JMS is on the TLS/auth path to production brokers.

## Design notes

See `docs/ARCHITECTURE.md` for why one connector implementation covers three
broker platforms, the connection-handle pattern used to bridge a live Java
connection into Mendix microflows, and how inbound messages get routed back
into a configurable callback microflow.
