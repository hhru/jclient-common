package ru.hh.jclient.consul;

public final class PropertyKeys {
    public static final String UPSTREAMS_KEY = "jclient.upstream.services";
    public static final String CONSISTENCY_MODE_KEY = "consul.consistencyMode";
    public static final String SYNC_UPDATE_KEY = "jclient.failStartOnEmptyUpstream";
    public static final String WATCH_SECONDS_KEY = "jclient.upstream.watchSeconds";
    public static final String ALLOW_CROSS_DC_KEY = "allowCrossDCRequests";
    public static final String HEALTHY_ONLY_KEY = "consul.healthPassed";
    public static final String SELF_NODE_FILTERING_KEY = "jclient.selfNodeFiltering.enabled";
    public static final String DC_LIST_KEY = "jclient.upstream.DCList";
    public static final String DATACENTER_KEY = "datacenter";
    public static final String NODE_NAME_KEY = "nodeName";
}
