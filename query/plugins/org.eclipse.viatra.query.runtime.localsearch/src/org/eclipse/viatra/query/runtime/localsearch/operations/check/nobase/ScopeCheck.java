/*******************************************************************************
 * Copyright (c) 2010-2015, Marton Bur, Zoltan Ujhelyi, Akos Horvath, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Marton Bur - initial API and implementation
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.localsearch.operations.check.nobase;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.viatra.query.runtime.base.api.filters.IBaseIndexObjectFilter;
import org.eclipse.viatra.query.runtime.emf.EMFScope;
import org.eclipse.viatra.query.runtime.localsearch.MatchingFrame;
import org.eclipse.viatra.query.runtime.localsearch.matcher.ISearchContext;
import org.eclipse.viatra.query.runtime.localsearch.operations.CheckOperationExecutor;
import org.eclipse.viatra.query.runtime.localsearch.operations.ISearchOperation;

/**
 * This operation simply checks if a model element is part of the Query Scope
 * 
 * @author Marton Bur
 *
 */
public class ScopeCheck implements ISearchOperation {
    private class Executor extends CheckOperationExecutor {
        
        @Override
        protected boolean check(MatchingFrame frame, ISearchContext context) {
            Objects.requireNonNull(frame.getValue(position), () -> String.format("Invalid plan, variable %d unbound", position));
            Object value = frame.getValue(position);
            if(value instanceof EObject){
                EObject eObject = (EObject) value;
                IBaseIndexObjectFilter filterConfiguration = scope.getOptions().getObjectFilterConfiguration();
                boolean filtered = false;
                if(filterConfiguration != null){
                    filtered = filterConfiguration.isFiltered(eObject);
                }
                if(filtered){
                    return false;
                } else {
                    return EcoreUtil.isAncestor(scope.getScopeRoots(), eObject);
                }
            } else {
                return true;            
            }
        }

        @Override
        public ISearchOperation getOperation() {
            return ScopeCheck.this;
        }
        
    }

    private int position;
    private EMFScope scope;

    public ScopeCheck(int position, EMFScope scope) {
        this.position = position;
        this.scope = scope;

    }

    @Override
    public ISearchOperationExecutor createExecutor() {
        return new Executor();
    }

    @Override
    public String toString() {
        return toString(Object::toString);
    }
    
    
    @Override
    public String toString(Function<Integer, String> variableMapping) {
        return "check    +"+variableMapping.apply(position) +" in scope "+scope;
    }

    @Override
    public List<Integer> getVariablePositions() {
        return Arrays.asList(position);
    }
}
