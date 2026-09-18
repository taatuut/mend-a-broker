package com.solace.mendix.amqp;

/** Single entry point used by both the standalone demo and the Mendix Java actions. */
public final class BrokerConnectorFactory {

    private BrokerConnectorFactory() { }

    public static BrokerConnector create(BrokerConfig config) {
        // All three broker types speak AMQP 1.0, so today there is only one
        // implementation. This indirection exists so a broker-specific
        // implementation could be swapped in later without touching callers.
        return new QpidAmqpBrokerConnector(config);
    }
}
