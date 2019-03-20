/*******************************************************************************
 * Copyright (c) 2010-2017, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.rete.network.communication;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.eclipse.viatra.query.runtime.base.itc.alg.incscc.IncSCCAlg;
import org.eclipse.viatra.query.runtime.base.itc.alg.misc.topsort.TopologicalSorting;
import org.eclipse.viatra.query.runtime.base.itc.graphimpl.Graph;
import org.eclipse.viatra.query.runtime.matchers.tuple.TupleMask;
import org.eclipse.viatra.query.runtime.rete.aggregation.IAggregatorNode;
import org.eclipse.viatra.query.runtime.rete.boundary.ExternalInputEnumeratorNode;
import org.eclipse.viatra.query.runtime.rete.index.DualInputNode;
import org.eclipse.viatra.query.runtime.rete.index.ExistenceNode;
import org.eclipse.viatra.query.runtime.rete.index.Indexer;
import org.eclipse.viatra.query.runtime.rete.index.IndexerListener;
import org.eclipse.viatra.query.runtime.rete.index.IterableIndexer;
import org.eclipse.viatra.query.runtime.rete.network.IGroupable;
import org.eclipse.viatra.query.runtime.rete.network.Node;
import org.eclipse.viatra.query.runtime.rete.network.ProductionNode;
import org.eclipse.viatra.query.runtime.rete.network.Receiver;
import org.eclipse.viatra.query.runtime.rete.network.RederivableNode;
import org.eclipse.viatra.query.runtime.rete.network.ReteContainer;
import org.eclipse.viatra.query.runtime.rete.network.mailbox.FallThroughMailbox;
import org.eclipse.viatra.query.runtime.rete.network.mailbox.Mailbox;
import org.eclipse.viatra.query.runtime.rete.single.TransitiveClosureNode;
import org.eclipse.viatra.query.runtime.rete.single.TrimmerNode;

/**
 * An instance of this class is associated with every {@link ReteContainer}. The tracker serves two purposes: <br>
 * (1) It allows RETE nodes to register their communication dependencies on-the-fly. These dependencies can be
 * registered or unregistered when nodes are disposed of. <br>
 * (2) It allows RETE nodes to register their mailboxes as dirty, that is, they can tell the tracker that they have
 * something to send to other nodes in the network. The tracker is then responsible for ordering these messages (more
 * precisely, the mailboxes that contain the messages) for the associated {@link ReteContainer}. The ordering is
 * governed by the strongly connected components in the dependency network and follows a topological sorting scheme;
 * those mailboxes will be emptied first whose owner nodes do not depend on other undelivered messages.
 * 
 * @author Tamas Szabo
 * @since 1.6
 *
 */
public abstract class CommunicationTracker {

    /**
     * The minimum group id assigned so far
     */
    protected int minGroupId;

    /**
     * The maximum group id assigned so far
     */
    protected int maxGroupId;

    /**
     * The dependency graph of the communications in the RETE network
     */
    protected final Graph<Node> dependencyGraph;

    /**
     * Incremental SCC information about the dependency graph
     */
    protected final IncSCCAlg<Node> sccInformationProvider;

    /**
     * Precomputed node -> communication group map
     */
    protected final Map<Node, CommunicationGroup> groupMap;

    /**
     * Priority queue of active communication groups
     */
    protected final Queue<CommunicationGroup> groupQueue;

    // groups should have a simple integer flag which represents its position in a priority queue
    // priority queue only contains the ACTIVE groups

    public CommunicationTracker() {
        this.dependencyGraph = new Graph<Node>();
        this.sccInformationProvider = new IncSCCAlg<Node>(this.dependencyGraph);
        this.groupQueue = new PriorityQueue<CommunicationGroup>();
        this.groupMap = new HashMap<Node, CommunicationGroup>();
    }

    public Graph<Node> getDependencyGraph() {
        return dependencyGraph;
    }

    public CommunicationGroup getGroup(final Node node) {
        return this.groupMap.get(node);
    }

    private void precomputeGroups() {
        groupMap.clear();

        // reconstruct group map from dependency graph
        final Graph<Node> reducedGraph = sccInformationProvider.getReducedGraph();
        final List<Node> representatives = TopologicalSorting.compute(reducedGraph);

        for (int i = 0; i < representatives.size(); i++) { // groups for SCC representatives
            final Node representative = representatives.get(i);
            createAndStoreGroup(representative, i);
        }

        minGroupId = 0;
        maxGroupId = representatives.size() - 1;

        for (final Node node : dependencyGraph.getAllNodes()) { // extend group map to the rest of nodes
            final Node representative = sccInformationProvider.getRepresentative(node);
            final CommunicationGroup group = groupMap.get(representative);
            if (representative != node) {
                addToGroup(node, group);
            }
        }

        for (final Node node : dependencyGraph.getAllNodes()) {
            // set fall-through flags of default mailboxes
            precomputeFallThroughFlag(node);
            // set split flag of adaptive mailboxes
            processNode(node);
        }

        // reconstruct new queue contents based on new group map
        if (!groupQueue.isEmpty()) {
            final Set<CommunicationGroup> oldActiveGroups = new HashSet<CommunicationGroup>(groupQueue);
            groupQueue.clear();

            for (final CommunicationGroup oldGroup : oldActiveGroups) {
                for (final Entry<MessageSelector, Collection<Mailbox>> entry : oldGroup.getMailboxes().entrySet()) {
                    for (final Mailbox mailbox : entry.getValue()) {
                        final CommunicationGroup newGroup = this.groupMap.get(mailbox.getReceiver());
                        newGroup.notifyHasMessage(mailbox, entry.getKey());
                    }
                }

                for (final RederivableNode node : oldGroup.getRederivables()) {
                    final CommunicationGroup newGroup = this.groupMap.get(node);
                    newGroup.addRederivable(node);
                }
                oldGroup.isEnqueued = false;
            }
        }
    }

