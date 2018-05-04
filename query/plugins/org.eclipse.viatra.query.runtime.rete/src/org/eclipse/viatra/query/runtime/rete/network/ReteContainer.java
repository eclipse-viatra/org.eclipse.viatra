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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.log4j.Logger;
import org.eclipse.viatra.query.runtime.matchers.context.IQueryBackendContext;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;
import org.eclipse.viatra.query.runtime.matchers.util.Clearable;
import org.eclipse.viatra.query.runtime.matchers.util.CollectionsFactory;
import org.eclipse.viatra.query.runtime.rete.boundary.InputConnector;
import org.eclipse.viatra.query.runtime.rete.network.mailbox.Mailbox;
import org.eclipse.viatra.query.runtime.rete.remote.Address;
import org.eclipse.viatra.query.runtime.rete.single.SingleInputNode;
import org.eclipse.viatra.query.runtime.rete.util.Options;

/**
 * @author Gabor Bergmann
 *
 *         Mutexes: externalMessageLock - enlisting messages into and retrieving from the external message queue
 */
public final class ReteContainer {

    protected Thread consumerThread = null;
    protected boolean killed = false;

    protected Network network;

    protected LinkedList<Clearable> clearables;
    protected Map<Long, Node> nodesById;
    protected long nextId = 0;

    protected ConnectionFactory connectionFactory;
    protected NodeProvisioner nodeProvisioner;

    protected Deque<UpdateMessage> internalMessageQueue = new ArrayDeque<UpdateMessage>();
    protected/* volatile */Deque<UpdateMessage> externalMessageQueue = new ArrayDeque<UpdateMessage>();
    protected Object externalMessageLock = new Object();
    protected Long clock = 1L; // even: steady state, odd: active queue; access
                               // ONLY with messageQueue locked!
    protected Map<ReteContainer, Long> terminationCriteria = null;
    protected final Logger logger;
    protected final CommunicationTracker tracker;

    protected final IQueryBackendContext backendContext;

    /**
     * @param threaded
     *            false if operating in a single-threaded environment
     */
    public ReteContainer(Network network, boolean threaded) {
        super();
        this.network = network;
        this.backendContext = network.getEngine().getBackendContext();
        this.tracker = new CommunicationTracker();
        this.nodesById = CollectionsFactory.createMap();
        this.clearables = new LinkedList<Clearable>();
        this.logger = network.getEngine().getLogger();

        this.connectionFactory = new ConnectionFactory(this);
        this.nodeProvisioner = new NodeProvisioner(this);

        if (threaded) {
            this.terminationCriteria = CollectionsFactory.createMap();
            this.consumerThread = new Thread("Rete thread of " + ReteContainer.super.toString()) {
                @Override
                public void run() {
                    messageConsumptionCycle();
                }
            };
            this.consumerThread.start();
        }
    }

    /**
     * @since 1.6
     * @return the communication graph of the nodes, incl. messgae scheduling
     */
    public CommunicationTracker getTracker() {
        return tracker;
    }

    /**
     * Stops this container. To be called by Network.kill()
     */
    public void kill() {
        killed = true;
        if (consumerThread != null)
            consumerThread.interrupt();
    }

    /**
     * Establishes connection between a supplier and a receiver node, regardless which container they are in. Assumption
     * is that this container is the home of the receiver, but it is not strictly necessary.
     *
     * @param synchronise
     *            indicates whether the receiver should be synchronised to the current contents of the supplier
     */
    public void connectRemoteNodes(Address<? extends Supplier> supplier, Address<? extends Receiver> receiver,
            boolean synchronise) {
        if (!isLocal(receiver))
            receiver.getContainer().connectRemoteNodes(supplier, receiver, synchronise);
        else {
            Receiver child = resolveLocal(receiver);
            connectRemoteSupplier(supplier, child, synchronise);
        }
    }

    /**
     * Severs connection between a supplier and a receiver node, regardless which container they are in. Assumption is
     * that this container is the home of the receiver, but it is not strictly necessary.
     *
     * @param desynchronise
     *            indicates whether the current contents of the supplier should be subtracted from the receiver
     */
    public void disconnectRemoteNodes(Address<? extends Supplier> supplier, Address<? extends Receiver> receiver,
            boolean desynchronise) {
        if (!isLocal(receiver))
            receiver.getContainer().disconnectRemoteNodes(supplier, receiver, desynchronise);
        else {
            Receiver child = resolveLocal(receiver);
            disconnectRemoteSupplier(supplier, child, desynchronise);
        }
    }

