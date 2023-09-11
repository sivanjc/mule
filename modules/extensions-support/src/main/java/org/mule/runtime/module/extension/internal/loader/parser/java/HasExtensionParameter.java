/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;

/**
 * Indicates that the classes that implement this interface rely on an ExtensionParameter
 *
 * @since 4.5
 */
public interface HasExtensionParameter {

  ExtensionParameter getExtensionParameter();

}
