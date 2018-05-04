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

package org.eclipse.viatra.query.runtime.rete.network;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;
import org.eclipse.viatra.query.runtime.matchers.tuple.TupleMask;
import org.eclipse.viatra.query.runtime.matchers.util.CollectionsFactory;
import org.eclipse.viatra.query.runtime.rete.index.GenericProjectionIndexer;
import org.eclipse.viatra.query.runtime.rete.index.ProjectionIndexer;
import org.eclipse.viatra.query.runtime.rete.network.mailbox.Mailbox;
import org.eclipse.viatra.query.runtime.rete.traceability.TraceInfo;

/**
 * Base implementation for a supplier node.
 * 
 * @author Gabor Bergmann
 * 
 */
public abstract class StandardNode extends BaseNode implements Supplier {
    protected List<Receiver> children = CollectionsFactory.createObserverList();
    private List<Mailbox> childMailboxes = CollectionsFactory.createObserverList();

    public StandardNode(ReteContainer reteContainer) {
        super(reteContainer);
    }
    
    protected void propagateUpdate(Direction direction, Tuple updateElement) {
        for (Mailbox childMailbox : childMailboxes)
            childMailbox.postMessage(direction, updateElement);            
    }

    @Override
    public void appendChild(Receiver receiver) {
        children.add(receiver);
        childMailboxes.add(receiver.getMailbox());
    }

    @Override
    public void removeChild(Receiver receiver) {
        children.remove(receiver);
        childMailboxes.remove(receiver.getMailbox());
    }

    @Override
    public Collection<Receiver> getReceivers() {
        return children;
    }
    
    @Override
    public Set<Tuple> getPulledContents() {
        HashSet<Tuple> results = new HashSet<Tuple>();
        pullInto(results);
        return results;
    }

    @Override
    public ProjectionIndexer constructIndex(TupleMask mask, TraceInfo... traces) {
        final GenericProjectionIndexer indexer = new GenericProjectionIndexer(reteContainer, mask);
        for (TraceInfo traceInfo : traces) indexer.assignTraceInfo(traceInfo);
        reteContainer.connectAndSynchronize(this, indexer);
        return indexer;
    }
    
    /**
     * @since 1.6
     */
    protected void issueError(String message, Exception ex) {
        if (ex == null) {
            this.reteContainer.getNetwork().getEngine().getLogger().error(message);
        } else {
            this.reteContainer.getNetwork().getEngine().getLogger().error(message, ex);
        }
    }

}
