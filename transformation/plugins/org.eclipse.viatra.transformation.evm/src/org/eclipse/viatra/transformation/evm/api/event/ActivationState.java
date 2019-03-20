/*******************************************************************************
 * Copyright (c) 2010-2013, Abel Hegedus, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.transformation.evm.api.event;

/**
 * Interface for specifying a state that an activation can be in.
 * 
 * @author Abel Hegedus
 *
 */
public interface ActivationState {

    boolean isInactive();
    
    /**
     * An enum of activation states where the state should follow CRUD (Create/Read/Update/Dispose) events.
     */
    public enum DynamicActivationState implements ActivationState {
        
        INACTIVE, APPEARED, FIRED, UPDATED, DISAPPEARED;
        
        @Override
        public boolean isInactive() {
            return (this == INACTIVE);
        }
        
    }
}
