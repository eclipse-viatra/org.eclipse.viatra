/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.viatra.query.runtime.matchers.psystem.basicdeferred;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.viatra.query.runtime.matchers.context.IQueryMetaContext;
import org.eclipse.viatra.query.runtime.matchers.psystem.PBody;
import org.eclipse.viatra.query.runtime.matchers.psystem.PVariable;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PQuery;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;

/**
 * @author Gabor Bergmann
 */
public class PatternMatchCounter extends PatternCallBasedDeferred {

    private PVariable resultVariable;

    public PatternMatchCounter(PBody pBody, Tuple actualParametersTuple,
            PQuery query, PVariable resultVariable) {
        super(pBody, actualParametersTuple, query, Collections.singleton(resultVariable));
        this.resultVariable = resultVariable;
    }

    @Override
    public Set<PVariable> getDeducedVariables() {
        return Collections.singleton(resultVariable);
    }
    
    @Override
    public Map<Set<PVariable>, Set<PVariable>> getFunctionalDependencies(IQueryMetaContext context) {
        final Map<Set<PVariable>, Set<PVariable>> result = new HashMap<Set<PVariable>, Set<PVariable>>();
        result.put(getDeferringVariables(), getDeducedVariables());
        return result;
    }

    @Override
    protected void doDoReplaceVariables(PVariable obsolete, PVariable replacement) {
        if (resultVariable.equals(obsolete))
            resultVariable = replacement;
    }

    @Override
    protected Set<PVariable> getCandidateQuantifiedVariables() {
        return actualParametersTuple.<PVariable> getDistinctElements();
    }


    @Override
    protected String toStringRest() {
        return query.getFullyQualifiedName() + "@" + actualParametersTuple.toString() + "->"
                + resultVariable.toString();
    }

    public PVariable getResultVariable() {
        return resultVariable;
    }

}