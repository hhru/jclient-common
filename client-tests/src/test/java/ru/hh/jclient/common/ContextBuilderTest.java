package ru.hh.jclient.common;

import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;
import static ru.hh.jclient.common.HttpHeaderNames.X_REQUEST_ID;

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

  public void assertRequestIdInContext(String expectedRequestId) {
    assertEquals(expectedRequestId, MDC.get("rid"));
    assertNotNull(supplier.get());
    assertEquals(expectedRequestId, supplier.get().getHeaders().get(X_REQUEST_ID).get(0));
  }
}
