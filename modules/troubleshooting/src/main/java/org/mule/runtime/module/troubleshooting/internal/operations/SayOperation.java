/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.troubleshooting.internal.operations;

import static java.lang.String.format;

import org.mule.runtime.module.troubleshooting.api.AnnotatedTroubleshootingOperationCallback;
import org.mule.runtime.module.troubleshooting.api.Argument;
import org.mule.runtime.module.troubleshooting.api.Operation;

import java.io.Serializable;

@Operation(name = "say", description = "Makes the Mule talk")
public class SayOperation implements AnnotatedTroubleshootingOperationCallback {

  @Argument(description = "The message to say")
  private String message;

  @Override
  public Serializable execute() {
    return format(MESSAGE_FORMAT, message);
  }

  private static final String MESSAGE_FORMAT = "\n ____________________\n< %s >\n" +
      " --------------------\n" +
      "     \\\n" +
      "      \\\n" +
      "  /\\          /\\\n" +
      " ( \\\\        // )\n" +
      "  \\ \\\\      // /\n" +
      "   \\_\\\\||||//_/\n" +
      "     / _  _ \\/\n" +
      "     |(o)(o)|\\/\n" +
      "     |      | \\/\n" +
      "     \\      /  \\/_____________________\n" +
      "      |____|     \\\\                  \\\\\n" +
      "     /      \\     ||                  \\\\\n" +
      "     \\ 0  0 /     |/                  |\\\\\n" +
      "      \\____/ \\    V           (       / \\\\\n" +
      "       / \\    \\     )          \\     /   \\\\\n" +
      "      / | \\    \\_|  |___________\\   /     ))\n" +
      "                  ||  |     \\   /\\  \\\n" +
      "                  ||  /      \\  \\ \\  \\\n" +
      "                  || |        | |  | |\n" +
      "                  || |        | |  | |\n" +
      "                  ||_|        |_|  |_|\n" +
      "                 //_/        /_/  /_/\n";
}