    private void addToGroup(final Node node, final CommunicationGroup group) {
        groupMap.put(node, group);
        if (node instanceof Receiver) {
            ((Receiver) node).getMailbox().setCurrentGroup(group);
            if (node instanceof IGroupable) {
                ((IGroupable) node).setCurrentGroup(group);
            }
        }
    }

    /**
     * Depends on the groups, as well as the parent nodes of the argument, so recomputation is needed if these change
     */
    private void precomputeFallThroughFlag(final Node node) {
        CommunicationGroup group = groupMap.get(node);
        if (node instanceof Receiver) {
            IGroupable mailbox = ((Receiver) node).getMailbox();
            if (mailbox instanceof FallThroughMailbox) {
                Set<Node> directParents = dependencyGraph.getSourceNodes(node).distinctValues();
                // decide between using quick&cheap fall-through, or allowing for update cancellation
                boolean fallThrough =
                        // disallow fallthrough: updates at production nodes should cancel, if they can be trimmed or
                        // disjunctive
                        (!(node instanceof ProductionNode && ( // it is a production node...
                        // with more than one parent
                        directParents.size() > 0 ||
                        // or true trimming in its sole parent
                                directParents.size() == 1 && trueTrimming(directParents.iterator().next())))) &&
                        // disallow fallthrough: external updates should be stored (if updates are delayed)
                                (!(node instanceof ExternalInputEnumeratorNode));
                // do additional checks
                if (fallThrough) {
                    // recursive parent groups generate excess updates that should be cancelled after delete&rederive
                    // phases
                    // aggregator and transitive closure parent nodes also generate excess updates that should be
                    // cancelled
                    directParentLoop: for (Node directParent : directParents) {
                        Set<Node> parentsToCheck = new HashSet<>();
                        // check the case where a direct parent is the reason for mailbox usage
                        parentsToCheck.add(directParent);
                        // check the case where an indirect parent (join slot) is the reason for mailbox usage
                        if (directParent instanceof DualInputNode) {
                            // in case of existence join (typically antijoin), a mailbox should allow
                            // an insertion and deletion (at the secondary slot) to cancel each other out
                            if (directParent instanceof ExistenceNode) {
                                fallThrough = false;
                                break directParentLoop;
                            }
                            // in beta nodes, indexer slots (or their active nodes) are considered indirect parents
                            DualInputNode dualInput = (DualInputNode) directParent;
                            IterableIndexer primarySlot = dualInput.getPrimarySlot();
                            if (primarySlot != null)
                                parentsToCheck.add(primarySlot.getActiveNode());
                            Indexer secondarySlot = dualInput.getSecondarySlot();
                            if (secondarySlot != null)
                                parentsToCheck.add(secondarySlot.getActiveNode());
                        }
                        for (Node parent : parentsToCheck) {
                            CommunicationGroup parentGroup = groupMap.get(parent);
                            if ( // parent is in a different, recursive group
                            (group != parentGroup && parentGroup.isRecursive()) ||
                            // node and parent within the same recursive group, and...
                                    (group == parentGroup && group.isRecursive() && (
                                    // parent is a transitive closure or aggregator node, or a trimmer
                                    // allow trimmed or disjunctive tuple updates to cancel each other
                                    (parent instanceof TransitiveClosureNode) || (parent instanceof IAggregatorNode)
                                            || trueTrimming(parent)))) {
                                fallThrough = false;
                                break directParentLoop;
                            }
                        }
                    }
                }
                // overwrite fallthrough flag with newly computed value
                ((FallThroughMailbox) mailbox).setFallThrough(fallThrough);
            }
        }
    }

    /**
     * A trimmer node that actually eliminates some columns (not just reorders)
     */
    private boolean trueTrimming(Node node) {
        if (node instanceof TrimmerNode) {
            TupleMask mask = ((TrimmerNode) node).getMask();
            return (mask.indices.length != mask.sourceWidth);
        }
        return false;
    }

    public void activateUnenqueued(final CommunicationGroup group) {
        groupQueue.add(group);
        group.isEnqueued = true;
    }

    public void deactivate(final CommunicationGroup group) {
        groupQueue.remove(group);
        group.isEnqueued = false;
    }

