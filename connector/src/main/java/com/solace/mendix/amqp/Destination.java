package com.solace.mendix.amqp;

import java.util.Objects;

/** A named send/receive target: either a queue or a topic. */
public final class Destination {

    private final String name;
    private final DestinationType type;

    private Destination(String name, DestinationType type) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
    }

    public static Destination queue(String name) {
        return new Destination(name, DestinationType.QUEUE);
    }

    public static Destination topic(String name) {
        return new Destination(name, DestinationType.TOPIC);
    }

    public static Destination of(String name, DestinationType type) {
        return new Destination(name, type);
    }

    public String getName() {
        return name;
    }

    public DestinationType getType() {
        return type;
    }

    @Override
    public String toString() {
        return type + ":" + name;
    }
}
