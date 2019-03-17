/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Gabor Bergmann - initial API and implementation
 *******************************************************************************/

package org.eclipse.viatra.query.runtime.rete.misc;

import java.util.Collection;
import java.util.Map;

import org.eclipse.viatra.query.runtime.matchers.context.IQueryRuntimeContext;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;
import org.eclipse.viatra.query.runtime.rete.network.ReteContainer;
import org.eclipse.viatra.query.runtime.rete.network.StandardNode;
import org.eclipse.viatra.query.runtime.rete.network.communication.ddf.DifferentialTimestamp;

/**
 * Node that always contains a single constant Tuple
 * 
 * @author Gabor Bergmann
 */
public class ConstantNode extends StandardNode {

    protected Tuple constant;

    /**
     * @param constant will be wrapped using {@link IQueryRuntimeContext#wrapTuple(Tuple)}
     */
    public ConstantNode(ReteContainer reteContainer, Tuple constant) {
        super(reteContainer);
        this.constant = reteContainer.getNetwork().getEngine().getRuntimeContext().wrapTuple(constant);
    }

    @Override
    public void pullInto(Collection<Tuple> collector, boolean flush) {
        collector.add(constant);
    }
    
    @Override
    public void pullIntoWithTimestamp(final Map<Tuple, DifferentialTimestamp> collector, final boolean flush) {
        collector.put(constant, DifferentialTimestamp.ZERO);
    }

}
