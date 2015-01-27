package ru.hh.jclient.common.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "container")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlTest {

  @XmlAttribute
  public String name;

  public XmlTest(String name) {
    this.name = name;
  }

  public XmlTest() {
  }

}
