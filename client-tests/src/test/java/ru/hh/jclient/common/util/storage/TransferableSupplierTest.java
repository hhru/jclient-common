package ru.hh.jclient.common.util.storage;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import com.google.common.base.Throwables;
import ru.hh.jclient.common.util.storage.TransferUtils.PreparedTransfers;
import ru.hh.jclient.common.util.storage.TransferUtils.Transfers;
import ru.hh.jclient.common.util.storage.threadlocal.TransferableThreadLocalSupplier;

public class TransferableSupplierTest {

  @Test
  public void testThreadLocalTransfer() throws InterruptedException {
    String data = "testData";
    TransferableThreadLocalSupplier<String> supplier = new TransferableThreadLocalSupplier<>();
    Transfers transfers = TransferUtils.build(supplier);

    supplier.set(data);
    PreparedTransfers preparedTransfers = transfers.prepare();

    checkInThread(supplier, preparedTransfers, data, () -> {
      checkInThread(supplier, preparedTransfers, data, null);
      return null;
    });
  }

  private void checkInThread(TransferableSupplier<String> supplier, PreparedTransfers preparedTransfers, String data, Callable<Void> additionalStep)
      throws InterruptedException {
    AtomicBoolean emptyBefore = new AtomicBoolean();
    AtomicBoolean setCorrectly = new AtomicBoolean();
    AtomicBoolean emptyAfter = new AtomicBoolean();
    CountDownLatch latch = new CountDownLatch(1);

    Thread thread = new Thread(() -> {
      emptyBefore.set(supplier.get() == null);
      preparedTransfers.perform();
      setCorrectly.set(data.equals(supplier.get()));
      preparedTransfers.rollback();
      emptyAfter.set(supplier.get() == null);

      if (additionalStep != null) {
        try {
          additionalStep.call();
        }
        catch (Exception e) {
          throw Throwables.propagate(e);
        }
      }

      latch.countDown();
    });

    thread.start();

    assertTrue(latch.await(1000, MILLISECONDS));

    assertTrue(emptyBefore.get());
    assertTrue(setCorrectly.get());
    assertTrue(emptyAfter.get());
  }

  @Test
  public void testThreadLocalNotMixing() {
    TransferableThreadLocalSupplier<String> supplier1 = new TransferableThreadLocalSupplier<>();
    TransferableThreadLocalSupplier<String> supplier2 = new TransferableThreadLocalSupplier<>();

    supplier1.set("1");
    supplier2.set("2");

    assertEquals("1", supplier1.get());
    assertEquals("2", supplier2.get());

    supplier1.set("3"); // overwrite previous value
    supplier2.set("4"); // overwrite previous value

    assertEquals("3", supplier1.get());
    assertEquals("4", supplier2.get());

    supplier1.remove();
    supplier2.remove();

    assertNull(supplier1.get());
    assertNull(supplier2.get());
  }

}
