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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.viatra.query.runtime.localsearch.MatchingFrame;
import org.eclipse.viatra.query.runtime.localsearch.matcher.ISearchContext;
import org.eclipse.viatra.query.runtime.matchers.util.Preconditions;

/**
 * Iterates all child elements of a selected EObjects. 
 * 
 * @author Zoltan Ujhelyi
 * 
 */
public class IterateOverContainers extends ExtendOperation<EObject> {


    /**
     * A helper iterator for transitively traversing a parent of an object
     */
    private static final class ParentIterator implements Iterator<EObject> {
        private EObject current;

        public ParentIterator(EObject source) {
            this.current = source;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public EObject next() {
            EObject newObject = current.eContainer();
            if (newObject == null) {
                throw new NoSuchElementException(String.format("No more parents available for EObject %s", current));
            }
            current = newObject;
            return current;
        }

        @Override
        public boolean hasNext() {
            return current.eContainer() != null;
        }
    }

    private int sourcePosition;
    private boolean transitive;

    /**
     * 
     * @param containerPosition the position of the variable storing the found parent elements
     * @param sourcePosition the position of the variable storing the selected element; must be bound
     * @param transitive if false, only the direct container is returned; otherwise all containers
     */
    public IterateOverContainers(int containerPosition, int sourcePosition, boolean transitive) {
        super(containerPosition);
        this.sourcePosition = sourcePosition;
        this.transitive = transitive;
    }

    @Override
    public void onInitialize(MatchingFrame frame, ISearchContext context) {
        Preconditions.checkState(frame.get(sourcePosition) instanceof EObject, "Only children of EObject elements are supported.");
        EObject source = (EObject) frame.get(sourcePosition);
        EObject container = source.eContainer();
        if (container == null) {
            it = Collections.emptyIterator();
        } else if (transitive) {
            it = new ParentIterator(source);
        } else { 
            it = Collections.singleton(container).iterator();
        }
    }

    @Override
    public String toString() {
        return "extend    containment -"+sourcePosition+" <>--> +"+position+(transitive ? " transitively" : " directly");
    }

    @Override
    public List<Integer> getVariablePositions() {
        return Arrays.asList(position, sourcePosition);
    }
    
}
