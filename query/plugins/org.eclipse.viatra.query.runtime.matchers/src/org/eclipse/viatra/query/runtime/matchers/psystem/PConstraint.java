/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Gabor Bergmann - initial API and implementation
 *******************************************************************************/

package org.eclipse.viatra.query.runtime.matchers.psystem;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import org.eclipse.viatra.query.runtime.matchers.context.IQueryMetaContext;
import org.eclipse.viatra.query.runtime.matchers.psystem.analysis.QueryAnalyzer;

/**
 * @author Gabor Bergmann
 * 
 */
public interface PConstraint extends PTraceable {

    /**
     * All variables affected by this constraint.
     */
    public Set<PVariable> getAffectedVariables();

    /**
     * The set of variables whose potential values can be enumerated (once all non-deduced variables have known values).  
     */
    public Set<PVariable> getDeducedVariables();
    
    /**
     * A (preferably minimal) cover of known functional dependencies between variables.
     * @noreference Use {@link QueryAnalyzer} instead to properly handle dependencies of pattern calls. 
     * @return non-trivial functional dependencies in the form of {variables} --> {variables}, where dependencies with the same lhs are unified.   
     */
    public Map<Set<PVariable>,Set<PVariable>> getFunctionalDependencies(IQueryMetaContext context);  

    public void replaceVariable(PVariable obsolete, PVariable replacement);

    public void delete();

    public void checkSanity();

    /**
     * Returns an integer ID that is guaranteed to increase strictly monotonously for constraints within a pBody.
     */
    public abstract int getMonotonousID();
    
    /**
     * A comparator that orders constraints by their {@link #getMonotonousID() monotonous identifiers}. Should only used
     * for tiebreaking in other comparators.
     * 
     * @since 2.0
     */
    public static final Comparator<PConstraint> COMPARE_BY_MONOTONOUS_ID = (arg0, arg1) -> arg0.getMonotonousID() - arg1.getMonotonousID();

}
