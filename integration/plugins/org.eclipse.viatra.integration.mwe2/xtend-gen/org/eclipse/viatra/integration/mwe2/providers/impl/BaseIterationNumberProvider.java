/**
 * Copyright (c) 2004-2015, Peter Lunk, Zoltan Ujhelyi and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Peter Lunk - initial API and implementation
 */
package org.eclipse.viatra.integration.mwe2.providers.impl;

import org.eclipse.emf.mwe2.runtime.workflow.IWorkflowContext;
import org.eclipse.viatra.integration.mwe2.providers.IIterationNumberProvider;
import org.eclipse.viatra.integration.mwe2.providers.impl.BaseProvider;

/**
 * Basic iteration number provider. Returns the number handed to it in its constructor.
 * @author Peter Lunk
 */
@SuppressWarnings("all")
public class BaseIterationNumberProvider extends BaseProvider implements IIterationNumberProvider {
  private IWorkflowContext ctx;
  
  private Integer value;
  
  public BaseIterationNumberProvider(final Integer value) {
    this.value = value;
  }
  
  @Override
  public IWorkflowContext getContext() {
    return this.ctx;
  }
  
  @Override
  public void setContext(final IWorkflowContext ctx) {
    this.ctx = ctx;
  }
  
  @Override
  public Integer getIterationNumber() {
    return this.value;
  }
}
