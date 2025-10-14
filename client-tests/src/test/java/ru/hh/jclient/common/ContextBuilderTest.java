package ru.hh.jclient.common;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import static ru.hh.jclient.common.HttpHeaderNames.HH_PROTO_SESSION;
import ru.hh.jclient.common.util.storage.MDCStorage;
import ru.hh.jclient.common.util.storage.StorageUtils;

public class ContextBuilderTest {

  private HttpClientContextThreadLocalSupplier supplier;

  @BeforeEach
  public void before() {
    supplier = new HttpClientContextThreadLocalSupplier();
  }

  @Test
  public void contextSupplierTest() {
    supplier.forCurrentThread().execute(() -> assertNotNull(supplier.get()));
    assertNull(supplier.get());
  }

  @Test
  public void appendHeadersInChainTest() {
    supplier
        .forCurrentThread()
        .withHeaders(Map.of("a", List.of("a")))
        .withHeaders(Map.of("b", List.of("b")))
        .execute(() -> assertEquals(Map.of("a", List.of("a"), "b", List.of("b")), supplier.get().getHeaders()));
  }

  @Test
  public void realSupplierInContextTest() throws InterruptedException {
    var someTestId = "abcde";
    var someSession = "somesession";

    var realSupplier = new HttpClientContextThreadLocalSupplier(() -> {
      Map<String, List<String>> headers = new HashMap<>();
      var protoSessionValue = MDC.get(HH_PROTO_SESSION);
      if (protoSessionValue != null) {
        headers.put(HH_PROTO_SESSION, singletonList(protoSessionValue));
      }
      return new HttpClientContext(headers, emptyMap(), emptyList(), StorageUtils.build(new MDCStorage()));
    }, false);

    var isSessionCaptured = new AtomicBoolean();
    var isSameMdc = new AtomicBoolean();

    var context = realSupplier.get();
    assertNull(context.getHeaders().get(HH_PROTO_SESSION));

    MDC.put("someid", someTestId);
    MDC.put(HH_PROTO_SESSION, someSession);
    var transfers = context.getStorages().copy().add(realSupplier).prepare();
    var thread = new Thread(() -> {
      transfers.perform();

      var newContext = realSupplier.get();
      isSessionCaptured.set(someSession.equals(newContext.getHeaders().get(HH_PROTO_SESSION).get(0)));
      isSameMdc.set(someTestId.equals(MDC.get("someid")));

      transfers.rollback();
    });
    thread.start();
    thread.join();

    assertTrue(isSessionCaptured.get());
    assertTrue(isSameMdc.get());
  }
}
