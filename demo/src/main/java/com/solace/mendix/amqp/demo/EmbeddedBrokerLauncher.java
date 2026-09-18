package com.solace.mendix.amqp.demo;

/*
 * DISABLED FOR NOW - see README.md, "Why the embedded ActiveMQ demo mode is
 * disabled". As of the latest Artemis release (2.56.0), starting this
 * embedded broker on JDK 24+ crashes with
 * "UnsupportedOperationException: getSubject is not supported" while
 * Artemis builds its JMX management MBean, because the deprecated
 * java.security Subject/AccessControlContext APIs it still calls were
 * removed. Upstream tracking: https://issues.apache.org/jira/browse/ARTEMIS-5374
 * (closed as fixed, but https://issues.apache.org/jira/browse/ARTEMIS-5538,
 * a duplicate, still notes JDK 24 support as "a known issue").
 *
 * The demo now defaults to a local Solace PubSub+ software broker running
 * in Docker instead (see README.md, "Local broker for testing"), which
 * doesn't need this class at all. The corresponding Artemis dependencies
 * are commented out in demo/pom.xml. Once Artemis genuinely fixes JDK 24+
 * support, uncomment both this class and those dependencies to bring the
 * zero-external-infrastructure embedded mode back.

import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;

public final class EmbeddedBrokerLauncher {

    private final EmbeddedActiveMQ server = new EmbeddedActiveMQ();

    public void start(int amqpPort) throws Exception {
        Configuration config = new ConfigurationImpl();
        config.setSecurityEnabled(false); // demo only: accepts any username/password over AMQP SASL PLAIN
        config.setPersistenceEnabled(false);
        config.addAcceptorConfiguration("amqp", "tcp://0.0.0.0:" + amqpPort
                + "?protocols=AMQP;amqpMinLargeMessageSize=102400");
        config.setName("mendix-amqp-demo-embedded-broker");
        server.setConfiguration(config);
        server.start();
    }

    public void stop() throws Exception {
        server.stop();
    }
}
*/
