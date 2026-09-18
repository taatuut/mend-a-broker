package com.solace.mendix.amqp;

import java.time.Instant;

/** A received message, handed to a {@link MessageHandler}. */
public final class InboundMessage {

    private final String payload;
    private final String messageId;
    private final String destinationName;
    private final DestinationType destinationType;
    private final Instant receivedAt;

    public InboundMessage(String payload, String messageId, String destinationName,
                           DestinationType destinationType, Instant receivedAt) {
        this.payload = payload;
        this.messageId = messageId;
        this.destinationName = destinationName;
        this.destinationType = destinationType;
        this.receivedAt = receivedAt;
    }

    public String getPayload() { return payload; }
    public String getMessageId() { return messageId; }
    public String getDestinationName() { return destinationName; }
    public DestinationType getDestinationType() { return destinationType; }
    public Instant getReceivedAt() { return receivedAt; }

    @Override
    public String toString() {
        return "InboundMessage{" +
                "destination=" + destinationType + ":" + destinationName +
                ", messageId='" + messageId + '\'' +
                ", receivedAt=" + receivedAt +
                ", payload='" + payload + '\'' +
                '}';
    }
}