    /**
     * Establishes connection between a remote supplier and a local receiver node.
     *
     * @param synchronise
     *            indicates whether the receiver should be synchronised to the current contents of the supplier
     */
    public void connectRemoteSupplier(Address<? extends Supplier> supplier, Receiver receiver, boolean synchronise) {
        Supplier parent = nodeProvisioner.asSupplier(supplier);
        if (synchronise)
            connectAndSynchronize(parent, receiver);
        else
            connect(parent, receiver);
    }

    /**
     * Severs connection between a remote supplier and a local receiver node.
     *
     * @param desynchronise
     *            indicates whether the current contents of the supplier should be subtracted from the receiver
     */
    public void disconnectRemoteSupplier(Address<? extends Supplier> supplier, Receiver receiver,
            boolean desynchronise) {
        Supplier parent = nodeProvisioner.asSupplier(supplier);
        if (desynchronise)
            disconnectAndDesynchronize(parent, receiver);
        else
            disconnect(parent, receiver);
    }

    /**
     * Connects a receiver to a supplier
     */
    public void connect(Supplier supplier, Receiver receiver) {
        supplier.appendChild(receiver);
        receiver.appendParent(supplier);
        tracker.registerDependency(supplier, receiver);
    }

    /**
     * Disconnects a receiver from a supplier
     */
    public void disconnect(Supplier supplier, Receiver receiver) {
        supplier.removeChild(receiver);
        receiver.removeParent(supplier);
        tracker.unregisterDependency(supplier, receiver);
    }

    /**
     * Connects a receiver to a remote supplier, and synchronises it to the current contents of the supplier
     */
    public void connectAndSynchronize(Supplier supplier, Receiver receiver) {
        flushUpdates(); // first, all pending updates should be flushed into the
                        // supplier BEFORE connecting
        supplier.appendChild(receiver);
        receiver.appendParent(supplier);
        tracker.registerDependency(supplier, receiver);

        Collection<Tuple> pulled = pullContents(supplier);
        sendConstructionUpdates(receiver, Direction.INSERT, pulled);

        flushUpdates(); // deliver the positive updates
    }

    /**
     * Disconnects a receiver from a supplier
     */
    public void disconnectAndDesynchronize(Supplier supplier, Receiver receiver) {
        flushUpdates(); // first, all pending updates should be flushed into the
                        // supplier BEFORE disconnecting

        supplier.removeChild(receiver);
        receiver.removeParent(supplier);
        tracker.unregisterDependency(supplier, receiver);

        Collection<Tuple> pulled = pullContents(supplier);
        sendConstructionUpdates(receiver, Direction.REVOKE, pulled);

        flushUpdates(); // deliver the negative updates
    }

    /**
     * Sends an update message to the receiver node, indicating a newly found or lost partial matching. They are
     * inserted into the queue atomically. Delivery is either synchronous or asynchronous. Designed to be called during
     * network construction.
     * 
     * NOTE: unused in 1.7
     */
    public void sendConstructionUpdate(Receiver receiver, Direction direction, Tuple updateElement) {
        if (consumerThread == null) {
            sendUpdateInternal(receiver, direction, updateElement);
        } else {
            network.sendConstructionUpdate(makeAddress(receiver), direction, updateElement);
        }
    }

    /**
     * Sends several update messages atomically to the receiver node, indicating a newly found or lost partial matching.
     * They are inserted into the queue atomically. Delivery is either synchronous or asynchronous. Designed to be
     * called during network construction.
     */
    public void sendConstructionUpdates(Receiver receiver, Direction direction, Collection<Tuple> updateElements) {
        if (consumerThread == null) {
          Mailbox mailbox = receiver.getMailbox();
          for (Tuple updateElement : updateElements) {
              mailbox.postMessage(direction, updateElement);              
          }
        } else {
            network.sendConstructionUpdates(makeAddress(receiver), direction, updateElements);
        }
    }

