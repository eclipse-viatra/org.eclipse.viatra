/*******************************************************************************
 * Copyright (c) 2010-2013, Zoltan Ujhelyi, Akos Horvath, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Zoltan Ujhelyi, Akos Horvath - initial API and implementation
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.localsearch.operations.extend;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.viatra.query.runtime.base.api.NavigationHelper;
import org.eclipse.viatra.query.runtime.emf.types.EStructuralFeatureInstancesKey;
import org.eclipse.viatra.query.runtime.localsearch.MatchingFrame;
import org.eclipse.viatra.query.runtime.localsearch.matcher.ISearchContext;
import org.eclipse.viatra.query.runtime.localsearch.operations.IIteratingSearchOperation;
import org.eclipse.viatra.query.runtime.matchers.context.IInputKey;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;
import org.eclipse.viatra.query.runtime.matchers.tuple.TupleMask;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuples;

/**
 * Iterates all available {@link EStructuralFeature} elements using an {@link NavigationHelper VIATRA Base
 * indexer}. It is assumed that the base indexer has been registered for the selected reference type.
 * 
 */
public class IterateOverEStructuralFeatureInstances implements IIteratingSearchOperation{

    private final EStructuralFeature feature;
    private final int sourcePosition;
    private final int targetPosition;
    private final EStructuralFeatureInstancesKey type;
    private Iterator<Tuple> it;
    private static final TupleMask indexerMask = TupleMask.empty(2);
    
    public IterateOverEStructuralFeatureInstances(int sourcePosition, int targetPosition, EStructuralFeature feature) {
        this.sourcePosition = sourcePosition;
        this.targetPosition = targetPosition;
        this.feature = feature;
        type = new EStructuralFeatureInstancesKey(feature);
    }
    
    public EStructuralFeature getFeature() {
        return feature;
    }

    @Override
    public void onBacktrack(MatchingFrame frame, ISearchContext context) {
        frame.setValue(sourcePosition, null);
        frame.setValue(targetPosition, null);
        it = null;
    }

    @Override
    public void onInitialize(MatchingFrame frame, ISearchContext context) {
        Iterable<Tuple> tuples = context.getRuntimeContext().enumerateTuples(type, indexerMask, Tuples.staticArityFlatTupleOf());

        it = tuples.iterator();
    }

    @Override
    public boolean execute(MatchingFrame frame, ISearchContext context) {
        if (it.hasNext()) {
            final Tuple next = it.next();
            frame.setValue(sourcePosition, next.get(0));
            frame.setValue(targetPosition, next.get(1));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "extend    "+feature.getContainerClass().getSimpleName()+"."+feature.getName()+"(-"+sourcePosition+", -"+targetPosition+") indexed";
    }

    @Override
    public List<Integer> getVariablePositions() {
        return Arrays.asList(sourcePosition, targetPosition);
    }

    /**
     * @since 1.4
     */
    @Override
    public IInputKey getIteratedInputKey() {
        return type;
    }
    
}
