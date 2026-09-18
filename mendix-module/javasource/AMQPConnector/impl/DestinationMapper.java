package amqpconnector.impl;

import com.solace.mendix.amqp.Destination;

/** Maps the generated {@code amqpconnector.proxies.ENUM_DestinationType} enumeration onto {@link Destination}. */
public final class DestinationMapper {

    private DestinationMapper() { }

    public static Destination toDestination(String destinationName, amqpconnector.proxies.ENUM_DestinationType type) {
        return switch (type) {
            case Queue -> Destination.queue(destinationName);
            case Topic -> Destination.topic(destinationName);
        };
    }
}
