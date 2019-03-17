/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Tamas Szabo - initial API and implementation
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.rete.network.delayed;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;
import org.eclipse.viatra.query.runtime.rete.network.Direction;
import org.eclipse.viatra.query.runtime.rete.network.Receiver;
import org.eclipse.viatra.query.runtime.rete.network.ReteContainer;
import org.eclipse.viatra.query.runtime.rete.network.Supplier;
import org.eclipse.viatra.query.runtime.rete.network.communication.CommunicationTracker;
import org.eclipse.viatra.query.runtime.rete.network.communication.ddf.DifferentialTimestamp;
import org.eclipse.viatra.query.runtime.rete.network.mailbox.Mailbox;

/**
 * Instances of this class are responsible for initializing a {@link Receiver} with the 
 * contents of a {@link Supplier}. However, due to the dynamic nature of the Rete {@link Network} and to 
 * the fact that certain {@link Node}s in the {@link Network} are sensitive to the shape of the {@link Network}, 
 * the commands must be delayed until the construction of the {@link Network} has stabilized.  
 * 
 * @author Tamas Szabo
 * @since 2.2
 */
public abstract class DelayedCommand implements Runnable {

    protected final Supplier supplier;
    protected final Receiver receiver;
    protected final Direction direction;
    protected final ReteContainer container;

    public DelayedCommand(final Supplier supplier, final Receiver receiver, final Direction direction,
            final ReteContainer container) {
        this.supplier = supplier;
        this.receiver = receiver;
        this.direction = direction;
        this.container = container;
    }

    @Override
    public void run() {
        final CommunicationTracker tracker = this.container.getCommunicationTracker();
        final Mailbox mailbox = tracker.proxifyMailbox(this.supplier, this.receiver.getMailbox());

        if (this.isTimestampAware()) {
            final Map<Tuple, DifferentialTimestamp> contents = this.container.pullContentsWithTimestamp(this.supplier, false);
            for (final Entry<Tuple, DifferentialTimestamp> entry : contents.entrySet()) {
                mailbox.postMessage(this.direction, entry.getKey(), entry.getValue());
            }
        } else {
            final Collection<Tuple> contents = this.container.pullContents(this.supplier, false);
            for (final Tuple tuple : contents) {
                mailbox.postMessage(this.direction, tuple, DifferentialTimestamp.ZERO);
            }
        }
    }

    @Override
    public String toString() {
        return this.supplier + " -> " + this.receiver.toString();
    }

    protected abstract boolean isTimestampAware();

}