    /**
     * Sends an update message to the receiver node by placing the message into its mailbox. NOT to be called from user
     * threads.
     * 
     * NOTE: unused in 1.7
     */
    public void sendUpdateInternal(Receiver receiver, Direction direction, Tuple updateElement) {
        Mailbox mailbox = receiver.getMailbox();
        mailbox.postMessage(direction, updateElement);
    }

    /**
     * Sends an update message to the receiver node, indicating a newly found or lost partial matching. The receiver is
     * indicated by the Address. Designed to be called by the Network, DO NOT use in any other way. @pre:
     * address.container == this, e.g. address MUST be local
     *
     * @return the value of the container's clock at the time when the message was accepted into the local message queue
     */
    long sendUpdateToLocalAddress(Address<? extends Receiver> address, Direction direction, Tuple updateElement) {
        long timestamp;
        Receiver receiver = resolveLocal(address);
        UpdateMessage message = new UpdateMessage(receiver, direction, updateElement);
        synchronized (externalMessageLock) {
            externalMessageQueue.add(message);
            // messageQueue.add(new UpdateMessage(resolveLocal(address),
            // direction, updateElement));
            // this.sendUpdateInternal(resolveLocal(address), direction,
            // updateElement);
            timestamp = clock;
            externalMessageLock.notifyAll();
        }

        return timestamp;

    }

    /**
     * Sends multiple update messages atomically to the receiver node, indicating a newly found or lost partial
     * matching. The receiver is indicated by the Address. Designed to be called by the Network, DO NOT use in any other
     * way. @pre: address.container == this, e.g. address MUST be local @pre: updateElements is nonempty!
     *
     * @return the value of the container's clock at the time when the message was accepted into the local message queue
     */
    long sendUpdatesToLocalAddress(Address<? extends Receiver> address, Direction direction,
            Collection<Tuple> updateElements) {

        long timestamp;
        Receiver receiver = resolveLocal(address);
        // UpdateMessage message = new UpdateMessage(receiver, direction,
        // updateElement);
        synchronized (externalMessageLock) {
            for (Tuple ps : updateElements)
                externalMessageQueue.add(new UpdateMessage(receiver, direction, ps));
            // messageQueue.add(new UpdateMessage(resolveLocal(address),
            // direction, updateElement));
            // this.sendUpdateInternal(resolveLocal(address), direction,
            // updateElement);
            timestamp = clock;
            externalMessageLock.notifyAll();
        }

        return timestamp;
    }

    /**
     * Sends an update message to the receiver node, indicating a newly found or lost partial matching. The receiver is
     * indicated by the Address. Designed to be called by the Network in single-threaded operation, DO NOT use in any
     * other way.
     */
    void sendUpdateToLocalAddressSingleThreaded(Address<? extends Receiver> address, Direction direction,
            Tuple updateElement) {
        Receiver receiver = resolveLocal(address);
        UpdateMessage message = new UpdateMessage(receiver, direction, updateElement);
        internalMessageQueue.add(message);
    }

    /**
     * Sends multiple update messages to the receiver node, indicating a newly found or lost partial matching. The
     * receiver is indicated by the Address. Designed to be called by the Network in single-threaded operation, DO NOT
     * use in any other way.
     *
     * @pre: address.container == this, e.g. address MUST be local
     */
    void sendUpdatesToLocalAddressSingleThreaded(Address<? extends Receiver> address, Direction direction,
            Collection<Tuple> updateElements) {
        Receiver receiver = resolveLocal(address);
        for (Tuple ps : updateElements)
            internalMessageQueue.add(new UpdateMessage(receiver, direction, ps));
    }

    /**
     * Sends an update message to a node in a different container. The receiver is indicated by the Address. Designed to
     * be called by RemoteReceivers, DO NOT use in any other way.
     *
     * @return the value of the container's clock at the time when the message was accepted into the local message queue
     */
    public void sendUpdateToRemoteAddress(Address<? extends Receiver> address, Direction direction,
            Tuple updateElement) {
        ReteContainer otherContainer = address.getContainer();
        long otherClock = otherContainer.sendUpdateToLocalAddress(address, direction, updateElement);
        // Long criterion = terminationCriteria.get(otherContainer);
        // if (criterion==null || otherClock > criterion)
        terminationCriteria.put(otherContainer, otherClock);
    }

