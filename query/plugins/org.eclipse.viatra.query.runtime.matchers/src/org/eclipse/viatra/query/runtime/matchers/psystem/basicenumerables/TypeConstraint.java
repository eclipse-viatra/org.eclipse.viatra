/*******************************************************************************
 * Copyright (c) 2010-2014, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.viatra.query.runtime.matchers.context.IInputKey;
import org.eclipse.viatra.query.runtime.matchers.context.IQueryMetaContext;
import org.eclipse.viatra.query.runtime.matchers.psystem.ITypeConstraint;
import org.eclipse.viatra.query.runtime.matchers.psystem.KeyedEnumerablePConstraint;
import org.eclipse.viatra.query.runtime.matchers.psystem.PBody;
import org.eclipse.viatra.query.runtime.matchers.psystem.PVariable;
import org.eclipse.viatra.query.runtime.matchers.psystem.TypeJudgement;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;

/**
 * Represents an enumerable type constraint that asserts that values substituted for the given tuple of variables 
 * 	form a tuple that belongs to an enumerable extensional relation identified by an {@link IInputKey}.
 *
 * <p> The InputKey must be enumerable!
 *
 * @author Zoltan Ujhelyi
 *
 */
public class TypeConstraint extends KeyedEnumerablePConstraint<IInputKey> implements ITypeConstraint {
    
    private TypeJudgement equivalentJudgement;
    
    public TypeConstraint(PBody pBody, Tuple variablesTuple, IInputKey inputKey) {
        super(pBody, variablesTuple, inputKey);
        this.equivalentJudgement = new TypeJudgement(inputKey, variablesTuple);
        
        if (! inputKey.isEnumerable())
            throw new IllegalArgumentException(
                    this.getClass().getSimpleName() + 
                    " applicable for enumerable input keys only; received instead " + 
                            inputKey);
        if (variablesTuple.getSize() != inputKey.getArity())
            throw new IllegalArgumentException(
                    this.getClass().getSimpleName() + 
                    " applied for variable tuple " + variablesTuple + " having wrong arity for input key " + 
                            inputKey);
    }

    @Override
    protected String keyToString() {
        return supplierKey.getPrettyPrintableName();
    }
    
    @Override
    public TypeJudgement getEquivalentJudgement() {
        return equivalentJudgement;
    }

    @Override
    public Set<TypeJudgement> getImpliedJudgements(IQueryMetaContext context) {
        return Collections.singleton(equivalentJudgement);
        //return equivalentJudgement.getDirectlyImpliedJudgements(context);
    }
    
    @Override
    public Map<Set<PVariable>, Set<PVariable>> getFunctionalDependencies(IQueryMetaContext context) {
        return TypeConstraintUtil.getFunctionalDependencies(context, supplierKey, variablesTuple);
    }

    @Override
    public void doReplaceVariable(PVariable obsolete, PVariable replacement) {
        super.doReplaceVariable(obsolete, replacement);
        this.equivalentJudgement = new TypeJudgement(getSupplierKey(), variablesTuple);
    }
}