package ru.hh.jclient.common.util.stats;

import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;


public class SlowRequestsLoggingHandler extends SimpleChannelHandler {

  private static final Logger logger = LoggerFactory.getLogger(SlowRequestsLoggingHandler.class);

  private final ConcurrentMap<Integer, List<EventWithTimestamp>> cache;
  private final long thresholdMs;

  public SlowRequestsLoggingHandler(int threshold, TimeUnit timeUnit) {
    cache = new ConcurrentHashMap<>();
    thresholdMs = timeUnit.toMillis(threshold);
  }

  @Override
  public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
    storeEvent(e.getChannel().getId(), e);
    super.handleUpstream(ctx, e);
  }

  @Override
  public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
    storeEvent(e.getChannel().getId(), e);
    super.handleDownstream(ctx, e);
  }


  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    logEventsIfSlow(e.getChannel().getId());
    super.messageReceived(ctx, e);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
    logEventsIfSlow(e.getChannel().getId());
    super.exceptionCaught(ctx, e);
  }

  @Override
  public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    logEventsIfSlow(e.getChannel().getId());
    super.channelClosed(ctx, e);
  }

  private void storeEvent(Integer key, ChannelEvent e) {
    List<EventWithTimestamp> events = cache.computeIfAbsent(key, ArrayList::new);
    events.add(new EventWithTimestamp(e, System.currentTimeMillis()));
  }

  private void logEventsIfSlow(Integer key) {
    List<EventWithTimestamp> events = cache.remove(key);
    if (events == null || events.size() < 2) {
      return;
    }

    long duration = events.get(events.size() - 1).timestamp - events.get(0).timestamp;
    if (duration >= thresholdMs) {
      logger.debug("Duration: {}ms", duration);
      events.forEach(SlowRequestsLoggingHandler::log);
    }
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
