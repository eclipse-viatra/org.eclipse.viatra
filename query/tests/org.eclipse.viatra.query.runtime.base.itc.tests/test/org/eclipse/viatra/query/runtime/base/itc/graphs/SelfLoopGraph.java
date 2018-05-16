/*******************************************************************************
 * Copyright (c) 2010-2012, Abel Hegedus, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Abel Hegedus - initial API and implementation
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.base.itc.graphs;

/**
 * @author Abel Hegedus
 *
 */
public class SelfLoopGraph extends TestGraph<Integer> {

    public SelfLoopGraph() {
        super(null);
    }
    
    @Override
    public void modify() {
        Integer n1 = Integer.valueOf(1);
        this.insertNode(n1);
        this.insertEdge(n1, n1);
        this.deleteEdgeIfExists(n1, n1);
    }

}
