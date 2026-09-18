# Architecture

## Why one connector works for three broker platforms

Solace Cloud, Solace PubSub+ self-managed, and Apache ActiveMQ are three
different products, but all three implement **AMQP 1.0** as a standard wire
protocol (an OASIS/ISO standard, not a Solace- or ActiveMQ-specific dialect).
That means a single generic AMQP 1.0 client library - here, [Apache Qpid
JMS](https://qpid.apache.org/components/jms/index.html), which speaks AMQP
1.0 underneath a familiar `javax.jms` API - can open a connection, send, and
receive against any of them. Only the *connection parameters* differ:

| | Solace Cloud | Solace self-managed | ActiveMQ |
|---|---|---|---|
| Default AMQP ports | 5671 (TLS only) | 5672 / 5671 | 5672 / 5671 |
| Multi-tenant VPN in username | No (service = 1 VPN) | Optional (`user@vpn`) | N/A |
| Enable step | On by default | Toggle AMQP service per VPN | Add an AMQP acceptor/connector |

This is why `BrokerConfig` + `BrokerType` capture exactly the fields that
differ, and `QpidAmqpBrokerConnector` (one class) is the only implementation
of `BrokerConnector` needed. Swapping brokers is a configuration change, not
a code change or redeploy.

## Layers

```
+-------------------------------------------------------------+
|  Mendix module (mendix-module/)                             |
|  - Domain model: BrokerConfiguration, MessageLog             |
|  - Java actions: ConnectBroker, SendMessage, StartListening, |
|    StopListening, DisconnectBroker, IsConnected              |
|  - Microflows / pages: demo UI, callback microflow           |
|  - impl/: maps Mendix proxies <-> plain connector types       |
+-------------------------------------------------------------+
                          | depends on (userlib jar)
+-------------------------------------------------------------+
|  connector/ - com.solace.mendix.amqp                         |
|  - BrokerType, BrokerConfig, Destination(Type)                |
|  - BrokerConnector (interface) / QpidAmqpBrokerConnector       |
|  - MessageHandler, InboundMessage, Subscription                |
|  - BrokerConnectorFactory                                     |
|  Pure Java, zero Mendix dependencies - independently reusable |
|  and independently testable.                                  |
+-------------------------------------------------------------+
                          | depends on
+-------------------------------------------------------------+
|  Apache Qpid JMS (AMQP 1.0 protocol implementation)            |
+-------------------------------------------------------------+
```

`demo/` depends on `connector/` the same way the Mendix module does, but runs
as a plain Java program (optionally against an embedded ActiveMQ broker it
starts itself) so the connector logic can be exercised and debugged with a
normal debugger/IDE, without needing Studio Pro at all.

## Connection lifecycle & the "handle" pattern

Mendix microflows can only carry primitives, entities, lists and enumerations
between activities - not a live Java object like an open JMS `Connection`.
`ConnectBroker` therefore opens the connection, stores the resulting
`BrokerConnector` in a static, thread-safe `ConnectionRegistry` inside the
module (`javasource/AMQPConnector/impl/ConnectionRegistry.java`), and hands
the microflow back an opaque `ConnectionId` string. Every subsequent action
(`SendMessage`, `StartListening`, ...) takes that id and looks the connection
back up. This is the same pattern most Mendix integration modules use for any
long-lived external resource (JDBC connections, FTP sessions, WebSockets).

## Inbound messages -> microflow callback

`StartListening` registers a JMS `MessageListener` under the hood
(`QpidAmqpBrokerConnector.subscribe`). Message delivery happens on a
broker-client thread with no Mendix request context, so the callback obtains
a fresh system context (`Core.createSystemContext()`) and invokes the
configured microflow by name (`Core.microflowCall(...).withParam(...).execute(...)`),
passing `ConnectionId`, `DestinationName`, `MessageId` and `Payload`. This
keeps all Mendix-specific logic (what to do with a received message) in a
microflow, where app builders can change it without touching Java.

## Security notes for a Marketplace submission

- The `Password` attribute on `BrokerConfiguration` must be marked
  **confidential** in Studio Pro (see README-DOMAIN-MODEL.md) so it is
  encrypted at rest and excluded from exports/logs.
- `VerifyHostname=false` / `transport.trustAll=true` exists only to support
  self-signed certificates in development labs - document this clearly and
  default it to `true` in any published configuration.
- Consider adding entity access rules restricting who can read/edit
  `BrokerConfiguration.Password` even within the app, since Studio Pro's
  "confidential" flag protects storage/export but module security still
  governs in-app visibility.
