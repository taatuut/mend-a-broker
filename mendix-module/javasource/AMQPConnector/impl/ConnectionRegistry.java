package amqpconnector.impl;

import com.solace.mendix.amqp.BrokerConnector;
import com.solace.mendix.amqp.Subscription;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the live (non-serializable) {@link BrokerConnector} and
 * {@link Subscription} instances that a Mendix microflow cannot itself
 * carry between steps. Every AMQPConnector Java action looks connections
 * and subscriptions up here by the opaque String id that ConnectBroker /
 * StartListening hand back to the microflow.
 *
 * This is the same "resource handle" pattern used by most Mendix
 * integration modules that wrap a long-lived external connection (JDBC,
 * FTP, WebSockets, ...): the Java object itself is never exposed to the
 * domain model, only its id.
 */
public final class ConnectionRegistry {

    private static final Map<String, BrokerConnector> CONNECTIONS = new ConcurrentHashMap<>();
    private static final Map<String, Subscription> SUBSCRIPTIONS = new ConcurrentHashMap<>();

    private ConnectionRegistry() { }

    public static String register(BrokerConnector connector) {
        String id = UUID.randomUUID().toString();
        CONNECTIONS.put(id, connector);
        return id;
    }

    public static BrokerConnector get(String connectionId) {
        return CONNECTIONS.get(connectionId);
    }

    public static BrokerConnector remove(String connectionId) {
        return CONNECTIONS.remove(connectionId);
    }

    public static String registerSubscription(String connectionId, String destinationName, Subscription subscription) {
        String key = subscriptionKey(connectionId, destinationName);
        SUBSCRIPTIONS.put(key, subscription);
        return key;
    }

    public static Subscription removeSubscription(String connectionId, String destinationName) {
        return SUBSCRIPTIONS.remove(subscriptionKey(connectionId, destinationName));
    }

    private static String subscriptionKey(String connectionId, String destinationName) {
        return connectionId + "::" + destinationName;
    }
}
