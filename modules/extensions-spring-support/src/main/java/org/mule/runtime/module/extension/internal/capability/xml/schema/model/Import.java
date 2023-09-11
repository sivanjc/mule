/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.2-hudson-jaxb-ri-2.2-63- 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.06.14 at 03:58:12 PM GMT-03:00 
//


package org.mule.runtime.module.extension.internal.capability.xml.schema.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>
 * Java class for anonymous complex type.
 * <p/>
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.w3.org/2001/XMLSchema}annotated">
 *       &lt;attribute name="namespace" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;attribute name="schemaLocation" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "import")
public class Import extends Annotated {

  @XmlAttribute(name = "namespace")
  @XmlSchemaType(name = "anyURI")
  protected String namespace;
  @XmlAttribute(name = "schemaLocation")
  @XmlSchemaType(name = "anyURI")
  protected String schemaLocation;

  /**
   * Gets the value of the namespace property.
   *
   * @return possible object is {@link String }
   */
  public String getNamespace() {
    return namespace;
  }

  /**
   * Sets the value of the namespace property.
   *
   * @param value allowed object is {@link String }
   */
  public void setNamespace(String value) {
    this.namespace = value;
  }

  /**
   * Gets the value of the schemaLocation property.
   *
   * @return possible object is {@link String }
   */
  public String getSchemaLocation() {
    return schemaLocation;
  }

  /**
   * Sets the value of the schemaLocation property.
   *
   * @param value allowed object is {@link String }
   */
  public void setSchemaLocation(String value) {
    this.schemaLocation = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Import anImport = (Import) o;
    return org.apache.commons.lang3.StringUtils.equals(getNamespace(), anImport.getNamespace())
        && org.apache.commons.lang3.StringUtils.equals(getSchemaLocation(), anImport.getSchemaLocation());

  }

  @Override
  public int hashCode() {
    int result = getNamespace() != null ? getNamespace().hashCode() : 0;
    result = 31 * result + (getSchemaLocation() != null ? getSchemaLocation().hashCode() : 0);
    return result;
  }
}
