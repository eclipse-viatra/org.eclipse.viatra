/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.viatra.query.runtime.rete.index;

import java.util.Collection;

import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;
import org.eclipse.viatra.query.runtime.matchers.tuple.TupleMask;
import org.eclipse.viatra.query.runtime.rete.network.ReteContainer;
import org.eclipse.viatra.query.runtime.rete.network.Supplier;

/**
 * @author Gabor Bergmann Indexer whose lifetime last until the first get() DO NOT connect to nodes!
 */
public class OnetimeIndexer extends GenericProjectionIndexer {

    public OnetimeIndexer(ReteContainer reteContainer, TupleMask mask) {
        super(reteContainer, mask);
    }

    @Override
    public Collection<Tuple> get(Tuple signature) {
        if (org.eclipse.viatra.query.runtime.rete.util.Options.releaseOnetimeIndexers) {
            reteContainer.unregisterClearable(memory);
            reteContainer.unregisterNode(this);
        }
        return super.get(signature);
    }

    @Override
    public void appendParent(Supplier supplier) {
        throw new UnsupportedOperationException("onetime indexer cannot have parents");
    }

    @Override
    public void attachListener(IndexerListener listener) {
        throw new UnsupportedOperationException("onetime indexer cannot have listeners");
    }

}
