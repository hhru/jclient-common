package ru.hh.jclient.common;

public abstract class AbstractClient {

  protected String host;
  protected HttpClientBuilder http;

  protected AbstractClient(String host, HttpClientBuilder http) {
    this.host = host;
    this.http = http;
  }

}