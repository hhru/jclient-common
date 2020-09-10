package ru.hh.jclient.consul;

import ru.hh.jclient.common.balancing.Server;

import java.util.List;
import java.util.function.Consumer;

public interface UpstreamService {

  void setupListener(Consumer<String> callback);

  List<Server> getServers(String serviceName);

}
