/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.hackathon;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class AstBuffers {

  private final StringBuffer nodes;
  private final StringBuffer edges;

  public AstBuffers() {
    nodes = new StringBuffer();
    edges = new StringBuffer();
    appendToNode("Id,Label,Type\n");
    appendToEdge("Source,Target,Type,Id\n");
  }

  public void appendToNode(String toAppend) {
    nodes.append(toAppend);
  }

  public void appendToEdge(String toAppend) {
    edges.append(toAppend);
  }

  public StringBuffer getNodes() {
    return nodes;
  }

  public StringBuffer getEdges() {
    return edges;
  }

  public void printToFile(String prefix) {
    printToFile(prefix.concat("_nodes.csv"), nodes);
    printToFile(prefix.concat("_edges.csv"), edges);
  }

  private void printToFile(String fileName, StringBuffer stringBuffer) {
    try (BufferedWriter bwr = new BufferedWriter(new FileWriter(fileName))) {
      bwr.write(stringBuffer.toString());
      bwr.flush();
    } catch (IOException ignored) {
      throw new RuntimeException(ignored);
    }
  }
}