    /**
     * Finalises all update sequences and returns. To be called from user threads (e.g. network construction).
     */
    public void flushUpdates() {
        network.waitForReteTermination();
        // synchronized (messageQueue)
        // {
        // while (!messageQueue.isEmpty())
        // {
        // try {
        // UpdateMessage message = messageQueue.take();
        // message.receiver.update(message.direction, message.updateElement);
        // } catch (InterruptedException e) {}
        // }
        // }
    }

    /**
     * Retrieves a safe copy of the contents of a supplier.
     */
    public Collection<Tuple> pullContents(Supplier supplier) {
        flushUpdates();
        Collection<Tuple> collector = new LinkedList<Tuple>();
        supplier.pullInto(collector);
        return collector;
    }

    /**
     * Retrieves the contents of a SingleInputNode's parentage.
     */
    public Collection<Tuple> pullPropagatedContents(SingleInputNode supplier) {
        flushUpdates();
        Collection<Tuple> collector = new LinkedList<Tuple>();
        supplier.propagatePullInto(collector);
        return collector;
    }

    /**
     * Retrieves the contents of a supplier for a remote caller. Assumption is that this container is the home of the
     * supplier, but it is not strictly necessary.
     *
     * @param supplier
     *            the address of the supplier to be pulled.
     */
    public Collection<Tuple> remotePull(Address<? extends Supplier> supplier) {
        if (!isLocal(supplier))
            return supplier.getContainer().remotePull(supplier);
        return pullContents(resolveLocal(supplier));
    }

    /**
     * Proxies for the getPosMapping() of Production nodes. Retrieves the posmapping of a remote or local Production to
     * a remote or local caller.
     */
    public Map<String, Integer> remotePosMapping(Address<? extends Production> production) {
        if (!isLocal(production))
            return production.getContainer().remotePosMapping(production);
        return resolveLocal(production).getPosMapping();
    }

    /**
     * Continually consumes update messages. Should be run on a dedicated thread.
     */
    void messageConsumptionCycle() {
        while (!killed) // deliver messages on and on and on....
        {
            long incrementedClock = 0;
            UpdateMessage message = null;

            if (!internalMessageQueue.isEmpty()) // take internal messages first
                message = internalMessageQueue.removeFirst();
            else
                // no internal message, take an incoming message
                synchronized (externalMessageLock) { // no sleeping allowed,
                                                     // because external
                                                     // queue is locked for
                                                     // precise clocking of
                                                     // termination point!
                if (!externalMessageQueue.isEmpty()) { // if external queue
                                                       // is non-empty,
                                                       // retrieve the next
                                                       // message instantly
                    message = takeExternalMessage();
                } else { // if external queue is found empty (and this is
                         // the first time in a row)
                    incrementedClock = ++clock; // local termination point
                    // synchronized(clock){incrementedClock = ++clock;}
                }
                }

            if (message == null) // both queues were empty
            {
                localUpdateTermination(incrementedClock); // report local
                                                          // termination point
                while (message == null) // wait for a message while external
                                        // queue is still empty
                {
                    synchronized (externalMessageLock) {
                        while (externalMessageQueue.isEmpty()) {
                            try {
                                externalMessageLock.wait();
                            } catch (InterruptedException e) {
                                if (killed)
                                    return;
                            }
                        }
                        message = takeExternalMessage();
                    }

                }
            }

            // now we have a message to deliver
            message.receiver.update(message.direction, message.updateElement);
        }
    }

    /**
     * @since 1.6
     */
    public static final Function<Node, String> NAME_MAPPER = input -> input.toString().substring(0, Math.min(30, input.toString().length()));

    
    /**
     * Sends out all pending messages to their receivers. The delivery is governed by the communication tracker.
     * @since 1.6
     */
    public void deliverMessagesSingleThreaded() {
        if (!backendContext.areUpdatesDelayed()) {
            if (Options.MONITOR_VIOLATION_OF_RETE_NODEGROUP_TOPOLOGICAL_SORTING) { 
                // known unreachable; enable for debugging only
                
                CommunicationGroup lastGroup = null;
                Set<CommunicationGroup> seenInThisCycle = new HashSet<>();
                
                while (!tracker.isEmpty()) {
                    final CommunicationGroup group = tracker.getAndRemoveFirstGroup();
                    
                    /**
                     * The current group does not violate the communication schema iff
                     * (1) it was not seen before
                     * OR
                     * (2) the last one that was seen is exactly the same as the current one 
                     *     this can happen if the group was added back because of in-group message passing
                     */
                    boolean okGroup = (group == lastGroup) || seenInThisCycle.add(group);
                    
                    if (!okGroup) {
                        logger.error(
                                "[INTERNAL ERROR] Violation of communication schema! The communication component with representative "
                                        + group.getRepresentative() + " has already been processed!");
                    }
                    
                    group.deliverMessages();
                    
                    lastGroup = group;
                }
                
            } else {
                while (!tracker.isEmpty()) {
                    final CommunicationGroup group = tracker.getAndRemoveFirstGroup();
                    group.deliverMessages();
                }
            }
            
            
        }
    }

