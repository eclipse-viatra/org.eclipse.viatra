/*******************************************************************************
 * Copyright (c) 2010-2013, Istvan David, Abel Hegedus and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Istvan David - initial API and implementation
 *******************************************************************************/
package org.eclipse.viatra.transformation.evm.specific.resolver;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.viatra.query.runtime.matchers.util.Preconditions;
import org.eclipse.viatra.transformation.evm.api.Activation;
import org.eclipse.viatra.transformation.evm.api.resolver.ChangeableConflictSet;
import org.eclipse.viatra.transformation.evm.api.resolver.ConflictResolver;

/**
 * 
 * @author Istvan David
 *
 */
public class LifoConflictResolver implements ConflictResolver {
    @Override
    public LifoConflictSet createConflictSet() {
        return new LifoConflictSet(this);
    }

    public static final class LifoConflictSet implements ChangeableConflictSet {

        private static final String ACTIVATION_CANNOT_BE_NULL = "Activation cannot be null!";
        private LifoConflictResolver resolver;
        private Deque<Activation<?>> activations = new ArrayDeque<Activation<?>>();

        public LifoConflictSet(LifoConflictResolver resolver) {
            this.resolver = resolver;
        }

        @Override
        public ConflictResolver getConflictResolver() {
            return resolver;
        }

        @Override
        public Activation<?> getNextActivation() {
            return activations.peek();
        }

        @Override
        public Set<Activation<?>> getNextActivations() {
            return activations.isEmpty() ? Collections.emptySet() : Collections.singleton(getNextActivation());
        }

        @Override
        public Set<Activation<?>> getConflictingActivations() {
            return Collections.unmodifiableSet(new LinkedHashSet<>(activations));
        }

        @Override
        public boolean addActivation(Activation<?> activation) {
            Preconditions.checkArgument(activation != null, ACTIVATION_CANNOT_BE_NULL);
            if (activation.equals(activations.peek())) {
                // no change required
                return false;
            } else {
                // activation may already be in the queue, but never more than
                // once (see JavaDoc of method)
                activations.remove(activation);
                activations.push(activation);
                // if the first activation changes, we consider it
                return true;
                // a change in the set
            }
        }

        @Override
        public boolean removeActivation(Activation<?> activation) {
            Preconditions.checkArgument(activation != null, ACTIVATION_CANNOT_BE_NULL);
            return activations.remove(activation);
        }
    }
}