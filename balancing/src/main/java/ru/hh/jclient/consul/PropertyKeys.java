package ru.hh.jclient.consul;

public final class PropertyKeys {

    public static final String JCLIENT_PREFIX = "jclient";
    public static final String ALLOW_CROSS_DC_PATH = "allowCrossDCRequests";
    public static final String ALLOW_CROSS_DC_KEY = String.join(".", JCLIENT_PREFIX, ALLOW_CROSS_DC_PATH);
    public static final String SYNC_UPDATE_PATH = "failStartOnEmptyUpstream";
    public static final String SYNC_UPDATE_KEY = String.join(".", JCLIENT_PREFIX, SYNC_UPDATE_PATH);
    public static final String SELF_NODE_FILTERING_PATH = "selfNodeFiltering.enabled";
    public static final String SELF_NODE_FILTERING_KEY = String.join(".", JCLIENT_PREFIX, SELF_NODE_FILTERING_PATH);
    public static final String ALLOWED_DEGRADATION_PART_PATH = "allowedDegradationPart";
    public static final String ALLOWED_DEGRADATION_PART_KEY = String.join(".", JCLIENT_PREFIX, ALLOWED_DEGRADATION_PART_PATH);
    public static final String IGNORE_NO_SERVERS_IN_CURRENT_DC_PATH = "ignoreNoServersInCurrentDC";
    public static final String IGNORE_NO_SERVERS_IN_CURRENT_DC_KEY = String.join(".", JCLIENT_PREFIX, IGNORE_NO_SERVERS_IN_CURRENT_DC_PATH);

    public static final String UPSTREAM_PREFIX = "upstream";
    public static final String UPSTREAMS_PATH = "services";
    public static final String UPSTREAMS_KEY = String.join(".", JCLIENT_PREFIX, UPSTREAM_PREFIX, UPSTREAMS_PATH);
    public static final String DC_LIST_PATH = "DCList";
    public static final String DC_LIST_KEY = String.join(".", JCLIENT_PREFIX, UPSTREAM_PREFIX, DC_LIST_PATH);
    public static final String WATCH_SECONDS_PATH = "watchSeconds";
    public static final String WATCH_SECONDS_KEY = String.join(".", JCLIENT_PREFIX, UPSTREAM_PREFIX, WATCH_SECONDS_PATH);

    public static final String CONSUL_PREFIX = "consul";
    public static final String CONSISTENCY_PATH = "consistencyMode";
    public static final String CONSISTENCY_MODE_KEY = String.join(".", CONSUL_PREFIX, CONSISTENCY_PATH);
    public static final String HEALTHY_ONLY_PATH = "healthPassed";
    public static final String HEALTHY_ONLY_KEY = String.join(".", CONSUL_PREFIX, HEALTHY_ONLY_PATH);

    private PropertyKeys() {
    }
}
