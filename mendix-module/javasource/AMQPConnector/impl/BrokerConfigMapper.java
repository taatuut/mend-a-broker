package amqpconnector.impl;

import com.solace.mendix.amqp.BrokerConfig;
import com.solace.mendix.amqp.BrokerType;

/**
 * Maps the generated {@code amqpconnector.proxies.BrokerConfiguration}
 * entity proxy (created automatically by Studio Pro from the domain model
 * described in README-DOMAIN-MODEL.md) onto the plain
 * {@link com.solace.mendix.amqp.BrokerConfig} the connector library uses.
 *
 * Kept as a separate class (instead of inlined in every action) so the one
 * place that knows about the generated proxy's getters is easy to find and
 * update if the domain model changes.
 */
public final class BrokerConfigMapper {

    private BrokerConfigMapper() { }

    public static BrokerConfig from(amqpconnector.proxies.BrokerConfiguration cfg) {
        BrokerType type = BrokerType.valueOf(cfg.getBrokerType().toString()); // ENUM_BrokerType -> BrokerType
        BrokerConfig.Builder builder = BrokerConfig.builder(type)
                .host(cfg.getHost())
                .username(cfg.getUsername())
                .password(cfg.getPassword())
                .useTls(cfg.getUseTls())
                .verifyHostname(cfg.getVerifyHostname())
                .appendVpnToUsername(cfg.getAppendVpnToUsername())
                .messageVpn(cfg.getMessageVpn())
                .clientId(cfg.getClientId());

        if (cfg.getPort() != null && cfg.getPort() > 0) {
            builder.port(cfg.getPort());
        }
        if (cfg.getConnectTimeoutMs() != null && cfg.getConnectTimeoutMs() > 0) {
            builder.connectTimeoutMs(cfg.getConnectTimeoutMs());
        }
        return builder.build();
    }
}
