/*******************************************************************************
 * Copyright (c) 2010-2015, Marton Bur, Zoltan Ujhelyi, Akos Horvath, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Marton Bur, Zoltan Ujhelyi, Akos Horvath - initial API and implementation
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.localsearch.operations.extend.nobase;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.viatra.query.runtime.base.api.NavigationHelper;
import org.eclipse.viatra.query.runtime.localsearch.MatchingFrame;
import org.eclipse.viatra.query.runtime.localsearch.exceptions.LocalSearchException;
import org.eclipse.viatra.query.runtime.localsearch.matcher.ISearchContext;
import org.eclipse.viatra.query.runtime.localsearch.operations.extend.SingleValueExtendOperation;

/**
 * Iterates over all sources of {@link EStructuralFeature} using an {@link NavigationHelper VIATRA Base indexer}.
 * It is assumed that the indexer is initialized for the selected {@link EStructuralFeature}.
 * 
 */
public class ExtendToEStructuralFeatureSource extends SingleValueExtendOperation<Object> {

    private int targetPosition;
    private EStructuralFeature feature;

    public ExtendToEStructuralFeatureSource(int sourcePosition, int targetPosition, EStructuralFeature feature) {
        super(sourcePosition);
        this.targetPosition = targetPosition;
        this.feature = feature;
    }

    public EStructuralFeature getFeature() {
        return feature;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Iterator<?> getIterator(MatchingFrame frame, ISearchContext context) {
        if(!(feature instanceof EReference)){
            throw new LocalSearchException("Without base index, inverse navigation only possible along "
                    + "EReferences with defined EOpposite.");
        }
        EReference oppositeFeature = ((EReference)feature).getEOpposite();
        if(oppositeFeature == null){
            throw new LocalSearchException("Feature has no EOpposite, so cannot do inverse navigation " + feature.toString());            
        }
        try {
            final EObject value = (EObject) frame.getValue(targetPosition);
            if(! oppositeFeature.getEContainingClass().isSuperTypeOf(value.eClass()) ){
                // TODO planner should ensure the proper supertype relation
                return Collections.emptyIterator();
            }
            final Object featureValue = value.eGet(oppositeFeature);
            if (oppositeFeature.isMany()) {
                if (featureValue != null) {
                    final Collection<Object> objectCollection = (Collection<Object>) featureValue;
                    return objectCollection.iterator();
                } else {
                    return Collections.emptyIterator();
                }
            } else {
                if (featureValue != null) {
                    return Collections.singleton(featureValue).iterator();
                } else {
                    return Collections.emptyIterator();
                }
            }
        } catch (ClassCastException e) {
            throw new LocalSearchException("Invalid feature target in parameter" + Integer.toString(targetPosition), e);
        }
    }
    
    @Override
    public String toString() {
        return "extend    "+feature.getContainerClass().getSimpleName()+"."+feature.getName()+"(-"+position+", +"+targetPosition+") iterating";
    }

    @Override
    public List<Integer> getVariablePositions() {
        return Arrays.asList(position, targetPosition);
    }
    
}
