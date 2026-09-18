package com.solace.mendix.amqp.demo;

import com.solace.mendix.amqp.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Standalone, runnable proof that {@link BrokerConnectorFactory} produces a
 * working bidirectional AMQP connection against any of the three supported
 * broker types &mdash; entirely outside of Mendix.
 *
 * Usage:
 *   java -jar amqp-broker-demo.jar                        -> demos against a local Solace PubSub+
 *                                                             broker running in Docker (see README.md,
 *                                                             "Local broker for testing" - start the
 *                                                             container first)
 *   java -jar amqp-broker-demo.jar path/to/my.properties  -> demos against the broker described in that file
 *
 * See the *.properties(.example) files next to this jar for the connection
 * settings each broker type needs.
 *
 * Note: this used to default to a zero-setup embedded ActiveMQ Artemis
 * broker instead. That mode is temporarily disabled - see README.md, "Why
 * the embedded ActiveMQ demo mode is disabled" - and
 * EmbeddedBrokerLauncher.java is commented out rather than removed, so it's
 * easy to bring back once upstream Artemis fixes JDK 24+ support.
 */
public final class DemoApp {

    private static final Destination QUEUE = Destination.queue("mendix/amqp/demo/queue");
    private static final Destination TOPIC = Destination.topic("mendix/amqp/demo/topic");

    public static void main(String[] args) throws Exception {
        Properties props;
        if (args.length == 0) {
            // Defaults to a local Solace PubSub+ software broker running in
            // Docker - see README.md, "Local broker for testing", for the
            // one docker run command that starts it.
            props = loadClasspathProperties("/broker-solace-docker-local.properties");
        } else {
            props = loadFileProperties(args[0]);
        }

        BrokerConfig config = BrokerConfig.fromProperties(props);
        System.out.println("=== Config: " + config + " ===");
        runDemo(config);
    }

    private static void runDemo(BrokerConfig config) throws Exception {
        CountDownLatch queueLatch = new CountDownLatch(3);
        CountDownLatch topicLatch = new CountDownLatch(1);

        try (BrokerConnector connector = BrokerConnectorFactory.create(config)) {
            System.out.println("=== Connecting to " + config.getBrokerType() + " (" + config.getHost() + ":" + config.getPort() + ") ===");
            connector.connect();
            System.out.println("=== Connected: " + connector.isConnected() + " ===");

            MessageHandler queueHandler = msg -> {
                System.out.println("[RECEIVED FROM QUEUE] " + msg.getPayload());
                queueLatch.countDown();
            };
            MessageHandler topicHandler = msg -> {
                System.out.println("[RECEIVED FROM TOPIC] " + msg.getPayload());
                topicLatch.countDown();
            };

            try (Subscription queueSub = connector.subscribe(QUEUE, queueHandler);
                 Subscription topicSub = connector.subscribe(TOPIC, topicHandler)) {

                System.out.println("=== Subscribed to queue '" + QUEUE.getName() + "' and topic '" + TOPIC.getName() + "' ===");

                System.out.println("=== Sending 3 messages to the queue (point-to-point) ===");
                for (int i = 1; i <= 3; i++) {
                    connector.send(QUEUE, "Hello from Mendix AMQP demo - queue message #" + i);
                }

                System.out.println("=== Publishing 1 message to the topic (publish/subscribe) ===");
                connector.send(TOPIC, "Hello from Mendix AMQP demo - topic broadcast");

                boolean queueOk = queueLatch.await(15, TimeUnit.SECONDS);
                boolean topicOk = topicLatch.await(15, TimeUnit.SECONDS);

                System.out.println("=== RESULT: queue round-trip " + (queueOk ? "SUCCEEDED" : "TIMED OUT")
                        + ", topic round-trip " + (topicOk ? "SUCCEEDED" : "TIMED OUT") + " ===");

                if (!queueOk || !topicOk) {
                    System.exit(1);
                }
            }

            connector.disconnect();
            System.out.println("=== Disconnected cleanly. Demo complete. ===");
        }
    }

    private static Properties loadClasspathProperties(String resource) throws IOException {
        Properties p = new Properties();
        try (InputStream in = DemoApp.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("Classpath resource not found: " + resource);
            }
            p.load(in);
        }
        return p;
    }

    private static Properties loadFileProperties(String path) throws IOException {
        Properties p = new Properties();
        try (InputStream in = new FileInputStream(path)) {
            p.load(in);
        }
        return p;
    }
}