    public CommunicationGroup getAndRemoveFirstGroup() {
        final CommunicationGroup group = groupQueue.poll();
        group.isEnqueued = false;
        return group;
    }

    public boolean isEmpty() {
        return groupQueue.isEmpty();
    }

    protected abstract CommunicationGroup createGroup(final Node representative, final int index);

    protected CommunicationGroup createAndStoreGroup(final Node representative, final int index) {
        final CommunicationGroup group = createGroup(representative, index);
        addToGroup(representative, group);
        return group;
    }

    /**
     * Registers the dependency that the target {@link Node} depends on the source {@link Node}. In other words, source
     * may send messages to target in the RETE network.
     *
     * @param source
     *            the source node
     * @param target
     *            the target node
     */
    public void registerDependency(final Node source, final Node target) {
        // nodes can be immediately inserted, if they already exist in the graph, this is a noop
        dependencyGraph.insertNode(source);
        dependencyGraph.insertNode(target);

        // query all these information before the actual edge insertion
        // because SCCs may be unified during the process
        final Node sourceRepresentative = sccInformationProvider.getRepresentative(source);
        final Node targetRepresentative = sccInformationProvider.getRepresentative(target);
        final boolean hadOutgoingEdges = sccInformationProvider.hasOutgoingEdges(targetRepresentative);

        // insert the edge
        dependencyGraph.insertEdge(source, target);

        // create groups if they do not yet exist
        CommunicationGroup sourceGroup = groupMap.get(sourceRepresentative);
        if (sourceGroup == null) {
            // create on-demand with the next smaller group id
            sourceGroup = createAndStoreGroup(sourceRepresentative, --minGroupId);
        }
        final int sourceIndex = sourceGroup.identifier;

        CommunicationGroup targetGroup = groupMap.get(targetRepresentative);
        if (targetGroup == null) {
            // create on-demand with the next larger group id
            targetGroup = createAndStoreGroup(targetRepresentative, ++maxGroupId);
        }
        final int targetIndex = targetGroup.identifier;

        if (sourceIndex <= targetIndex) {
            // indices obey current topological ordering
            refreshFallThroughFlag(target);
            processNode(source);
            processNode(target);
        } else if (sourceIndex > targetIndex && !hadOutgoingEdges) {
            // indices violate current topological ordering, but we can simply bump the target index
            final boolean wasEnqueued = targetGroup.isEnqueued;
            if (wasEnqueued) {
                groupQueue.remove(targetGroup);
            }
            targetGroup.identifier = ++maxGroupId;
            if (wasEnqueued) {
                groupQueue.add(targetGroup);
            }

            refreshFallThroughFlag(target);
            processNode(source);
            processNode(target);
        } else {
            // needs a full re-computation because of more complex change
            precomputeGroups();
        }
    }

    /**
     * Returns true if the given {@link Node} is in a recursive {@link CommunicationGroup}, false otherwise.
     */
    public boolean isInRecursiveGroup(final Node node) {
        final CommunicationGroup group = this.getGroup(node);
        if (group == null) {
            return false;
        } else {
            return group.isRecursive();
        }
    }

    /**
     * Returns true if the given two {@link Node}s are in the same {@link CommunicationGroup}.
     */
    public boolean areInSameGroup(final Node left, final Node right) {
        final CommunicationGroup leftGroup = this.getGroup(left);
        final CommunicationGroup rightGroup = this.getGroup(right);
        return leftGroup != null && leftGroup == rightGroup;
    }

    /**
     * Unregisters a dependency between source and target.
     * 
     * @param source
     *            the source node
     * @param target
     *            the target node
     */
    public void unregisterDependency(final Node source, final Node target) {
        // delete the edge first, and then query the SCC info provider
        this.dependencyGraph.deleteEdgeIfExists(source, target);

        final Node sourceRepresentative = sccInformationProvider.getRepresentative(source);
        final Node targetRepresentative = sccInformationProvider.getRepresentative(target);

        // if they are still in the same SCC,
        // then this deletion did not affect the SCCs,
        // and it is sufficient to recompute affected fall-through flags;
        // otherwise, we need a new pre-computation for the groupMap and groupQueue
        if (sourceRepresentative.equals(targetRepresentative)) {
            // this deletion could not have affected the split flags
            refreshFallThroughFlag(target);
        } else {
            // preComputeGroups takes care of the split flag maintenance
            precomputeGroups();
        }
    }

    /**
     * Refresh fall-through flags if dependencies change for given target, but no SCC change
     */
    private void refreshFallThroughFlag(final Node target) {
        precomputeFallThroughFlag(target);
        if (target instanceof DualInputNode) {
            for (Node indirectTarget : dependencyGraph.getTargetNodes(target).distinctValues()) {
                precomputeFallThroughFlag(indirectTarget);
            }
        }
    }

    protected abstract void processNode(final Node node);

    public abstract Mailbox proxifyMailbox(final Node requester, final Mailbox original);

    public abstract IndexerListener proxifyIndexerListener(final Node requester, final IndexerListener original);

}
