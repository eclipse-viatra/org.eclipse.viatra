/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.viatra.query.runtime.matchers.context.IQueryMetaContext;
import org.eclipse.viatra.query.runtime.matchers.psystem.KeyedEnumerablePConstraint;
import org.eclipse.viatra.query.runtime.matchers.psystem.PBody;
import org.eclipse.viatra.query.runtime.matchers.psystem.PVariable;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuples;

/**
 * @author Gabor Bergmann
 * 
 */
public class ConstantValue extends KeyedEnumerablePConstraint<Object> {

    private PVariable variable;

    public ConstantValue(PBody pBody, PVariable variable, Object value) {
        super(pBody, Tuples.staticArityFlatTupleOf(variable), value);
        this.variable = variable;
    }

    @Override
    protected String keyToString() {
        return supplierKey.toString();
    }
    
    /**
     * @since 1.7
     */
    public PVariable getVariable() {
        return variable;
    }
    
    @Override
    public Map<Set<PVariable>, Set<PVariable>> getFunctionalDependencies(IQueryMetaContext context) {
        final Map<Set<PVariable>, Set<PVariable>> result = new HashMap<Set<PVariable>, Set<PVariable>>();
        final Set<PVariable> emptySet = Collections.emptySet(); // a constant value is functionally determined by everything
        result.put(emptySet, Collections.singleton(getVariableInTuple(0)));
        return result;
    }


}
