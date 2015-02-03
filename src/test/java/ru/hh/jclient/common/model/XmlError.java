package ru.hh.jclient.common.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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
