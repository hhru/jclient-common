package ru.hh.jclient.common.util.storage;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Exchanger;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import ru.hh.jclient.common.util.storage.StorageUtils.Transfers;
import ru.hh.jclient.common.util.storage.StorageUtils.Storages;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ThreadLocalStorage.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class ThreadLocalStorageTest {

  private Logger logMock;

  @Before
  public void setUp() throws Exception {
    logMock = spy(Logger.class);
    Whitebox.setInternalState(ThreadLocalStorage.class, "LOG", logMock);
  }

  @Test
  public void testThreadLocalTransfer() throws InterruptedException {
    String data = "testData";
    ThreadLocalStorage<String> storage = new ThreadLocalStorage<>();
    Storages transfers = StorageUtils.build(storage);

    storage.set(data);

    checkInThread(storage, transfers, data, () -> {
      checkInThread(storage, transfers, data, null);
      return null;
    });
  }

  private static void checkInThread(Storage<String> storage, Storages storages, String data,
                                    Callable<Void> additionalStep) throws InterruptedException {
    AtomicBoolean emptyBefore = new AtomicBoolean();
    AtomicBoolean setCorrectly = new AtomicBoolean();
    AtomicBoolean emptyAfter = new AtomicBoolean();
    CountDownLatch latch = new CountDownLatch(1);

    Transfers transfers = storages.prepare();

    Thread thread = new Thread(() -> {
      emptyBefore.set(storage.get() == null);
      transfers.perform();
      setCorrectly.set(data.equals(storage.get()));

      if (additionalStep != null) {
        try {
          additionalStep.call();
        }
        catch (Exception e) {
          throw new RuntimeException(e);
        }
      }

      transfers.rollback();
      emptyAfter.set(storage.get() == null);

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
    ThreadLocalStorage<String> storage1 = new ThreadLocalStorage<>();
    ThreadLocalStorage<String> storage2 = new ThreadLocalStorage<>();

    storage1.set("1");
    storage2.set("2");

    assertEquals("1", storage1.get());
    assertEquals("2", storage2.get());

    storage1.set("3"); // overwrite previous value
    storage2.set("4"); // overwrite previous value

    assertEquals("3", storage1.get());
    assertEquals("4", storage2.get());

    storage1.clear();
    storage2.clear();

    assertNull(storage1.get());
    assertNull(storage2.get());
  }

  @Test
  public void testThreadLocalStorageNotLoggingIfInitValuePassed() throws InterruptedException {
    var storage = new ThreadLocalStorage<>(() -> "default");

    Transfer transfer = storage.prepareTransferToAnotherThread();
    var exchanger = new Exchanger<>();
    Thread thread = new Thread(() -> {
      transfer.perform();
      try {
        exchanger.exchange(null);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });
    thread.start();
    exchanger.exchange(null);
    verify(logMock, times(0)).warn(anyString(), anyString(), anyString(), nullable(String.class));
  }
}
