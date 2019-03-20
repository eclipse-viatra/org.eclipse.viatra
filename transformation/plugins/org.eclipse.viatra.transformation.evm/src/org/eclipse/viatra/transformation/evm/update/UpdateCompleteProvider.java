/*******************************************************************************
 * Copyright (c) 2010-2012, Abel Hegedus, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.transformation.evm.update;

import java.util.HashSet;
import java.util.Set;

/**
 * This abstract implementation allows the registration of listeners and calls them when 
 * an update complete event occurs. The class is abstract since there is no definition of
 * the actual update complete event.
 * 
 * @author Abel Hegedus
 * 
 */
public abstract class UpdateCompleteProvider implements IUpdateCompleteProvider {

    private final Set<IUpdateCompleteListener> listeners;

    public UpdateCompleteProvider() {
        listeners = new HashSet<IUpdateCompleteListener>();
    }

    @Override
    public boolean addUpdateCompleteListener(final IUpdateCompleteListener listener, final boolean fireNow) {
        boolean empty = listeners.isEmpty();
        boolean added = listeners.add(listener);
        if (added) {
            if(empty) {
                firstListenerAdded();
            }
            if(fireNow) {
                listener.updateComplete();
            }
        }
        return added;
    }
    
    protected void firstListenerAdded() {}
    
    protected void lastListenerRemoved() {}

    @Override
    public boolean removeUpdateCompleteListener(final IUpdateCompleteListener listener) {
        boolean removed = this.listeners.remove(listener);
        if(removed && listeners.isEmpty()) {
            lastListenerRemoved();
        }
        return removed;
    }

    /**
     * Notifies each listener that an update complete event occurred.
     */
    protected void updateCompleted() {
        for (IUpdateCompleteListener listener : this.listeners) {
            listener.updateComplete();
        }
    }

    /**
     * Disposes of the provider by clearing the listener list
     */
    public void dispose() {
        if(!listeners.isEmpty()) {
            lastListenerRemoved();
        }
        listeners.clear();
    }

}
