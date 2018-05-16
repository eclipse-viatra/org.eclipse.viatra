/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Tamas Szabo - initial API and implementation
 *******************************************************************************/

package org.eclipse.viatra.query.runtime.base.itc.alg.misc.topsort;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.eclipse.viatra.query.runtime.base.itc.igraph.IGraphDataSource;

/**
 * @since 1.6
 */
public class TopologicalSorting {

    private TopologicalSorting() {/*Utility class constructor*/}
    
    private static final class Pair<T> {
        public T element;
        public boolean isParent;

        public Pair(final T element, final boolean isParent) {
            this.element = element;
            this.isParent = isParent;
        }
    }

    /**
     * Returns a topological ordering for the given graph data source. 
     * Output format: if there is an a -> b (transitive) reachability, then node <code>a</code> will come before node <code>b</code> in the resulting list.  
     * 
     * @param gds the graph data source
     * @return a topological ordering
     */
    public static <T> List<T> compute(final IGraphDataSource<T> gds) {
        final Set<T> visited = new HashSet<T>();
        final LinkedList<T> result = new LinkedList<T>();
        final Stack<Pair<T>> dfsStack = new Stack<Pair<T>>();

        for (final T node : gds.getAllNodes()) {
            if (!visited.contains(node)) {
                dfsStack.push(new Pair<T>(node, false));                
            }
            
            while (!dfsStack.isEmpty()) {
                final Pair<T> head = dfsStack.pop();
                final T source = head.element;
                
                if (head.isParent) {
                    // we have already seen source, push it to the resulting stack
                    result.addFirst(source);
                } else {
                    // first time we see source, continue with its children
                    visited.add(source);
                    dfsStack.push(new Pair<T>(source, true));
                    
                    for (final T target : gds.getTargetNodes(source).distinctValues()) {
                        if (!visited.contains(target)) {
                            dfsStack.push(new Pair<T>(target, false));
                        }
                    }
                }
            }
        }

        return result;
    }
}
