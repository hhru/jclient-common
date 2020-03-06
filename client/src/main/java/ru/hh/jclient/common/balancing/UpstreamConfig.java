package ru.hh.jclient.common.balancing;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.IntStream;

final class UpstreamConfig {
  static final int DEFAULT_MAX_TRIES = 2;
  static final int DEFAULT_MAX_FAILS = 1;
  static final int DEFAULT_MAX_TIMEOUT_TRIES = 1;

  static final int DEFAULT_FAIL_TIMEOUT_MS = 10;
  static final int DEFAULT_CONNECT_TIMEOUT_MS = 200;
  static final int DEFAULT_REQUEST_TIMEOUT_MS = 2_000;

  private int maxTries;
  private int maxFails;
  private int maxTimeoutTries;

  private int failTimeoutMs;
  private int connectTimeoutMs;
  private int requestTimeoutMs;

  private RetryPolicy retryPolicy = new RetryPolicy();
  private final List<Server> servers = new CopyOnWriteArrayList<>();

  static UpstreamConfig parse(String configString) {
    requireNonNull(configString, "config string should not be null");

    String[] configs = configString.split("\\|");

    try {
      Map<String, String> configMap = convertToMap(configs[0]);
      UpstreamConfig upstreamConfig = new UpstreamConfig();
      upstreamConfig.maxTries = parseIntOrFallback(configMap.get("max_tries"), DEFAULT_MAX_TRIES);
      upstreamConfig.maxFails = parseIntOrFallback(configMap.get("max_fails"), DEFAULT_MAX_FAILS);
      upstreamConfig.maxTimeoutTries = parseIntOrFallback(configMap.get("max_timeout_tries"), DEFAULT_MAX_TIMEOUT_TRIES);
      upstreamConfig.failTimeoutMs = parseAndConvertToMillisOrFallback(configMap.get("fail_timeout_sec"), DEFAULT_FAIL_TIMEOUT_MS);
      upstreamConfig.connectTimeoutMs = parseAndConvertToMillisOrFallback(configMap.get("connect_timeout_sec"), DEFAULT_CONNECT_TIMEOUT_MS);
      upstreamConfig.requestTimeoutMs = parseAndConvertToMillisOrFallback(configMap.get("request_timeout_sec"), DEFAULT_REQUEST_TIMEOUT_MS);

      if (configMap.containsKey("retry_policy")) {
        upstreamConfig.retryPolicy.update(configMap.get("retry_policy"));
      }

      for (int i = 1; i < configs.length; i++) {
        if (configs[i] != null && !configs[i].isBlank()) {
          upstreamConfig.addServer(parseServerConfig(configs[i]));
        }
      }

      return upstreamConfig;

    } catch (Exception e) {
      throw new UpstreamConfigFormatException("failed to parse upstream config: '" + configString + '\'', e);
    }
  }

  void update(UpstreamConfig newConfig) {
    requireNonNull(newConfig, "new config should not be empty");
    if (newConfig.servers.isEmpty()) {
      throw new IllegalArgumentException("new config should have servers");
    }

    maxTries = newConfig.maxTries;
    maxFails = newConfig.maxFails;
    maxTimeoutTries = newConfig.maxTimeoutTries;
    failTimeoutMs = newConfig.failTimeoutMs;
    connectTimeoutMs = newConfig.connectTimeoutMs;
    requestTimeoutMs = newConfig.requestTimeoutMs;
    retryPolicy = newConfig.retryPolicy;

    Map<String, Server> newAddressToServerMap = newConfig.servers
        .stream()
        .collect(toMap(Server::getAddress, Function.identity()));

    for (int i = 0; i < servers.size(); i++) {
      if (servers.get(i) != null) {
        String serverAddress = servers.get(i).getAddress();
        Server updatedServer = newAddressToServerMap.get(serverAddress);
        if (updatedServer == null) {
          servers.set(i, null);
        } else {
          servers.get(i).update(updatedServer);
          newAddressToServerMap.remove(serverAddress);
        }
      }
    }
    newAddressToServerMap.values().forEach(this::addServer);
  }

  private void addServer(Server server) {
    int index = getFirstFreeServerIndex();
    if (index > 0) {
      servers.set(index, server);
    } else {
      servers.add(server);
    }
  }

  int getMaxTries() {
    return maxTries;
  }

  int getMaxFails() {
    return maxFails;
  }

  int getMaxTimeoutTries() {
    return maxTimeoutTries;
  }

  int getFailTimeoutMs() {
    return failTimeoutMs;
  }

  int getConnectTimeoutMs() {
    return connectTimeoutMs;
  }

  int getRequestTimeoutMs() {
    return requestTimeoutMs;
  }

  public RetryPolicy getRetryPolicy() {
    return retryPolicy;
  }

  List<Server> getServers() {
    return Collections.unmodifiableList(servers);
  }

  @Override
  public String toString() {
    String serversStr = servers.stream().map(Object::toString).collect(joining(" | "));
    return "{max_tries=" + maxTries
        + ", max_timeout_tries=" + maxTimeoutTries
        + ", fail_timeout_ms=" + failTimeoutMs
        + ", max_fails=" + maxFails
        + ", connect_timeout_ms=" + connectTimeoutMs
        + ", request_timeout_ms=" + requestTimeoutMs
        + ", servers=" + servers.size()
        + "| " + serversStr
        + '}';
  }

  private int getFirstFreeServerIndex() {
    return IntStream.range(0, servers.size()).filter(i -> servers.get(i) == null).findFirst().orElse(-1);
  }

  private static Server parseServerConfig(String configStr) {
    Map<String, String> configMap = convertToMap(configStr);
    int weight = parseIntOrFallback(configMap.get("weight"), Server.DEFAULT_WEIGHT);
    String rack = configMap.get("rack");
    String datacenter = configMap.get("dc");
    return new Server(configMap.get("server"), weight, rack, datacenter);
  }

  private static Map<String, String> convertToMap(String configStr) {
    return Arrays.stream(configStr.split(" "))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(s -> s.split("="))
        .collect(toMap(p -> p[0], p -> p[1]));
  }

  private static int parseIntOrFallback(String value, int defaultValue) {
    return Optional.ofNullable(value).map(Integer::parseInt).orElse(defaultValue);
  }

  private static int parseAndConvertToMillisOrFallback(String value, int defaultValue) {
    return value != null ? Math.round(Float.parseFloat(value) * TimeUnit.SECONDS.toMillis(1)) : defaultValue;
  }

  private UpstreamConfig() {
  }

  public int getAllowedTimeoutMs() {
    return maxTimeoutTries * requestTimeoutMs;
  }
}
