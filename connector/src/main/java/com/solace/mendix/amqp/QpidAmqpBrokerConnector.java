package com.solace.mendix.amqp;

import org.apache.qpid.jms.JmsConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.jms.*;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link BrokerConnector} implementation built on Apache Qpid JMS, a plain
 * AMQP 1.0 client. Because AMQP 1.0 is a standard wire protocol implemented
 * by Solace PubSub+ (Cloud and self-managed) and by ActiveMQ, this single
 * class is all that is needed for every {@link BrokerType} &mdash; only the
 * {@link BrokerConfig} used to build the connection URI changes.
 */
public class QpidAmqpBrokerConnector implements BrokerConnector {

    private static final Logger LOG = LoggerFactory.getLogger(QpidAmqpBrokerConnector.class);

    private final BrokerConfig config;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicInteger subscriptionCounter = new AtomicInteger();
    private final Map<Integer, InternalSubscription> subscriptions = new ConcurrentHashMap<>();

    private Connection connection;
    private Session session;

    public QpidAmqpBrokerConnector(BrokerConfig config) {
        this.config = config;
    }

    @Override
    public synchronized void connect() throws BrokerConnectorException {
        if (connected.get()) {
            return;
        }
        try {
            String remoteUri = buildRemoteUri();
            LOG.info("Connecting to {} broker at {}:{} (tls={})",
                    config.getBrokerType(), config.getHost(), config.getPort(), config.isUseTls());

            JmsConnectionFactory factory = new JmsConnectionFactory(
                    config.getEffectiveUsername(), config.getPassword(), remoteUri);

            connection = factory.createConnection();
            if (config.getClientId() != null && !config.getClientId().isBlank()) {
                try {
                    connection.setClientID(config.getClientId());
                } catch (JMSException e) {
                    LOG.warn("Could not set clientID '{}': {}", config.getClientId(), e.getMessage());
                }
            }
            connection.setExceptionListener(e -> {
                LOG.error("AMQP connection-level exception on {} connection: {}",
                        config.getBrokerType(), e.getMessage(), e);
                connected.set(false);
            });
            connection.start();

            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            connected.set(true);
            LOG.info("Connected to {} broker.", config.getBrokerType());
        } catch (JMSException e) {
            connected.set(false);
            throw new BrokerConnectorException(
                    "Failed to connect to " + config.getBrokerType() + " broker at "
                            + config.getHost() + ":" + config.getPort() + " - " + e.getMessage(), e);
        }
    }

    private String buildRemoteUri() {
        String scheme = config.isUseTls() ? "amqps" : "amqp";
        StringBuilder uri = new StringBuilder(scheme).append("://")
                .append(config.getHost()).append(':').append(config.getPort());

        Map<String, String> params = new java.util.LinkedHashMap<>();
        params.put("jms.connectTimeout", String.valueOf(config.getConnectTimeoutMs()));
        if (config.isUseTls() && !config.isVerifyHostname()) {
            params.put("transport.verifyHost", "false");
            params.put("transport.trustAll", "true");
        }
        params.putAll(config.getExtraUriParams()); // caller-supplied params win on conflict

        if (!params.isEmpty()) {
            uri.append('?');
            boolean first = true;
            for (Map.Entry<String, String> e : params.entrySet()) {
                if (!first) uri.append('&');
                uri.append(e.getKey()).append('=').append(e.getValue());
                first = false;
            }
        }
        return uri.toString();
    }

    @Override
    public synchronized void disconnect() {
        subscriptions.values().forEach(InternalSubscription::closeQuietly);
        subscriptions.clear();
        try {
            if (session != null) session.close();
        } catch (JMSException e) {
            LOG.debug("Error closing session: {}", e.getMessage());
        }
        try {
            if (connection != null) connection.close();
        } catch (JMSException e) {
            LOG.debug("Error closing connection: {}", e.getMessage());
        }
        connected.set(false);
        session = null;
        connection = null;
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }

    @Override
    public BrokerConfig getConfig() {
        return config;
    }

    @Override
    public synchronized void send(Destination destination, String payload) throws BrokerConnectorException {
        requireConnected();
        try (MessageProducer producer = session.createProducer(toJmsDestination(destination))) {
            TextMessage message = session.createTextMessage(payload);
            producer.send(message);
            LOG.debug("Sent message to {}: {}", destination, payload);
        } catch (JMSException e) {
            throw new BrokerConnectorException("Failed to send message to " + destination + ": " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized Subscription subscribe(Destination destination, MessageHandler handler)
            throws BrokerConnectorException {
        requireConnected();
        try {
            MessageConsumer consumer = session.createConsumer(toJmsDestination(destination));
            int id = subscriptionCounter.incrementAndGet();
            InternalSubscription sub = new InternalSubscription(id, destination, consumer);
            consumer.setMessageListener(msg -> deliver(msg, destination, handler));
            subscriptions.put(id, sub);
            LOG.info("Subscribed to {}", destination);
            return sub;
        } catch (JMSException e) {
            throw new BrokerConnectorException("Failed to subscribe to " + destination + ": " + e.getMessage(), e);
        }
    }

    private void deliver(Message msg, Destination destination, MessageHandler handler) {
        try {
            String payload = extractPayload(msg);
            InboundMessage inbound = new InboundMessage(
                    payload, msg.getJMSMessageID(), destination.getName(), destination.getType(), Instant.now());
            handler.onMessage(inbound);
        } catch (Exception e) {
            LOG.error("Error handling inbound message on {}: {}", destination, e.getMessage(), e);
        }
    }

    private String extractPayload(Message msg) throws JMSException {
        if (msg instanceof TextMessage tm) {
            return tm.getText();
        }
        if (msg instanceof BytesMessage bm) {
            byte[] data = new byte[(int) bm.getBodyLength()];
            bm.readBytes(data);
            return new String(data, java.nio.charset.StandardCharsets.UTF_8);
        }
        return String.valueOf(msg);
    }

    private jakarta.jms.Destination toJmsDestination(Destination destination) throws JMSException {
        return destination.getType() == DestinationType.TOPIC
                ? session.createTopic(destination.getName())
                : session.createQueue(destination.getName());
    }

    private void requireConnected() throws BrokerConnectorException {
        if (!connected.get() || session == null) {
            throw new BrokerConnectorException("Not connected to " + config.getBrokerType() + " broker. Call connect() first.");
        }
    }

    private final class InternalSubscription implements Subscription {
        private final int id;
        private final Destination destination;
        private final MessageConsumer consumer;
        private volatile boolean active = true;

        InternalSubscription(int id, Destination destination, MessageConsumer consumer) {
            this.id = id;
            this.destination = destination;
            this.consumer = consumer;
        }

        @Override
        public Destination getDestination() {
            return destination;
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public void close() {
            closeQuietly();
            subscriptions.remove(id);
        }

        void closeQuietly() {
            active = false;
            try {
                consumer.close();
            } catch (JMSException e) {
                LOG.debug("Error closing consumer for {}: {}", destination, e.getMessage());
            }
        }
    }
}
