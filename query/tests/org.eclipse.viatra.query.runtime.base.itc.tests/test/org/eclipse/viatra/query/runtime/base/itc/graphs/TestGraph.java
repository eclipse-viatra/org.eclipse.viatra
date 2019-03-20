/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.base.itc.graphs;

import org.eclipse.viatra.query.runtime.base.itc.graphimpl.Graph;
import org.eclipse.viatra.query.runtime.base.itc.misc.TestObserver;

public abstract class TestGraph<T> extends Graph<T> {

    protected TestObserver<Integer> observer;
    
    public TestGraph(TestObserver<Integer> observer) {
        this.observer = observer;
    }
    
    public abstract void modify();
    
    public TestObserver<Integer> getObserver() {
        return observer;
    }
    
}
