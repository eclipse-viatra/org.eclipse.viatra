/*******************************************************************************
 * Copyright (c) 2010-2013, Abel Hegedus, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.transformation.evm.api.resolver;

import org.eclipse.viatra.transformation.evm.api.Activation;



/**
 * A conflict set is responsible for managing and ordering the set of enabled activations.
 *
 * @author Abel Hegedus
 *
 */
public interface ChangeableConflictSet extends ConflictSet {

    /**
     * This method is called by the Agenda when an activation changes state and becomes or is still enabled.
     *
     * <p/>NOTE: The ChangeableConflictSet is responsible for handling that <code>add</code> may be called
     *  multiple times on an Activation already in the conflict set!
     *
     * @param activation the activation that should be added to the conflict set
     * @return true, if the conflict set changed
     */
    boolean addActivation(Activation<?> activation);

    /**
     * This method is called by the Agenda when an activation changes state and becomes or is still disabled.
     *
     * <p/>NOTE: The ChangeableConflictSet is responsible for handling that <code>remove</code> may be called
     * on Activations that are not in the conflict set!
     *
     * @param activation the activation that should be removed from the conflict set
     * @return true, if the conflict set changed
     */
    boolean removeActivation(Activation<?> activation);

}
