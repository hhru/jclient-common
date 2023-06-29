package ru.hh.jclient.common.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "error")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlError {

  @XmlElement
  public String message;

  public XmlError(String message) {
    this.message = message;
  }

  public XmlError() {
  }

}
