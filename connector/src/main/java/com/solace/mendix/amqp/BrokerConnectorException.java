package com.solace.mendix.amqp;

/** Wraps any connection, send, or subscribe failure so callers (and Mendix Java actions) deal with one checked exception type. */
public class BrokerConnectorException extends Exception {

    public BrokerConnectorException(String message) {
        super(message);
    }

    public BrokerConnectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
