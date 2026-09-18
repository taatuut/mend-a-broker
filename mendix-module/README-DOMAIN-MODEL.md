# AMQPConnector module - domain model & enumerations to create in Studio Pro

Studio Pro projects (.mpr) are a proprietary binary/model format that can only
be authored correctly inside Studio Pro itself - there is no reliable way to
hand-write one outside the tool. This document is the exact spec to recreate
in Studio Pro 11.12 LTS; it takes about 10 minutes. Once these exist, Studio
Pro auto-generates the Java proxy classes (`amqpconnector.proxies.*`) that the
hand-written Java actions in `javasource/AMQPConnector/actions` compile
against - you do not write those proxy classes yourself.

## 1. Create the module

App menu -> **Add module** -> name it `AMQPConnector`.

## 2. Enumerations

### `ENUM_BrokerType`
| Name | Caption |
|---|---|
| SOLACE_CLOUD | Solace Cloud |
| SOLACE_SELF_MANAGED | Solace self-managed |
| ACTIVEMQ | ActiveMQ |

> The enumeration value **names** must match `com.solace.mendix.amqp.BrokerType`
> exactly (case-sensitive) - `BrokerConfigMapper.from(...)` maps them with
> `BrokerType.valueOf(cfg.getBrokerType().toString())`.

### `ENUM_DestinationType`
| Name | Caption |
|---|---|
| Queue | Queue |
| Topic | Topic |

## 3. Entities

### `BrokerConfiguration`
A reusable, storable connection profile - create one instance per broker
environment you want to connect to (e.g. "Solace Cloud - Dev", "ActiveMQ -
Local").

| Attribute | Type | Notes |
|---|---|---|
| Name | String (length 100) | Friendly label shown in the UI |
| BrokerType | Enumeration `ENUM_BrokerType` | |
| Host | String (length 255) | Hostname or IP, no scheme/port |
| Port | Integer | 0/empty = use the AMQP default (5672 plain, 5671 TLS) |
| UseTls | Boolean | Default `false` |
| VerifyHostname | Boolean | Default `true`; only disable for self-signed lab certs |
| Username | String (length 100) | |
| Password | String (length 255), attribute **marked confidential** | Studio Pro: Properties -> tick "Confidential" so it isn't logged/exported in plain text |
| MessageVpn | String (length 100) | Solace Message VPN name; leave blank for ActiveMQ |
| AppendVpnToUsername | Boolean | Default `false`; see the demo's `broker-solace-selfmanaged.properties.example` for when to turn this on |
| ClientId | String (length 100) | Optional JMS client id |
| ConnectTimeoutMs | Integer | 0/empty = 10000 |

### `MessageLog` (demo/audit trail - optional but included in the demo pages)

| Attribute | Type | Notes |
|---|---|---|
| Direction | Enumeration, values `Inbound` / `Outbound` | |
| ConnectionId | String (length 100) | The handle returned by `ConnectBroker` |
| DestinationName | String (length 255) | |
| DestinationType | Enumeration `ENUM_DestinationType` | |
| Payload | String (length 5000) | |
| MessageId | String (length 100) | Only set for inbound messages |
| Timestamp | Date and time | Default "current date and time" |

Add a 1-to-many association `BrokerConfiguration_MessageLog` (one
BrokerConfiguration has many MessageLog) if you want the demo page to filter
history per configured broker; the demo microflows below don't require it.

## 4. Java actions

Create each with **exactly** these names, parameter names/types and return
types (they must match the hand-written `.java` files under
`javasource/AMQPConnector/actions` word for word, since Studio Pro generates
the method signature from what you configure and then preserves only the
`BEGIN USER CODE` / `END USER CODE` block on subsequent edits):

| Java action | Parameters (name : type) | Return type |
|---|---|---|
| `ConnectBroker` | `BrokerConfiguration : BrokerConfiguration` (entity) | String |
| `DisconnectBroker` | `ConnectionId : String` | Boolean |
| `SendMessage` | `ConnectionId : String`, `DestinationName : String`, `DestinationType : ENUM_DestinationType`, `Payload : String` | Boolean |
| `StartListening` | `ConnectionId : String`, `DestinationName : String`, `DestinationType : ENUM_DestinationType`, `OnMessageMicroflow : String` | Boolean |
| `StopListening` | `ConnectionId : String`, `DestinationName : String` | Boolean |
| `IsConnected` | `ConnectionId : String` | Boolean |

After creating each action's shell in Studio Pro, open the generated `.java`
file (double-click the action, or find it under `javasource/AMQPConnector/actions`
in the App Explorer's "Show in App Explorer" toggle) and paste in the code
between `BEGIN USER CODE` / `END USER CODE` from the corresponding file in
this repo - the surrounding generated boilerplate should already match if the
signature above was entered exactly.

## 5. Microflows to demonstrate it (see README-STUDIO-PRO-SETUP.md for the full wiring)

- `ACT_ConnectAndSend` - builds a BrokerConfiguration (or retrieves a saved
  one), calls ConnectBroker, StartListening on a demo queue/topic, then
  SendMessage a test payload.
- `SUB_OnMessageReceived` - the callback microflow named in
  `OnMessageMicroflow`. Parameters: `ConnectionId : String`,
  `DestinationName : String`, `MessageId : String`, `Payload : String`.
  Creates and commits a `MessageLog` (Direction = Inbound) from them.
- `ACT_Disconnect` - calls StopListening then DisconnectBroker.
