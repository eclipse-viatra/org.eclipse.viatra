/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.viatra.query.runtime.matchers.psystem;

import java.util.Set;

import org.eclipse.viatra.query.runtime.matchers.context.IQueryMetaContext;
import org.eclipse.viatra.query.runtime.matchers.planning.SubPlan;

/**
 * A kind of deferred constraint that can only be checked when a set of deferring variables are all present in a plan.
 * 
 * @author Gabor Bergmann
 * 
 */
public abstract class VariableDeferredPConstraint extends DeferredPConstraint {

    public VariableDeferredPConstraint(PBody pBody,
            Set<PVariable> affectedVariables) {
        super(pBody, affectedVariables);
    }

    public abstract Set<PVariable> getDeferringVariables();

    /**
     * Refine further if needed
     */
    @Override
    public boolean isReadyAt(SubPlan plan, IQueryMetaContext context) {
        return plan.getVisibleVariables().containsAll(getDeferringVariables());
    }

}
