package ru.hh.jclient.consul;

import com.google.common.io.BaseEncoding;
import com.orbitz.consul.Consul;
import com.orbitz.consul.model.kv.ImmutableValue;
import com.orbitz.consul.model.kv.Value;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class UpstreamConfigServiceImplTest {
  private static UpstreamConfigServiceImpl service;
  private static String SERVICE_NAME = "upstream1";
  static List<String> upstreamList = List.of(SERVICE_NAME);
  static Consul consulClient = mock(Consul.class);
  static int watchSeconds = 10;

  @BeforeClass
  public static void init() {
    service = new UpstreamConfigServiceImpl(upstreamList, consulClient, watchSeconds);
  }

  @Test
  public void testGetConfig() {
    Collection<Value> values = prepareValues();
    ValueNode valueNode = service.convertToTree(values);
    Map<String, ValueNode> map = valueNode.getMap();

    //two apps
    assertEquals(2, map.size());
    assertEquals("42", valueNode.getNode("app-name").getNode("some-profile").getValue("key1"));

    //same level
    assertEquals("56", valueNode.getNode("app-name").getNode("some-profile").getValue("key2"));

    //another length
    assertEquals("137", valueNode.getNode("second-app").getNode("some-profile")
            .getNode("additional-level").getValue("key1"));
  }

  @Test
  public void testNotify() {
    List<String> consumerMock = new ArrayList<>();

    try {
      service.setupListener(consumerMock::add);
    } catch (Exception ex) {
      //ignore
    }
    service.notifyListeners();

    assertEquals(1, consumerMock.size());

  }


  private Collection<Value> prepareValues() {
    Collection<Value> values = new ArrayList<>();
    ImmutableValue template = ImmutableValue.builder().key("template").value("template")
            .createIndex(System.currentTimeMillis()).modifyIndex(System.currentTimeMillis())
            .lockIndex(System.currentTimeMillis()).flags(System.currentTimeMillis())
            .build();

    values.add(ImmutableValue.copyOf(template).withKey("upstream/app-name/some-profile/key1")
            .withValue(BaseEncoding.base64().encode("42".getBytes())));
    values.add(ImmutableValue.copyOf(template).withKey("upstream/app-name/some-profile/key2")
            .withValue(BaseEncoding.base64().encode("56".getBytes())));
    values.add(ImmutableValue.copyOf(template).withKey("upstream/second-app/some-profile/additional-level/key1")
            .withValue(BaseEncoding.base64().encode("137".getBytes())));
    return values;
  }
}
