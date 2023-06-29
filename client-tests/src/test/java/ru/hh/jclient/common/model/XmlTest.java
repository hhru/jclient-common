package ru.hh.jclient.common.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    XmlTest xmlTest = (XmlTest) o;
    return Objects.equals(name, xmlTest.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }
}
