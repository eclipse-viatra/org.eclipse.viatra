/*******************************************************************************
 * Copyright (c) 2010-2016, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.rete.network.mailbox;

import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;
import org.eclipse.viatra.query.runtime.matchers.util.Clearable;
import org.eclipse.viatra.query.runtime.rete.network.Direction;
import org.eclipse.viatra.query.runtime.rete.network.IGroupable;
import org.eclipse.viatra.query.runtime.rete.network.Receiver;
import org.eclipse.viatra.query.runtime.rete.network.communication.CommunicationGroup;
import org.eclipse.viatra.query.runtime.rete.network.communication.MessageSelector;
import org.eclipse.viatra.query.runtime.rete.network.communication.ddf.DifferentialTimestamp;

/**
 * A mailbox is associated with every {@link Receiver}. Messages can be sent to a {@link Receiver} by posting them into
 * the mailbox. Different mailbox implementations may differ in the way how they deliver the posted messages.
 * 
 * @author Tamas Szabo
 * @since 2.0
 *
 */
public interface Mailbox extends Clearable, IGroupable {

    /**
     * Posts a new message to this mailbox.
     * 
     * @param direction
     *            the direction of the update
     * @param update
     *            the update element
     * @return the effect of the message posting
     */
    public void postMessage(final Direction direction, final Tuple update, final DifferentialTimestamp timestamp);

    /**
     * Delivers all messages according to the given selector from this mailbox. The selector can also be null. In this case, no
     * special separation is expected between the messages.
     * 
     * @param selector the message selector
     */
    public void deliverAll(final MessageSelector selector);

    /**
     * Returns the {@link Receiver} of this mailbox.
     * 
     * @return the receiver
     */
    public Receiver getReceiver();

    /**
     * Returns the {@link CommunicationGroup} of the receiver of this mailbox.
     * 
     * @return the communication group
     */
    public CommunicationGroup getCurrentGroup();

    /**
     * Sets the {@link CommunicationGroup} that the receiver of this mailbox is associated with.
     * 
     * @param group
     *            the communication group
     */
    public void setCurrentGroup(final CommunicationGroup group);

    /**
     * Returns true if this mailbox is empty.
     * 
     * @return
     */
    public boolean isEmpty();

}
