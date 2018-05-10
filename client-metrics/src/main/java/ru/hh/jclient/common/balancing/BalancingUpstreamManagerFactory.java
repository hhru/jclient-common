package ru.hh.jclient.common.balancing;

import com.datastax.driver.core.Session;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.common.UpstreamManager;
import ru.hh.jclient.common.metrics.UpstreamMonitoring;
import ru.hh.metrics.StatsDSender;
import ru.hh.settings.SettingsClient;
import ru.hh.settings.SystemSettingsClient;

import java.util.concurrent.ScheduledExecutorService;

public class BalancingUpstreamManagerFactory {

  public static UpstreamManager create(String serviceName, Session cassandraSession, StatsDSender statsDSender) {
    return create(serviceName, cassandraSession, statsDSender, createScheduledExecutor());
  }

  public static UpstreamManager create(String serviceName,
                                       Session cassandraSession,
                                       StatsDSender statsDSender,
                                       ScheduledExecutorService scheduledExecutorService) {

    Monitoring monitoring = new UpstreamMonitoring(statsDSender, serviceName);
    UpstreamManager upstreamManager = new BalancingUpstreamManager(scheduledExecutorService, monitoring);
    createSystemSettingsListener(cassandraSession, upstreamManager);
    return upstreamManager;
  }

  private static void createSystemSettingsListener(Session cassandraSession, UpstreamManager upstreamManager) {
    requireNonNull(cassandraSession, "cassandraSession must not be null");
    SettingsClient settingsClient = new SystemSettingsClient(cassandraSession);
    settingsClient.addListener("*", upstreamManager::updateUpstream, true);
  }

  private static ScheduledExecutorService createScheduledExecutor() {
    return newSingleThreadScheduledExecutor((Runnable r) -> {
      Thread thread = new Thread(r, "balancing upstream manager scheduled executor");
      thread.setDaemon(true);
      return thread;
    });
  }
}
