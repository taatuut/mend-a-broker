# Mendix Marketplace submission checklist

Working backward from what a reviewer / the Marketplace intake process
actually checks, before submitting `AMQPConnector`:

## Content

- [ ] Module exported as a `.mpk` (App menu -> Export module package) built
      from an app on the Mendix version you're targeting (11.12 LTS).
- [ ] No hard-coded credentials, hostnames, or test data left in
      `BrokerConfiguration` instances inside the exported module.
- [ ] `Password` attribute confirmed **confidential** (Studio Pro encrypts it
      in the export and masks it in logs/inspection).
- [ ] Module works standalone in a blank test app (no dependency on the demo
      app's other modules) - verify by creating a fresh app and importing
      just the `.mpk`.
- [ ] `userlib` jar's licenses reviewed and compatible with redistribution:
      Apache Qpid JMS Client and SLF4J are both Apache License 2.0 - include
      their license text/notice per Marketplace content guidelines.

## Documentation (Marketplace listing + in-repo)

- [ ] Short description (what it does, one sentence): "Config-driven,
      bidirectional AMQP 1.0 connector for Solace Cloud, Solace PubSub+
      self-managed, and Apache ActiveMQ."
- [ ] Long description covering: supported brokers, supported Mendix
      versions, the six Java actions and their parameters, the
      `ConnectionId` handle pattern, and a link/reference to
      README-STUDIO-PRO-SETUP.md for setup.
- [ ] Category/tags: Connectivity, Integration, Messaging/AMQP, Solace,
      ActiveMQ.
- [ ] Icon (Marketplace requires a square PNG, typically 128x128 or larger)
      and at least one screenshot of the demo page in action.
- [ ] Versioning: start at 1.0.0 and follow semver for updates; note the
      supported Mendix version range explicitly (this module targets Mendix
      11.12 LTS; document the minimum Studio Pro version you actually tested
      against).
- [ ] Changelog file for future updates.

## Testing before publishing

- [ ] Bidirectional round trip verified against **all three** broker types
      you claim to support (this repo's `demo` module and Studio Pro demo
      page both exercise the same connector code - run both against real
      Solace Cloud, self-managed, and ActiveMQ instances).
- [ ] Reconnect behavior checked: what happens to `ConnectionId` and any
      active `StartListening` subscriptions if the broker restarts or the
      network blips (current implementation does not auto-reconnect - decide
      whether to add that before a 1.0 release, or document it as a known
      limitation with a suggested pattern: catch the `ExceptionListener`
      callback in a future version and re-run `ConnectBroker`).
- [ ] Load/soak test if the target use case is high message volume - the
      demo only proves correctness, not throughput.
- [ ] Security review of the Java code (this repo has no external HTTP
      calls, no reflection beyond Mendix's own proxy classes, no dynamic
      class loading beyond what Qpid JMS itself does internally).

## Support

- [ ] A support/contact channel listed on the Marketplace entry (GitHub
      issues, email, or your normal support process).
- [ ] Decide and document an update/maintenance cadence, since Marketplace
      listings are expected to stay compatible with new Mendix LTS releases.
