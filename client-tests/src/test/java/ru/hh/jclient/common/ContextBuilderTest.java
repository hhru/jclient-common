package ru.hh.jclient.common;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;
import static ru.hh.jclient.common.HttpHeaderNames.HH_PROTO_SESSION;
import static ru.hh.jclient.common.HttpHeaderNames.X_REQUEST_ID;
import ru.hh.jclient.common.util.storage.MDCStorage;
import ru.hh.jclient.common.util.storage.StorageUtils;

public class ContextBuilderTest {

  private static final String OLD_REQUEST_ID = "oldRequestId";
  private static final String NEW_REQUEST_ID = "newRequestId";

  private HttpClientContextThreadLocalSupplier supplier;

  @Before
  public void before() {
    MDC.remove("rid");
    supplier = new HttpClientContextThreadLocalSupplier();
  }

  @Test
  public void contextSupplierTest() {
    supplier.forCurrentThread().execute(() -> assertNotNull(supplier.get()));
    assertNull(supplier.get());
  }

  @Test
  public void requestIdInContextTest() {
    supplier.forCurrentThread().withRequestId(NEW_REQUEST_ID).execute(() -> assertRequestIdInContext(NEW_REQUEST_ID));

    //Must clear requestId after execute if
    assertNull(MDC.get("rid"));

    MDC.put("rid", OLD_REQUEST_ID);

    //Must take requestId from MDC if present
    supplier.forCurrentThread().execute(() -> assertRequestIdInContext(OLD_REQUEST_ID));

    //Must not clear requestId after execute if was present
    assertEquals(OLD_REQUEST_ID, MDC.get("rid"));

    //withRequestId must override requestId from MDC
    supplier.forCurrentThread().withRequestId(NEW_REQUEST_ID).execute(() -> assertRequestIdInContext(NEW_REQUEST_ID));

    //Must restore old requestId after execute
    assertEquals(OLD_REQUEST_ID, MDC.get("rid"));

    MDC.remove("rid");
    supplier.forCurrentThread().withRequestId(OLD_REQUEST_ID).execute(() -> {
      supplier.forCurrentThread().withRequestId(NEW_REQUEST_ID).execute(() -> assertRequestIdInContext(NEW_REQUEST_ID));
      assertRequestIdInContext(OLD_REQUEST_ID);
    });
  }

  @Test
  public void appendHeadersInChainTest() {
    supplier.forCurrentThread()
        .withHeaders(Map.of("a", List.of("a")))
        .withHeaders(Map.of("b", List.of("b")))
        .withRequestId(NEW_REQUEST_ID)
        .execute(() -> {
          assertEquals(Map.of("a", List.of("a"), "b", List.of("b"), X_REQUEST_ID, List.of(NEW_REQUEST_ID)), supplier.get().getHeaders());
        });
  }

  @Test
  public void realSupplierInContextTest() throws InterruptedException {
    var someRequestId = "abcde";
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

    MDC.put("rid", someRequestId);
    MDC.put(HH_PROTO_SESSION, someSession);
    var transfers = context.getStorages().copy().add(realSupplier).prepare();
    var thread = new Thread(() -> {
      transfers.perform();

      var newContext = realSupplier.get();
      isSessionCaptured.set(someSession.equals(newContext.getHeaders().get(HH_PROTO_SESSION).get(0)));
      isSameMdc.set(someRequestId.equals(MDC.get("rid")));

      transfers.rollback();
    });
    thread.start();
    thread.join();

    assertTrue(isSessionCaptured.get());
    assertTrue(isSameMdc.get());
  }

  public void assertRequestIdInContext(String expectedRequestId) {
    assertEquals(expectedRequestId, MDC.get("rid"));
    assertNotNull(supplier.get());
    assertEquals(expectedRequestId, supplier.get().getHeaders().get(X_REQUEST_ID).get(0));
  }
}
