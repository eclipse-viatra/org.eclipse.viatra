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
package org.eclipse.viatra.integration.evm.jdt.wrappers;

import java.util.Set;
import org.eclipse.viatra.integration.evm.jdt.util.QualifiedName;

@SuppressWarnings("all")
public interface ReferenceStorage {
  public abstract Set<QualifiedName> getQualifiedNameReferences();
  
  public abstract Set<String> getSimpleNameReferences();
  
  public abstract Set<String> getRootReferences();
}
