/*******************************************************************************
 * Copyright (c) 2004-2012 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.viatra.query.runtime.rete.index;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;
import org.eclipse.viatra.query.runtime.matchers.tuple.TupleMask;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuples;
import org.eclipse.viatra.query.runtime.rete.network.Direction;
import org.eclipse.viatra.query.runtime.rete.network.Node;
import org.eclipse.viatra.query.runtime.rete.network.ReteContainer;
import org.eclipse.viatra.query.runtime.rete.network.Supplier;
import org.eclipse.viatra.query.runtime.rete.network.communication.ddf.DifferentialTimestamp;

/**
 * Defines an abstract trivial indexer that projects the contents of some stateful node to the empty tuple, and can
 * therefore save space. Can only exist in connection with a stateful store, and must be operated by another node (the
 * active node). Do not attach parents directly!
 * 
 * @author Gabor Bergmann
 * @noimplement Rely on the provided implementations
 * @noreference Use only via standard Node and Indexer interfaces
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public abstract class NullIndexer extends SpecializedProjectionIndexer {

    protected abstract Collection<Tuple> getTuples();

    protected static final Tuple nullSignature = Tuples.staticArityFlatTupleOf();
    protected static final Collection<Tuple> nullSingleton = Collections.singleton(nullSignature);
    protected static final Collection<Tuple> emptySet = Collections.emptySet();

    public NullIndexer(ReteContainer reteContainer, int tupleWidth, Supplier parent, Node activeNode,
            List<ListenerSubscription> sharedSubscriptionList) {
        super(reteContainer, TupleMask.linear(0, tupleWidth), parent, activeNode, sharedSubscriptionList);
    }

    @Override
    public Collection<Tuple> get(Tuple signature) {
        if (nullSignature.equals(signature))
            return isEmpty() ? null : getTuples();
        else
            return null;
    }

    @Override
    public Collection<Tuple> getSignatures() {
        return isEmpty() ? emptySet : nullSingleton;
    }

    protected boolean isEmpty() {
        return getTuples().isEmpty();
    }

    protected boolean isSingleElement() {
        return getTuples().size() == 1;
    }

    @Override
    public Iterator<Tuple> iterator() {
        return getTuples().iterator();
    }

    @Override
    public int getBucketCount() {
        return getTuples().isEmpty() ? 0 : 1;
    }

    @Override
    public void propagateToListener(IndexerListener listener, Direction direction, Tuple updateElement,
            DifferentialTimestamp timestamp) {
        boolean radical = (direction == Direction.REVOKE && isEmpty())
                || (direction == Direction.INSERT && isSingleElement());
        listener.notifyIndexerUpdate(direction, updateElement, nullSignature, radical, timestamp);
    }

}