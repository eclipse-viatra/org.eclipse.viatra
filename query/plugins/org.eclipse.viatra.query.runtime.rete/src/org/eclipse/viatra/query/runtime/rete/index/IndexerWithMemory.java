/*******************************************************************************
 * Copyright (c) 2004-2009 Gabor Bergmann and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Gabor Bergmann - initial API and implementation
 *******************************************************************************/

package org.eclipse.viatra.query.runtime.rete.index;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.viatra.query.runtime.matchers.memories.MaskedTupleMemory;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;
import org.eclipse.viatra.query.runtime.matchers.tuple.TupleMask;
import org.eclipse.viatra.query.runtime.matchers.util.CollectionsFactory.BucketType;
import org.eclipse.viatra.query.runtime.rete.network.Direction;
import org.eclipse.viatra.query.runtime.rete.network.Receiver;
import org.eclipse.viatra.query.runtime.rete.network.ReteContainer;
import org.eclipse.viatra.query.runtime.rete.network.Supplier;
import org.eclipse.viatra.query.runtime.rete.network.mailbox.AdaptiveMailbox;
import org.eclipse.viatra.query.runtime.rete.network.mailbox.Mailbox;

/**
 * @author Gabor Bergmann
 * 
 */
public abstract class IndexerWithMemory extends StandardIndexer implements Receiver {

    protected MaskedTupleMemory memory;
    /**
     * @since 1.6
     */
    protected final Mailbox mailbox;

    /**
     * @param reteContainer
     * @param mask
     */
    public IndexerWithMemory(ReteContainer reteContainer, TupleMask mask) {
        super(reteContainer, mask);
        memory = MaskedTupleMemory.create(mask, BucketType.SETS, this);
        reteContainer.registerClearable(memory);
        mailbox = instantiateMailbox();
        reteContainer.registerClearable(mailbox);
    }

    /**
     * Instantiates the {@link Mailbox} of this receiver. Subclasses may override this method to provide their own
     * mailbox implementation.
     * 
     * @return the mailbox
     * @since 2.0
     */
    protected Mailbox instantiateMailbox() {
        return new AdaptiveMailbox(this, this.reteContainer);
    }

    @Override
    public Mailbox getMailbox() {
        return mailbox;
    }

    /**
     * @since 2.0
     */
    public MaskedTupleMemory getMemory() {
        return memory;
    }

    @Override
    public void update(Direction direction, Tuple updateElement) {
        Tuple signature = mask.transform(updateElement);
        boolean change = (direction == Direction.INSERT) ? memory.add(updateElement, signature)
                : memory.remove(updateElement, signature);
        update(direction, updateElement, signature, change);
    }

    /**
     * Refined version of update
     */
    protected abstract void update(Direction direction, Tuple updateElement, Tuple signature, boolean change);

    @Override
    public void appendParent(Supplier supplier) {
        if (parent == null)
            parent = supplier;
        else
            throw new UnsupportedOperationException("Illegal RETE edge: " + this + " already has a parent (" + parent
                    + ") and cannot connect to additional parent (" + supplier + "). ");
    }

    @Override
    public void removeParent(Supplier supplier) {
        if (parent == supplier)
            parent = null;
        else
            throw new IllegalArgumentException(
                    "Illegal RETE edge removal: the parent of " + this + " is not " + supplier);
    }

    @Override
    public Collection<Supplier> getParents() {
        return Collections.singleton(parent);
    }

}