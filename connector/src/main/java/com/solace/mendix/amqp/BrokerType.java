package com.solace.mendix.amqp;

/**
 * The broker platforms this connector supports. All three are reached
 * through the same wire protocol (AMQP 1.0) so a single JMS-over-AMQP
 * client (Apache Qpid JMS) is enough to talk to any of them &mdash; only
 * the connection parameters differ.
 */
public enum BrokerType {

    /**
     * Solace Cloud (a managed Event Broker Service). Each service exposes
     * its own AMQP endpoint that is already scoped to a single Message VPN,
     * so no VPN qualifier is normally required in the username.
     */
    SOLACE_CLOUD,

    /**
     * Solace PubSub+ software / appliance broker managed by the customer
     * (on-prem, VM, container, or self-hosted in any cloud). A single AMQP
     * service on such a broker can be shared by several Message VPNs, so
     * the Message VPN is (optionally) encoded into the SASL username as
     * {@code username@vpn-name}.
     */
    SOLACE_SELF_MANAGED,

    /**
     * Apache ActiveMQ (Artemis or "Classic"/5.x) with its AMQP acceptor/
     * transport connector enabled.
     */
    ACTIVEMQ
}
