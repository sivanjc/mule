/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.hackathon;

import com.sun.istack.NotNull;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.ast.api.ArtifactAst;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AstDumper {

  public void dump(String prefixName, ArtifactAst artifactAst) {
    AstBuffers astBuffers = new AstBuffers();
    Reference<Integer> count = new Reference<>(0);
    artifactAst.topLevelComponentsStream()
        .filter(x -> "flow".equals(x.getIdentifier().getName()) || "sub-flow".equals(x.getIdentifier().getName()))
        .forEach(topLevel -> {
          String sourceName = topLevel.getParameter("General", "name").getRawValue();
          astBuffers.getNodes().append(sourceName).append(",").append(sourceName).append(",")
              .append(topLevel.getIdentifier().getName()).append("\n");
          topLevel.recursiveStream()
              .filter(x -> "flow-ref".equals(x.getIdentifier().getName()))
              .forEach(flowRefAst -> {
                String targetName = flowRefAst.getParameter("General", "name").getRawValue();
                astBuffers.getEdges().append(sourceName).append(",").append(targetName).append(",flowRef,").append(count.get())
                    .append("\n");
                count.set(count.get() + 1);
              });
        });

    Map<String, List<String>> listeners = new HashMap<>();
    Map<String, List<String>> publishers = new HashMap<>();
    artifactAst.topLevelComponentsStream()
        .filter(x -> "flow".equals(x.getIdentifier().getName()) || "sub-flow".equals(x.getIdentifier().getName()))
        .forEach(topLevel -> {
          String sourceName = topLevel.getParameter("General", "name").getRawValue();
          topLevel.recursiveStream()
              .filter(x -> ("listener".equals(x.getIdentifier().getName()) || "publish".equals(x.getIdentifier().getName()))
                  && "vm".equals(x.getIdentifier().getNamespace()))
              .forEach(vmAst -> {
                String queue = vmAst.getParameter("queue", "queueName").getRawValue();
                if ("listener".equals(vmAst.getIdentifier().getName())) {
                  listeners.putIfAbsent(queue, new ArrayList<>());
                  listeners.get(queue).add(sourceName);
                } else {
                  publishers.putIfAbsent(queue, new ArrayList<>());
                  publishers.get(queue).add(sourceName);
                }
              });
        });

    listeners.forEach((queue, listenerFlows) -> {
      publishers.get(queue).forEach(publisherFlow -> {
        listenerFlows.forEach(listenerFlow -> astBuffers.getEdges().append(publisherFlow).append(",").append(listenerFlow)
            .append(",vm,").append(count.get())
            .append("\n"));
      });
    });


    astBuffers.printToFile(prefixName);
  }
}
