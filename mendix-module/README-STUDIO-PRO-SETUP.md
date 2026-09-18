# Assembling the AMQPConnector module in Mendix Studio Pro 11.12 LTS

This turns the files in this folder into a working module + demo app. Total
time: roughly 30-45 minutes the first time.

## 0. Prerequisites

- Mendix Studio Pro 11.12 LTS.
- The connector's shaded jar built and placed at
  `userlib/amqp-broker-connector-1.0.0-with-dependencies.jar` (see
  `userlib/README.txt` - build it with the standalone Maven project in
  `../connector`, on a machine with normal internet access to Maven Central).
- Real connection details for whichever broker(s) you want to test against
  (Solace Cloud service AMQP endpoint, a self-managed PubSub+ broker with its
  AMQP service enabled, and/or an ActiveMQ broker with an AMQP
  acceptor/transport connector enabled - see the `*.properties.example`
  files under `../demo/src/main/resources` for what each needs).

## 1. Create the app and module

1. Create a new blank Mendix app (or open the app you're adding this to).
2. **App menu -> Add module -> `AMQPConnector`**.
3. Follow `README-DOMAIN-MODEL.md` to create the two enumerations, two
   entities, and six Java actions inside this module. Do this before step 2
   below, since the Java action code references the generated proxies.

## 2. Add the connector jar

1. Copy `amqp-broker-connector-1.0.0-with-dependencies.jar` into this
   module's `userlib` folder inside the Studio Pro project directory (the
   same `userlib` folder this README lives next to).
2. In Studio Pro, right-click the project -> **Synchronize App Directory**
   (or just save; Studio Pro picks up new jars under `userlib` automatically).

## 3. Paste in the Java action implementations

For each of the six actions, open its generated `.java` file and replace the
`BEGIN USER CODE` / `END USER CODE` block with the matching file from
`javasource/AMQPConnector/actions` in this repo:

- `ConnectBroker.java`
- `DisconnectBroker.java`
- `SendMessage.java`
- `StartListening.java`
- `StopListening.java`
- `IsConnected.java`

Also copy the three plain helper classes - these are **not** Java actions, so
create them as regular Java classes under `javasource/AMQPConnector/impl`
(right-click the module in App Explorer -> "Show in App Explorer" if you
don't see javasource, then create the package/files directly, or drop the
files in via your OS file explorer and refresh):

- `impl/ConnectionRegistry.java`
- `impl/BrokerConfigMapper.java`
- `impl/DestinationMapper.java`

Save and let Studio Pro's Java compiler check the module (View -> "Errors").
The only errors you should see, if any, are typos introduced while
copy-pasting - the generated proxy classes (`amqpconnector.proxies.*`) are
created automatically from the domain model in step 1.

## 4. Demo microflows

Create a folder `AMQPConnector / Demo` and add:

### `SUB_OnMessageReceived` (microflow, used as a sub-microflow / callback)
Parameters: `ConnectionId` (String), `DestinationName` (String), `MessageId`
(String), `Payload` (String).
Body: create a `MessageLog` object, set `Direction` = Inbound, map the four
parameters onto `ConnectionId` / `DestinationName` / `MessageId` / `Payload`,
set `Timestamp` = `[%CurrentDateTime%]`, commit.

### `ACT_ConnectAndListen` (microflow, e.g. wired to a button)
1. Retrieve or create a `BrokerConfiguration` object (for a first test, add a
   "New" page - see step 5 - so you can fill one in from the running app
   instead of hard-coding values in the microflow).
2. Call **ConnectBroker** with that object -> store result in a (non-
   persistable) string variable `ConnectionId`. If you want the app to reuse
   the same connection across multiple button clicks, save `ConnectionId`
   into a session variable or a small non-persistable "ActiveConnection"
   entity instead of re-connecting every time.
3. Call **StartListening** with `ConnectionId`, `DestinationName` =
   `'mendix/amqp/demo/queue'`, `DestinationType` = `Queue`,
   `OnMessageMicroflow` = `'AMQPConnector.SUB_OnMessageReceived'`.
4. (Optional) Also `StartListening` on `DestinationType` = `Topic`,
   `DestinationName` = `'mendix/amqp/demo/topic'`, to demo publish/subscribe
   too.

### `ACT_SendTestMessage` (microflow, wired to a "Send" button)
Call **SendMessage** with the same `ConnectionId`, `DestinationName` =
`'mendix/amqp/demo/queue'`, `DestinationType` = `Queue`, and a `Payload`
taken from a text box on the page. Because `ACT_ConnectAndListen` already
subscribed to that same queue, sending a message you also receive it back and
`SUB_OnMessageReceived` logs it - a clean, self-contained bidirectional demo
even before you point it at colleagues or a second app.

### `ACT_Disconnect` (microflow, wired to a "Disconnect" button)
Call **StopListening** for each destination you subscribed to, then
**DisconnectBroker**.

## 5. Demo pages

1. **BrokerConfiguration_NewEdit**: a standard New/Edit page (generate one
   from the entity) so a user can fill in and save a `BrokerConfiguration`
   from the running app - this is exactly how you'll enter your real Solace
   Cloud / self-managed / ActiveMQ credentials to test against them.
2. **AMQP_Demo_Home**: a page with:
   - A data grid over `BrokerConfiguration` with a "New" button (opens the
     page above) and an "Open"/"Connect" button per row calling
     `ACT_ConnectAndListen` with that row's object.
   - A text box + "Send" button calling `ACT_SendTestMessage`.
   - A data grid over `MessageLog`, newest first, auto-refreshing (enable
     "Refresh on open" and consider a 2-3s client refresh timer for the demo),
     so incoming/outgoing messages appear live.
   - A "Disconnect" button calling `ACT_Disconnect`.
3. Add `AMQP_Demo_Home` to the app's navigation.

## 6. Run it

**Run locally** (F6 / the Run button). Open the demo page, create a
`BrokerConfiguration` pointed at your embedded/local ActiveMQ, Solace Cloud
service, or self-managed broker, click Connect, then Send a few messages -
they should appear in the log grid within a second or two. This is the same
scenario the standalone `demo` Maven module proves outside of Mendix; running
it inside Studio Pro proves the Java actions and microflow wiring on top of
that.

## Troubleshooting

- **"NoClassDefFoundError" for `org.apache.qpid...`**: the shaded jar in
  `userlib` wasn't picked up - re-check step 2, and confirm you built with
  `mvn package` (not `mvn compile`, which skips shading).
- **Connects but never receives anything on a topic**: JMS topic
  subscriptions in this connector are non-durable, so `StartListening` must
  run (and finish) before the message is sent - subscribe first, then send.
- **Solace self-managed: authentication failures only on that broker**:
  double-check `AppendVpnToUsername` / `MessageVpn` (see
  `broker-solace-selfmanaged.properties.example`) and confirm the AMQP
  service is enabled on that Message VPN in PubSub+ Manager.
- **TLS handshake errors against Solace Cloud**: confirm `UseTls` = true and
  `Port` = 5671 - Solace Cloud services normally only expose AMQP over TLS.
