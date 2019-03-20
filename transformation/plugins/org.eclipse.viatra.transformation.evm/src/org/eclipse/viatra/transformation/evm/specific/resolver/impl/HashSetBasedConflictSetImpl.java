/*******************************************************************************
 * Copyright (c) 2010-2013 Gabor Bergmann, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.transformation.evm.specific.resolver.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.viatra.transformation.evm.api.Activation;
import org.eclipse.viatra.transformation.evm.api.resolver.ChangeableConflictSet;

public abstract class HashSetBasedConflictSetImpl implements ChangeableConflictSet {
    
    protected Set<Activation<?>> container = new HashSet<Activation<?>>();

    @Override
    public Set<Activation<?>> getConflictingActivations() {
        return Collections.unmodifiableSet(new HashSet<>(container));
    }

    @Override
    public boolean addActivation(Activation<?> activation) {
        return container.add(activation);
    }

    @Override
    public boolean removeActivation(Activation<?> activation) {
        return container.remove(activation);
    }

    @Override
    public Set<Activation<?>> getNextActivations() {
        return Collections.unmodifiableSet(new HashSet<>(container));
    }

}
