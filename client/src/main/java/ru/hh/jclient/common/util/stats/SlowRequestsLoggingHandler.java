package ru.hh.jclient.common.util.stats;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


public class SlowRequestsLoggingHandler implements ChannelUpstreamHandler, ChannelDownstreamHandler {

  private static final Logger logger = LoggerFactory.getLogger(SlowRequestsLoggingHandler.class);

  private final Cache<Integer, List<EventWithTimestamp>> cache;
  private final long thresholdMs;

  public SlowRequestsLoggingHandler(int threshold, int expireTimeout, TimeUnit timeUnit) {
    cache = CacheBuilder.newBuilder().expireAfterWrite(expireTimeout, TimeUnit.SECONDS).weakKeys().weakValues().build();
    thresholdMs = timeUnit.toMillis(threshold);
  }

  @Override
  public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
    handle(ctx, e);
    ctx.sendUpstream(e);
  }

  @Override
  public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
    handle(ctx, e);
    ctx.sendDownstream(e);
  }

  private void handle(ChannelHandlerContext ctx, ChannelEvent e) {
    Integer key = e.getChannel().getId();
    storeEvent(key, e);
    if (isTerminal(e)) {
      logEventsIfSlow(key);
    }
  }

  private static boolean isTerminal(ChannelEvent e) {
    //FUUUUUUUUUUUUUUUUUUUUUUCK
    return e instanceof ExceptionEvent || e instanceof MessageEvent && ((MessageEvent) e).getMessage().toString().contains("RECEIVED:");
  }

  private void storeEvent(Integer key, ChannelEvent e) {
    try {
      List<EventWithTimestamp> events = cache.get(key, ArrayList::new);
      events.add(new EventWithTimestamp(e, System.currentTimeMillis()));
    } catch (ExecutionException | ConcurrentModificationException ex) {
      logger.warn("Failed to store event for channel {}", e.getChannel(), ex);
    }
  }

  private void logEventsIfSlow(Integer key) {
    List<EventWithTimestamp> events = cache.getIfPresent(key);
    if (events == null || events.isEmpty()) {
      return;
    }
    if (events.get(events.size() - 1).timestamp - events.get(0).timestamp < thresholdMs) {
      return;
    }
    cache.invalidate(key);
    events.forEach(SlowRequestsLoggingHandler::log);
  }

  private static final class EventWithTimestamp {
    private final ChannelEvent event;
    private final long timestamp;

    private EventWithTimestamp(ChannelEvent event, long timestamp) {
      this.event = event;
      this.timestamp = timestamp;
    }

    @Override
    public String toString() {
      return "EventWithTimestamp{" +
        "event=" + event +
        ", timestamp=" + timestamp +
        " from epoch}";
    }
  }

  private static void log(EventWithTimestamp e) {
    if (e.event instanceof ExceptionEvent) {
      logger.info(e.toString(), ((ExceptionEvent) e.event).getCause());
    } else {
      logger.info(e.toString());
    }
  }
}