    private void localUpdateTermination(long incrementedClock) {
        network.reportLocalUpdateTermination(this, incrementedClock, terminationCriteria);
        terminationCriteria.clear();

        // synchronized(clock){++clock;} // +1 incrementing for parity and easy
        // comparison
    }

    // @pre: externalMessageQueue synchronized && nonempty
    private UpdateMessage takeExternalMessage() {
        UpdateMessage message = externalMessageQueue.removeFirst();
        if (!externalMessageQueue.isEmpty()) { // copy the whole queue over
                                               // for speedup
            Deque<UpdateMessage> temp = externalMessageQueue;
            externalMessageQueue = internalMessageQueue;
            internalMessageQueue = temp;
        }
        return message;
    }

    /**
     * Provides an external address for the selected node.
     *
     * @pre node belongs to this container.
     */
    public <N extends Node> Address<N> makeAddress(N node) {
        return new Address<N>(node);
    }

    /**
     * Checks whether a certain address points to a node at this container.
     */
    public boolean isLocal(Address<? extends Node> address) {
        return address.getContainer() == this;
    }

    /**
     * Returns an addressed node at this container.
     * 
     * @pre: address.container == this, e.g. address MUST be local
     * @throws IllegalArgumentException
     *             if address is non-local
     */
    @SuppressWarnings("unchecked")
    public <N extends Node> N resolveLocal(Address<N> address) {
        if (this != address.getContainer())
            throw new IllegalArgumentException(String.format("Address %s non-local at container %s", address, this));

        N cached = address.getNodeCache();
        if (cached != null)
            return cached;
        else {
            N node = (N) nodesById.get(address.getNodeId());
            address.setNodeCache(node);
            return node;
        }
    }

    /**
     * Registers a node into the rete network (should be called by constructor). Every node MUST be registered by its
     * constructor.
     *
     * @return the unique nodeId issued to the node.
     */
    public long registerNode(Node n) {
        long id = nextId++;
        nodesById.put(id, n);
        return id;
    }

    /**
     * Unregisters a node from the rete network. Do NOT call if node is still connected to other Nodes, or Adressed or
     * otherwise referenced.
     */
    public void unregisterNode(Node n) {
        nodesById.remove(n.getNodeId());
    }

    /**
     * Registers a pattern memory into the rete network. Every memory MUST be registered by its owner node.
     */
    public void registerClearable(Clearable c) {
        clearables.addFirst(c);
    }

    /**
     * Unregisters a pattern memory from the rete network.
     */
    public void unregisterClearable(Clearable c) {
        clearables.remove(c);
    }

    /**
     * Clears all memory contents in the network. Reverts to initial state.
     */
    public void clearAll() {
        for (Clearable c : clearables) {
            c.clear();
        }
    }

    public NodeFactory getNodeFactory() {
        return network.getNodeFactory();
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public NodeProvisioner getProvisioner() {
        return nodeProvisioner;
    }

    public Network getNetwork() {
        return network;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String separator = System.getProperty("line.separator");
        sb.append(super.toString() + "[[[" + separator);
        java.util.List<Long> keys = new java.util.ArrayList<Long>(nodesById.keySet());
        java.util.Collections.sort(keys);
        for (Long key : keys) {
            sb.append(key + " -> " + nodesById.get(key) + separator);
        }
        sb.append("]]] of " + network);
        return sb.toString();
    }

    /**
     * Access all the Rete nodes inside this container.
     *
     * @return the collection of {@link Node} instances
     */
    public Collection<Node> getAllNodes() {
        return nodesById.values();
    }

    public InputConnector getInputConnectionFactory() {
        return network.getInputConnector();
    }

}
