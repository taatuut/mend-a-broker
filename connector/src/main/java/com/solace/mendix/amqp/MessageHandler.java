package com.solace.mendix.amqp;

/**
 * Callback invoked for every message received on a subscription. In the
 * Mendix module, the Java action implementation of {@code StartListening}
 * wraps a call back into a configured Mendix microflow inside this method
 * (see the module's README for the exact wiring via {@code Core.microflowCall}).
 */
@FunctionalInterface
public interface MessageHandler {
    void onMessage(InboundMessage message);
}
