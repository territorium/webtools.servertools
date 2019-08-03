/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core.internal.xml;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An XML element.
 */
public class XMLElement {

  private Element xmlElement;
  private Factory factory;

  public XMLElement() {
    // do nothing
  }

  public Element getElementNode() {
    return xmlElement;
  }

  private Attr addAttribute(String s, String s1) {
    Attr attr = factory.createAttribute(s, xmlElement);
    attr.setValue(s1);
    return attr;
  }

  public XMLElement createElement(int index, String s) {
    return factory.createElement(index, s, xmlElement);
  }

  public XMLElement createElement(String s) {
    return factory.createElement(s, xmlElement);
  }

  public XMLElement findElement(String s) {
    NodeList nodelist = xmlElement.getElementsByTagName(s);
    int i = nodelist == null ? 0 : nodelist.getLength();
    for (int j = 0; j < i; j++) {
      Node node = nodelist.item(j);
      String s1 = node.getNodeName().trim();
      if (s1.equals(s)) {
        return factory.newInstance((Element) node);
      }
    }

    return createElement(s);
  }

  public XMLElement findElement(String s, int i) {
    NodeList nodelist = xmlElement.getElementsByTagName(s);
    int j = nodelist == null ? 0 : nodelist.getLength();
    for (int k = 0; k < j; k++) {
      Node node = nodelist.item(k);
      String s1 = node.getNodeName().trim();
      if (s1.equals(s) && (k == i)) {
        return factory.newInstance((Element) node);
      }
    }

    return createElement(s);
  }

  public String getAttributeValue(String s) {
    Attr attr = xmlElement.getAttributeNode(s);
    if (attr != null) {
      return attr.getValue();
    }

    return null;
  }

  public Map<String, String> getAttributes() {
    Map<String, String> attributes = new LinkedHashMap<>();
    NamedNodeMap attrs = xmlElement.getAttributes();
    if (null != attrs) {
      for (int i = 0; i < attrs.getLength(); i++) {
        Node attr = attrs.item(i);
        String name = attr.getNodeName();
        String value = attr.getNodeValue();
        attributes.put(name, value);
      }
    }
    return attributes;
  }

  public String getElementName() {
    return xmlElement.getNodeName();
  }

  public String getElementValue() {
    return XMLElement.getElementValue(xmlElement);
  }

  private static String getElementValue(Element element) {
    String s = element.getNodeValue();
    if (s != null) {
      return s;
    }
    NodeList nodelist = element.getChildNodes();
    for (int i = 0; i < nodelist.getLength(); i++) {
      if (nodelist.item(i) instanceof Text) {
        return ((Text) nodelist.item(i)).getData();
      }
    }

    return null;
  }

  public boolean removeAttribute(String s) {
    try {
      xmlElement.removeAttribute(s);
      return true;
    } catch (Exception ex) {
      return false;
    }
  }

  public boolean removeElement(String s, int i) {
    NodeList nodelist = xmlElement.getElementsByTagName(s);
    int j = nodelist == null ? 0 : nodelist.getLength();
    for (int k = 0; k < j; k++) {
      Node node = nodelist.item(k);
      String s1 = node.getNodeName().trim();
      if (s1.equals(s) && (k == i)) {
        xmlElement.removeChild(node);
        return true;
      }
    }

    return false;
  }

  public void setAttributeValue(String s, String s1) {
    Attr attr = xmlElement.getAttributeNode(s);
    if (attr == null) {
      attr = addAttribute(s, s1);
    } else {
      attr.setValue(s1);
    }
  }

  void setElement(Element element) {
    xmlElement = element;
  }

  void setFactory(Factory factory1) {
    factory = factory1;
  }

  public int sizeOfElement(String s) {
    NodeList nodelist = xmlElement.getElementsByTagName(s);
    int i = nodelist == null ? 0 : nodelist.getLength();
    return i;
  }

  public boolean hasChildNodes() {
    return xmlElement.hasChildNodes();
  }

  public void removeChildren() {
    while (xmlElement.hasChildNodes()) {
      xmlElement.removeChild(xmlElement.getFirstChild());
    }
  }

  public void copyChildrenTo(XMLElement destination) {
    NodeList nodelist = xmlElement.getChildNodes();
    int len = nodelist == null ? 0 : nodelist.getLength();
    for (int i = 0; i < len; i++) {
      Node node = nodelist.item(i);
      destination.importNode(node, true);
    }
  }

  public void importNode(Node node, boolean deep) {
    xmlElement.appendChild(xmlElement.getOwnerDocument().importNode(node, deep));
  }

