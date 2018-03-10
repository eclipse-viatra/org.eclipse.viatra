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

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.viatra.query.runtime.base.api.NavigationHelper;
import org.eclipse.viatra.query.runtime.emf.types.EStructuralFeatureInstancesKey;
import org.eclipse.viatra.query.runtime.localsearch.MatchingFrame;
import org.eclipse.viatra.query.runtime.localsearch.matcher.ISearchContext;
import org.eclipse.viatra.query.runtime.localsearch.operations.IIteratingSearchOperation;
import org.eclipse.viatra.query.runtime.matchers.context.IInputKey;
import org.eclipse.viatra.query.runtime.matchers.tuple.TupleMask;
import org.eclipse.viatra.query.runtime.matchers.tuple.VolatileMaskedTuple;

import com.google.common.collect.Iterables;

/**
 * Iterates over all sources of {@link EStructuralFeature} using an {@link NavigationHelper VIATRA Base indexer}.
 * It is assumed that the indexer is initialized for the selected {@link EStructuralFeature}.
 * 
 */
public class ExtendToEStructuralFeatureSource extends SingleValueExtendOperation<EObject> implements IIteratingSearchOperation{

    private int targetPosition;
    private EStructuralFeature feature;
    private final IInputKey type;
    private VolatileMaskedTuple maskedTuple;
    private static final TupleMask indexerMask = TupleMask.fromSelectedIndices(2, new int[] {1});
    
    /**
     * @since 1.7
     */
    public ExtendToEStructuralFeatureSource(int sourcePosition, int targetPosition, EStructuralFeature feature, TupleMask mask) {
        super(sourcePosition);
        this.targetPosition = targetPosition;
        this.feature = feature;
        this.type = new EStructuralFeatureInstancesKey(feature);
        this.maskedTuple = new VolatileMaskedTuple(mask);
    }

    public EStructuralFeature getFeature() {
        return feature;
    }

    @Override
    public Iterator<EObject> getIterator(MatchingFrame frame, ISearchContext context) {
        maskedTuple.updateTuple(frame);
        Iterable<? extends Object> values = context.getRuntimeContext().enumerateValues(type, indexerMask, maskedTuple);
        return Iterables.filter(values, EObject.class).iterator();
    }
    
    @Override
    public String toString() {
        return "extend    "+feature.getContainerClass().getSimpleName()+"."+feature.getName()+"(-"+position+", +"+targetPosition+") indexed";
    }

    @Override
    public List<Integer> getVariablePositions() {
        return Arrays.asList(position, targetPosition);
    }
    
    /**
     * @since 1.4
     */
    @Override
    public IInputKey getIteratedInputKey() {
        return type;
    }
    
}
