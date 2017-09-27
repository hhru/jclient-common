package ru.hh.jclient.common.balancing;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.List;

public class UpstreamConfigParserTest {

  @Test
  public void parseOneServer() throws Exception {
    String configStr = "max_tries=3 max_timeout_tries=2 max_fails=30  connect_timeout_sec=0.2 request_timeout_sec=2   " +
        "fail_timeout_sec=1.5 |server=localhost:9090   weight=42 ";
    UpstreamConfig config = UpstreamConfig.parse(configStr);

    assertEquals(3, config.getMaxTries());
    assertEquals(30, config.getMaxFails());
    assertEquals(2, config.getMaxTimeoutTries());
    assertEquals(1500, config.getFailTimeoutMs());
    assertEquals(200, config.getConnectTimeoutMs());
    assertEquals(2000, config.getRequestTimeoutMs());

    List<Server> servers = config.getServers();
    assertEquals(1, servers.size());
    assertEquals("localhost:9090", servers.get(0).getAddress());
    assertEquals(42, servers.get(0).getWeight());
  }

  @Test
  public void parseMultipleServers() throws Exception {
    String configStr = "max_tries=1 max_fails=1 fail_timeout_sec=1 | server=local-host:9090 weight=5   |server=host:9091 weight=2 | ";
    UpstreamConfig config = UpstreamConfig.parse(configStr);

    assertEquals(1, config.getMaxTries());
    assertEquals(1, config.getMaxFails());
    assertEquals(1_000, config.getFailTimeoutMs());

    List<Server> servers = config.getServers();
    assertEquals(2, servers.size());
    assertEquals("local-host:9090", servers.get(0).getAddress());
    assertEquals(5, servers.get(0).getWeight());

    assertEquals("host:9091", servers.get(1).getAddress());
    assertEquals(2, servers.get(1).getWeight());
  }

  @Test
  public void parseEmptyConfig() throws Exception {
    UpstreamConfig config = UpstreamConfig.parse("");

    assertEquals(UpstreamConfig.DEFAULT_MAX_TRIES, config.getMaxTries());
    assertEquals(UpstreamConfig.DEFAULT_MAX_FAILS, config.getMaxFails());
    assertEquals(UpstreamConfig.DEFAULT_MAX_TIMEOUT_TRIES, config.getMaxTimeoutTries());
    assertEquals(UpstreamConfig.DEFAULT_FAIL_TIMEOUT_MS, config.getFailTimeoutMs());
    assertEquals(UpstreamConfig.DEFAULT_CONNECT_TIMEOUT_MS, config.getConnectTimeoutMs());
    assertEquals(UpstreamConfig.DEFAULT_REQUEST_TIMEOUT_MS, config.getRequestTimeoutMs());
    assertEquals(0, config.getServers().size());
  }

  @Test
  public void parseConfigWithOneServer() throws Exception {
    UpstreamConfig config = UpstreamConfig.parse("|server=test ");

    List<Server> servers = config.getServers();
    assertEquals("test", servers.get(0).getAddress());
    assertEquals(Server.DEFAULT_WEIGHT, servers.get(0).getWeight());
  }

  @Test
  public void parseConfigWithNoServers() throws Exception {
    UpstreamConfig config = UpstreamConfig.parse(" | ");

    List<Server> servers = config.getServers();
    assertEquals(0, servers.size());
  }

  @Test(expected = UpstreamConfigFormatException.class)
  public void parseShouldFailIfServerPrefixIsMissed() throws Exception {

    UpstreamConfig.parse("| http://test");
  }
}
