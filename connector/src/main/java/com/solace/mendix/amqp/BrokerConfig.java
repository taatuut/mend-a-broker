package com.solace.mendix.amqp;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Everything needed to open one AMQP 1.0 connection to a broker, regardless
 * of which of the three supported platforms it is. Build one with
 * {@link #builder(BrokerType)}, or load one from a {@link Properties} object
 * with {@link #fromProperties(Properties)} (used by the demo app and, in the
 * Mendix module, populated from the BrokerConfiguration domain-model entity).
 */
public final class BrokerConfig {

    private final BrokerType brokerType;
    private final String host;
    private final int port;
    private final boolean useTls;
    private final boolean verifyHostname;
    private final String username;
    private final String password;
    private final String messageVpn;
    private final boolean appendVpnToUsername;
    private final String clientId;
    private final int connectTimeoutMs;
    private final Map<String, String> extraUriParams;

    private BrokerConfig(Builder b) {
        this.brokerType = Objects.requireNonNull(b.brokerType, "brokerType");
        this.host = Objects.requireNonNull(b.host, "host");
        this.port = b.port;
        this.useTls = b.useTls;
        this.verifyHostname = b.verifyHostname;
        this.username = b.username;
        this.password = b.password;
        this.messageVpn = b.messageVpn;
        this.appendVpnToUsername = b.appendVpnToUsername;
        this.clientId = b.clientId;
        this.connectTimeoutMs = b.connectTimeoutMs;
        this.extraUriParams = new LinkedHashMap<>(b.extraUriParams);
    }

    public static Builder builder(BrokerType brokerType) {
        return new Builder(brokerType);
    }

    /**
     * Loads a config from simple key/value properties. Recognised keys:
     * broker.type (SOLACE_CLOUD|SOLACE_SELF_MANAGED|ACTIVEMQ), broker.host,
     * broker.port, broker.tls (true/false), broker.verifyHostname
     * (true/false), broker.username, broker.password, broker.messageVpn,
     * broker.appendVpnToUsername (true/false), broker.clientId,
     * broker.connectTimeoutMs, and any broker.uriParam.&lt;name&gt;=value
     * pairs, which are passed straight through as AMQP/transport URI
     * options (e.g. broker.uriParam.transport.verifyHost=false).
     */
    public static BrokerConfig fromProperties(Properties p) {
        BrokerType type = BrokerType.valueOf(require(p, "broker.type").trim().toUpperCase());
        Builder b = builder(type)
                .host(require(p, "broker.host"))
                .port(Integer.parseInt(require(p, "broker.port")))
                .username(p.getProperty("broker.username", ""))
                .password(p.getProperty("broker.password", ""))
                .useTls(Boolean.parseBoolean(p.getProperty("broker.tls", "false")))
                .verifyHostname(Boolean.parseBoolean(p.getProperty("broker.verifyHostname", "true")))
                .messageVpn(p.getProperty("broker.messageVpn"))
                .appendVpnToUsername(Boolean.parseBoolean(p.getProperty("broker.appendVpnToUsername", "false")))
                .clientId(p.getProperty("broker.clientId"))
                .connectTimeoutMs(Integer.parseInt(p.getProperty("broker.connectTimeoutMs", "10000")));
        for (String key : p.stringPropertyNames()) {
            if (key.startsWith("broker.uriParam.")) {
                b.uriParam(key.substring("broker.uriParam.".length()), p.getProperty(key));
            }
        }
        return b.build();
    }

    private static String require(Properties p, String key) {
        String v = p.getProperty(key);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("Missing required property: " + key);
        }
        return v;
    }

    public BrokerType getBrokerType() { return brokerType; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public boolean isUseTls() { return useTls; }
    public boolean isVerifyHostname() { return verifyHostname; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getMessageVpn() { return messageVpn; }
    public boolean isAppendVpnToUsername() { return appendVpnToUsername; }
    public String getClientId() { return clientId; }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public Map<String, String> getExtraUriParams() { return extraUriParams; }

    /** The effective SASL username sent to the broker, with the Message VPN folded in if configured to do so. */
    public String getEffectiveUsername() {
        if (appendVpnToUsername && messageVpn != null && !messageVpn.isBlank()) {
            return username + "@" + messageVpn;
        }
        return username;
    }

    @Override
    public String toString() {
        // Deliberately omits password.
        return "BrokerConfig{" +
                "brokerType=" + brokerType +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", useTls=" + useTls +
                ", verifyHostname=" + verifyHostname +
                ", username='" + username + '\'' +
                ", messageVpn='" + messageVpn + '\'' +
                ", appendVpnToUsername=" + appendVpnToUsername +
                ", clientId='" + clientId + '\'' +
                ", connectTimeoutMs=" + connectTimeoutMs +
                ", extraUriParams=" + extraUriParams +
                '}';
    }

    public static final class Builder {
        private final BrokerType brokerType;
        private String host;
        private int port = -1;
        private boolean useTls = false;
        private boolean verifyHostname = true;
        private String username = "";
        private String password = "";
        private String messageVpn;
        private boolean appendVpnToUsername = false;
        private String clientId;
        private int connectTimeoutMs = 10_000;
        private final Map<String, String> extraUriParams = new LinkedHashMap<>();

        private Builder(BrokerType brokerType) {
            this.brokerType = brokerType;
        }

        public Builder host(String host) { this.host = host; return this; }
        public Builder port(int port) { this.port = port; return this; }
        public Builder useTls(boolean useTls) { this.useTls = useTls; return this; }
        public Builder verifyHostname(boolean verifyHostname) { this.verifyHostname = verifyHostname; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder password(String password) { this.password = password; return this; }
        public Builder messageVpn(String messageVpn) { this.messageVpn = messageVpn; return this; }
        public Builder appendVpnToUsername(boolean append) { this.appendVpnToUsername = append; return this; }
        public Builder clientId(String clientId) { this.clientId = clientId; return this; }
        public Builder connectTimeoutMs(int ms) { this.connectTimeoutMs = ms; return this; }
        public Builder uriParam(String key, String value) { this.extraUriParams.put(key, value); return this; }

        public BrokerConfig build() {
            if (port < 0) {
                port = useTls ? 5671 : 5672; // AMQP 1.0 IANA-registered defaults, used by all three brokers.
            }
            return new BrokerConfig(this);
        }
    }
}
