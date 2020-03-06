package ru.hh.jclient.common.balancing;

import javax.annotation.Nonnull;
import java.util.Objects;

@FunctionalInterface
public interface UpstreamProfileSelector {

  UpstreamProfileSelector EMPTY = s -> null;
  static UpstreamProfileSelector forProfile(@Nonnull String profile) {
    return s -> Objects.requireNonNull(profile);
  }

  String getProfile(String serviceName);
}
