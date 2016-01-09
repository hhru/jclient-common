package ru.hh.jclient.common.enforcer;

public class MethodDescriptor {

  public String name;
  public Integer line;
  public boolean violating = true;

  public MethodDescriptor(String name) {
    this.name = name;
  }

}
