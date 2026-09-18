package com.solace.mendix.amqp;

/** A handle to an active subscription created by {@link BrokerConnector#subscribe}. Close it to stop listening. */
public interface Subscription extends AutoCloseable {

    Destination getDestination();

    boolean isActive();

    @Override
    void close();
}
