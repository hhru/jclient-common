package ru.hh.jclient.consul;

import ru.hh.jclient.common.balancing.Server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class TestUpstreamService implements UpstreamService {

    private final Map<String, List<Server>> servers;

    public TestUpstreamService() {
        this.servers = new HashMap<>();
    }

    public void setServers(String serviceName, List<Server> servers) {
        this.servers.put(serviceName, servers);
    }

    public void clearServers() {
        this.servers.clear();
    }

    @Override
    public void setupListener(Consumer<String> callback) {

    }

    @Override
    public List<Server> getServers(String serviceName) {
        return servers.computeIfAbsent(serviceName, ignored -> new ArrayList<>());
    }
}