  /**
   * This method tries to compare two XMLElements for equivalence. Due to the lack of normalization,
   * they aren't compared for equality. Elements are required to have the same attributes or the
   * same node value if attributes aren't present. Attributes and node value are assumed to be
   * mutually exclusive for smart.IO configuration XML files. The same non-text child nodes are
   * required to be present in an element and appear in the same order. If a node type other than
   * element or comment is encountered, this method punts and returns false.
   * 
   * @param obj XMLElement to compare
   * @return true if the elements are equivalent
   */
  public boolean isEquivalent(XMLElement obj) {
    if (obj != null) {
      try {
        return XMLElement.elementsAreEquivalent(xmlElement, obj.getElementNode());
      } catch (Exception e) {
        // Catch and ignore just to be safe
      }
    }
    return false;
  }

  /**
   * Same as isEquivalent() but doesn't ignore exceptions for test purposes. This avoids hiding an
   * expected mismatch behind an unexpected exception.
   * 
   * @param obj XMLElement to compare
   * @return true if the elements are equivalent
   */
  public boolean isEquivalentTest(XMLElement obj) {
    if (obj != null) {
      return XMLElement.elementsAreEquivalent(xmlElement, obj.getElementNode());
    }
    return false;
  }

  private static boolean elementsAreEquivalent(Element element, Element otherElement) {
    if (element == otherElement) {
      return true;
    }

    if (!element.getNodeName().equals(otherElement.getNodeName())) {
      return false;
    }

    if (element.hasChildNodes()) {
      if (otherElement.hasChildNodes() && XMLElement.attributesAreEqual(element, otherElement)) {
        // Compare child nodes
        NodeList nodelist = element.getChildNodes();
        NodeList otherNodelist = otherElement.getChildNodes();
        if (nodelist.getLength() == otherNodelist.getLength()) {
          Node node = XMLElement.nextNonTextNode(element.getFirstChild());
          Node otherNode = XMLElement.nextNonTextNode(otherElement.getFirstChild());
          while (node != null) {
            if (otherNode == null) {
              return false;
            }
            short nextNodeType = node.getNodeType();
            if (nextNodeType != otherNode.getNodeType()) {
              return false;
            }
            // If elements, compare
            if (nextNodeType == Node.ELEMENT_NODE) {
              if (!XMLElement.elementsAreEquivalent((Element) node, (Element) otherNode)) {
                return false;
              }
            }
            // Else if comment, compare
            else if (nextNodeType == Node.COMMENT_NODE) {
              if (!XMLElement.nodeValuesAreEqual(node, otherNode)) {
                return false;
              }
            }
            // Else punt on other node types
            else {
              return false;
            }
            node = XMLElement.nextNonTextNode(node.getNextSibling());
            otherNode = XMLElement.nextNonTextNode(otherNode.getNextSibling());
          }
          // If also at end of other children, return equal
          if (otherNode == null) {
            return true;
          }
        }
      }
    } else if (!otherElement.hasChildNodes()) {
      return XMLElement.attributesAreEqual(element, otherElement);
    }
    return false;
  }

  private static Node nextNonTextNode(Node node) {
    while ((node != null) && (node.getNodeType() == Node.TEXT_NODE)) {
      node = node.getNextSibling();
    }
    return node;
  }

  private static boolean attributesAreEqual(Element element, Element otherElement) {
    NamedNodeMap attrs = element.getAttributes();
    NamedNodeMap otherAttrs = otherElement.getAttributes();
    if ((attrs == null) && (otherAttrs == null)) {
      // Return comparison of element values if there are no attributes
      return XMLElement.nodeValuesAreEqual(element, otherElement);
    }

    if (attrs.getLength() == otherAttrs.getLength()) {
      if (attrs.getLength() == 0) {
        // Return comparison of element values if there are no attributes
        return XMLElement.nodeValuesAreEqual(element, otherElement);
      }

      for (int i = 0; i < attrs.getLength(); i++) {
        Node attr = attrs.item(i);
        Node otherAttr = otherAttrs.getNamedItem(attr.getNodeName());
        if (!XMLElement.nodeValuesAreEqual(attr, otherAttr)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  private static boolean nodeValuesAreEqual(Node node, Node otherNode) {
    String value = node.getNodeValue();
    String otherValue = otherNode.getNodeValue();
    if ((value != null) && (otherValue != null)) {
      if (value.equals(otherValue)) {
        return true;
      }
    } else if ((value == null) && (otherValue == null)) {
      return true;
    }
    return false;
  }
}
