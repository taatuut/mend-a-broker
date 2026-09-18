package com.solace.mendix.amqp;

/**
 * A live, bidirectional connection to one broker. One instance = one
 * connection. Thread-safe: {@link #send} may be called concurrently with
 * an active {@link #subscribe} listener.
 */
public interface BrokerConnector extends AutoCloseable {

    /** Opens the AMQP connection and session. Idempotent if already connected. */
    void connect() throws BrokerConnectorException;

    /** Closes the connection and any open subscriptions. Safe to call multiple times. */
    void disconnect();

    boolean isConnected();

    BrokerConfig getConfig();

    /** Publishes a text message to a queue or topic. */
    void send(Destination destination, String payload) throws BrokerConnectorException;

    /**
     * Registers an asynchronous listener on a queue or topic. Returns a
     * {@link Subscription} handle; close it to unsubscribe.
     */
    Subscription subscribe(Destination destination, MessageHandler handler) throws BrokerConnectorException;

    @Override
    default void close() {
        disconnect();
    }
}
