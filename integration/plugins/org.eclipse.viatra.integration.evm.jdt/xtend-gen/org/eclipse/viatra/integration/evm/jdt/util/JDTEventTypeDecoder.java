/**
 * Copyright (c) 2015-2016, IncQuery Labs Ltd. and Ericsson AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Abel Hegedus, Daniel Segesdi, Robert Doczi, Zoltan Ujhelyi - initial API and implementation
 */
package org.eclipse.viatra.integration.evm.jdt.util;

import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.viatra.transformation.evm.specific.crud.CRUDEventTypeEnum;

@SuppressWarnings("all")
public class JDTEventTypeDecoder {
  public static CRUDEventTypeEnum toEventType(final int value) {
    switch (value) {
      case IJavaElementDelta.ADDED:
        return CRUDEventTypeEnum.CREATED;
      case IJavaElementDelta.REMOVED:
        return CRUDEventTypeEnum.DELETED;
      case IJavaElementDelta.CHANGED:
        return CRUDEventTypeEnum.UPDATED;
      default:
        throw new IllegalArgumentException("Event type value is invalid.");
    }
  }
}
