/*******************************************************************************
 * Copyright (c) 2010-2013, Abel Hegedus, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.transformation.evm.specific;

import java.util.Comparator;

import org.eclipse.viatra.transformation.evm.api.Activation;
import org.eclipse.viatra.transformation.evm.specific.resolver.ArbitraryOrderConflictResolver;
import org.eclipse.viatra.transformation.evm.specific.resolver.ComparingConflictResolver;
import org.eclipse.viatra.transformation.evm.specific.resolver.FixedPriorityConflictResolver;

/**
 * @author Abel Hegedus
 *
 */
public final class ConflictResolvers {

    /**
     * 
     */
    private ConflictResolvers() {
    }
    
    public static ArbitraryOrderConflictResolver createArbitraryResolver() {
        return new ArbitraryOrderConflictResolver();
    }
    
    public static FixedPriorityConflictResolver createFixedPriorityResolver() {
        return new FixedPriorityConflictResolver();
    }
    
    public static ComparingConflictResolver createComparingResolver(Comparator<Activation<?>> comparator) {
        return new ComparingConflictResolver(comparator);
    }
    
    // TODO LIFO
    
    // TODO state-based buckets
}
