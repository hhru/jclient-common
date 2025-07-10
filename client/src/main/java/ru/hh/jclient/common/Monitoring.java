package ru.hh.jclient.common;

public interface Monitoring {

  void countRequest(
      String upstreamName,
      String serverDatacenter,
      String serverAddress,
      HttpHeaders requestHeaders,
      int statusCode,
      long requestTimeMillis,
      boolean isRequestFinal,
      String balancingStrategyType
  );

  void countRequestTime(String upstreamName, String serverDatacenter, HttpHeaders requestHeaders, long requestTimeMillis);

  void countRetry(
      String upstreamName,
      String serverDatacenter,
      String serverAddress,
      HttpHeaders requestHeaders,
      int statusCode,
      int firstStatusCode,
      int triesUsed
  );

  void countUpdateIgnore(String upstreamName, String serverDatacenter);
}
