/*******************************************************************************
 * Copyright (c) 2010-2013, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.rete.eval;

import org.eclipse.viatra.query.runtime.rete.network.ReteContainer;
import org.eclipse.viatra.query.runtime.rete.single.SingleInputNode;

/**
 * @author Bergmann Gabor
 */
public abstract class AbstractEvaluatorNode extends SingleInputNode implements IEvaluatorNode {
    
    /**
     * @since 1.5
     */
    protected EvaluatorCore core;


    /**
     * @since 1.5
     */
    public AbstractEvaluatorNode(ReteContainer reteContainer, EvaluatorCore core) {
        super(reteContainer);
        this.core = core;
        core.init(this);
    }
    
    /**
     * @since 1.5
     */
    @Override
    public ReteContainer getReteContainer() {
        return getContainer();
    }
    
    /**
     * @since 1.5
     */
    @Override
    public String prettyPrintTraceInfoPatternList() {
        return getTraceInfoPatternsEnumerated();
    }
    
}
