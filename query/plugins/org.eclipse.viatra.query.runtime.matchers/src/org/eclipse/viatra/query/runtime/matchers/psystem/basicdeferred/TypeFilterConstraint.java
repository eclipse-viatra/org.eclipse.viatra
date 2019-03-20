/*******************************************************************************
 * Copyright (c) 2010-2015, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.matchers.psystem.basicdeferred;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.viatra.query.runtime.matchers.context.IInputKey;
import org.eclipse.viatra.query.runtime.matchers.context.IQueryMetaContext;
import org.eclipse.viatra.query.runtime.matchers.psystem.ITypeConstraint;
import org.eclipse.viatra.query.runtime.matchers.psystem.PBody;
import org.eclipse.viatra.query.runtime.matchers.psystem.PVariable;
import org.eclipse.viatra.query.runtime.matchers.psystem.TypeJudgement;
import org.eclipse.viatra.query.runtime.matchers.psystem.VariableDeferredPConstraint;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.TypeConstraint;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;

/**
 * Represents a non-enumerable type constraint that asserts that values substituted for the given tuple of variables 
 * 	form a tuple that belongs to a (typically non-enumerable) extensional relation identified by an {@link IInputKey}.
 * 
 * <p> The InputKey is typically not enumerable. If it is enumerable, use {@link TypeConstraint} instead, so that the PConstraint carries over the property of enumerability.
 * 
 * @author Bergmann Gabor
 *
 */
public class TypeFilterConstraint extends VariableDeferredPConstraint implements
        ITypeConstraint {

    private Tuple variablesTuple;
    private IInputKey inputKey;
    
    private TypeJudgement equivalentJudgement;

    
    public TypeFilterConstraint(PBody pBody, Tuple variablesTuple, IInputKey inputKey) {
        super(pBody, variablesTuple.<PVariable> getDistinctElements());
        this.equivalentJudgement = new TypeJudgement(inputKey, variablesTuple);

        this.variablesTuple = variablesTuple;
        this.inputKey = inputKey;
        
        if (variablesTuple.getSize() != inputKey.getArity())
            throw new IllegalArgumentException(
                    this.getClass().getSimpleName() + 
                    " applied for variable tuple " + variablesTuple + " having wrong arity for input key " + 
                            inputKey);
   }

    
    
    public Tuple getVariablesTuple() {
        return variablesTuple;
    }

    public IInputKey getInputKey() {
        return inputKey;
    }

    @Override
    public TypeJudgement getEquivalentJudgement() {
        return equivalentJudgement;
    }

    @Override
    protected void doReplaceVariable(PVariable obsolete, PVariable replacement) {
        variablesTuple = variablesTuple.replaceAll(obsolete, replacement);
        this.equivalentJudgement = new TypeJudgement(inputKey, variablesTuple);
    }

    @Override
    public Set<TypeJudgement> getImpliedJudgements(IQueryMetaContext context) {
        return Collections.singleton(equivalentJudgement);
    }

    @Override
    public Set<PVariable> getDeducedVariables() {
        return Collections.emptySet();
    }

    @Override
    public Set<PVariable> getDeferringVariables() {
        return getAffectedVariables();
    }

    @Override
    protected String toStringRest() {
        return inputKey.getPrettyPrintableName() + "@" + variablesTuple;
    }
    
    @Override
    public Map<Set<PVariable>, Set<PVariable>> getFunctionalDependencies(IQueryMetaContext context) {
        return TypeConstraintUtil.getFunctionalDependencies(context, inputKey, variablesTuple);
    }

    
    
}
