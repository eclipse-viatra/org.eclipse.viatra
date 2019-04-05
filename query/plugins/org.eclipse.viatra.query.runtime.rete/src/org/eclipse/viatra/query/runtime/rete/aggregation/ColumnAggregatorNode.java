/*******************************************************************************
 * Copyright (c) 2010-2016, Gabor Bergmann, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.rete.aggregation;

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.viatra.query.runtime.matchers.context.IPosetComparator;
import org.eclipse.viatra.query.runtime.matchers.psystem.aggregations.IMultisetAggregationOperator;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;
import org.eclipse.viatra.query.runtime.matchers.tuple.TupleMask;
import org.eclipse.viatra.query.runtime.matchers.util.CollectionsFactory;
import org.eclipse.viatra.query.runtime.rete.network.Direction;
import org.eclipse.viatra.query.runtime.rete.network.PosetAwareReceiver;
import org.eclipse.viatra.query.runtime.rete.network.RederivableNode;
import org.eclipse.viatra.query.runtime.rete.network.ReteContainer;
import org.eclipse.viatra.query.runtime.rete.network.communication.CommunicationGroup;
import org.eclipse.viatra.query.runtime.rete.network.communication.Timestamp;
import org.eclipse.viatra.query.runtime.rete.network.mailbox.Mailbox;
import org.eclipse.viatra.query.runtime.rete.network.mailbox.timeless.BehaviorChangingMailbox;
import org.eclipse.viatra.query.runtime.rete.network.mailbox.timeless.PosetAwareMailbox;
import org.eclipse.viatra.query.runtime.rete.network.mailbox.timely.TimelyMailbox;

/**
 * Groups incoming tuples by the given mask, and aggregates values at a specific index in each group.
 * 
 * <p>
 * Direct children are not supported, use via outer join indexers instead.
 *
 * <p>
 * 
 * This node cannot be used in recursive differential dataflow evaluation.
 * 
 * @author Gabor Bergmann
 * @author Tamas Szabo
 * @since 1.4
 */
public class ColumnAggregatorNode<Domain, Accumulator, AggregateResult>
        extends AbstractColumnAggregatorNode<Domain, Accumulator, AggregateResult>
        implements RederivableNode, PosetAwareReceiver {

    /**
     * @since 1.6
     */
    protected final IPosetComparator posetComparator;

    /**
     * @since 1.6
     */
    protected final boolean deleteRederiveEvaluation;

    // invariant: neutral values are not stored
    /**
     * @since 1.6
     */
    protected final Map<Tuple, Accumulator> memory;
    /**
     * @since 1.6
     */
    protected final Map<Tuple, Accumulator> rederivableMemory;

    /**
     * @since 1.7
     */
    protected CommunicationGroup currentGroup = null;

    /**
     * Creates a new column aggregator node.
     * 
     * @param reteContainer
     *            the RETE container of the node
     * @param operator
     *            the aggregation operator
     * @param deleteRederiveEvaluation
     *            true if the node should run in DRED mode, false otherwise
     * @param groupMask
     *            the mask that masks a tuple to obtain the key that we are grouping-by
     * @param columnMask
     *            the mask that masks a tuple to obtain the tuple element(s) that we are aggregating over
     * @param posetComparator
     *            the poset comparator for the column, if known, otherwise it can be null
     * @since 1.6
     */
    public ColumnAggregatorNode(final ReteContainer reteContainer,
            final IMultisetAggregationOperator<Domain, Accumulator, AggregateResult> operator,
            final boolean deleteRederiveEvaluation, final TupleMask groupMask, final TupleMask columnMask,
            final IPosetComparator posetComparator) {
        super(reteContainer, operator, groupMask, columnMask);
        this.memory = CollectionsFactory.createMap();
        this.rederivableMemory = CollectionsFactory.createMap();
        this.deleteRederiveEvaluation = deleteRederiveEvaluation;
        this.posetComparator = posetComparator;
        // mailbox MUST be instantiated after the fields are all set
        this.mailbox = instantiateMailbox();
    }

    /**
     * Creates a new column aggregator node.
     * 
     * @param reteContainer
     *            the RETE container of the node
     * @param operator
     *            the aggregation operator
     * @param groupMask
     *            the mask that masks a tuple to obtain the key that we are grouping-by
     * @param aggregatedColumn
     *            the index of the column that the aggregator node is aggregating over
     */
    public ColumnAggregatorNode(final ReteContainer reteContainer,
            final IMultisetAggregationOperator<Domain, Accumulator, AggregateResult> operator,
            final TupleMask groupMask, final int aggregatedColumn) {
        this(reteContainer, operator, false, groupMask, TupleMask.selectSingle(aggregatedColumn, groupMask.sourceWidth),
                null);
    }

    @Override
    public boolean isInDRedMode() {
        return this.deleteRederiveEvaluation;
    }

    @Override
    protected Mailbox instantiateMailbox() {
        if (this.reteContainer.isDifferentialDataFlowEvaluation()) {
            return new TimelyMailbox(this, this.reteContainer);
        } else if (groupMask != null && columnMask != null && posetComparator != null) {
            return new PosetAwareMailbox(this, this.reteContainer);
        } else {
            return new BehaviorChangingMailbox(this, this.reteContainer);
        }
    }

    @Override
    public TupleMask getCoreMask() {
        return groupMask;
    }

    @Override
    public TupleMask getPosetMask() {
        return columnMask;
    }

    @Override
    public IPosetComparator getPosetComparator() {
        return posetComparator;
    }

    @Override
    public void rederiveOne() {
        final Entry<Tuple, Accumulator> entry = rederivableMemory.entrySet().iterator().next();
        final Tuple group = entry.getKey();
        final Accumulator accumulator = entry.getValue();
        rederivableMemory.remove(group);
        memory.put(group, accumulator);
        // unregister the node if there is nothing left to be re-derived
        if (this.rederivableMemory.isEmpty()) {
            currentGroup.removeRederivable(this);
        }
        final AggregateResult value = operator.getAggregate(accumulator);
        propagate(group, NEUTRAL, value, Timestamp.ZERO);
    }

    @Override
    public void updateWithPosetInfo(final Direction direction, final Tuple update, final boolean monotone) {
        if (this.deleteRederiveEvaluation) {
            updateWithDeleteAndRederive(direction, update, monotone);
        } else {
            updateDefault(direction, update, Timestamp.ZERO);
        }
    }

    @Override
    public void update(final Direction direction, final Tuple update, final Timestamp timestamp) {
        updateWithPosetInfo(direction, update, false);
    }

    /**
     * @since 1.6
     */
    protected void updateDefault(final Direction direction, final Tuple update, final Timestamp timestamp) {
        final Tuple key = groupMask.transform(update);
        final Tuple value = columnMask.transform(update);
        @SuppressWarnings("unchecked")
        final Domain aggregableValue = (Domain) runtimeContext.unwrapElement(value.get(0));
        final boolean isInsertion = direction == Direction.INSERT;

        final Accumulator oldMainAccumulator = getMainAccumulator(key);
        final AggregateResult oldValue = operator.getAggregate(oldMainAccumulator);

        final Accumulator newMainAccumulator = operator.update(oldMainAccumulator, aggregableValue, isInsertion);
        storeIfNotNeutral(key, newMainAccumulator, memory);
        final AggregateResult newValue = operator.getAggregate(newMainAccumulator);

        propagate(key, oldValue, newValue, timestamp);
    }

    /**
     * @since 1.6
     */
    protected void updateWithDeleteAndRederive(final Direction direction, final Tuple update, final boolean monotone) {
        final Tuple group = groupMask.transform(update);
        final Tuple value = columnMask.transform(update);
        @SuppressWarnings("unchecked")
        final Domain aggregableValue = (Domain) runtimeContext.unwrapElement(value.get(0));
        final boolean isInsertion = direction == Direction.INSERT;

        Accumulator oldMainAccumulator = memory.get(group);
        Accumulator oldRederivableAccumulator = rederivableMemory.get(group);

        if (direction == Direction.INSERT) {
            // INSERT
            if (oldRederivableAccumulator != null) {
                // the group is in the re-derivable memory
                final Accumulator newRederivableAccumulator = operator.update(oldRederivableAccumulator,
                        aggregableValue, isInsertion);
                storeIfNotNeutral(group, newRederivableAccumulator, rederivableMemory);
                if (rederivableMemory.isEmpty()) {
                    // there is nothing left to be re-derived
                    // this can happen if the accumulator became neutral in response to the INSERT
                    currentGroup.removeRederivable(this);
                }
            } else {
                // the group is in the main memory
                // at this point, it can happen that we need to initialize with a neutral accumulator
                if (oldMainAccumulator == null) {
                    oldMainAccumulator = operator.createNeutral();
                }

                final AggregateResult oldValue = operator.getAggregate(oldMainAccumulator);
                final Accumulator newMainAccumulator = operator.update(oldMainAccumulator, aggregableValue,
                        isInsertion);
                storeIfNotNeutral(group, newMainAccumulator, memory);
                final AggregateResult newValue = operator.getAggregate(newMainAccumulator);
                propagate(group, oldValue, newValue, Timestamp.ZERO);
            }
        } else {
            // DELETE
            if (oldRederivableAccumulator != null) {
                // the group is in the re-derivable memory
                if (oldMainAccumulator != null) {
                    issueError("[INTERNAL ERROR] Inconsistent state for " + update
                            + " because it is present both in the main and re-derivable memory in the ColumnAggregatorNode "
                            + this + " for pattern(s) " + getTraceInfoPatternsEnumerated(), null);
                }
                try {
                    final Accumulator newRederivableAccumulator = operator.update(oldRederivableAccumulator,
                            aggregableValue, isInsertion);
                    storeIfNotNeutral(group, newRederivableAccumulator, rederivableMemory);
                    if (rederivableMemory.isEmpty()) {
                        // there is nothing left to be re-derived
                        // this can happen if the accumulator became neutral in response to the DELETE
                        currentGroup.removeRederivable(this);
                    }
                } catch (final NullPointerException ex) {
                    issueError("[INTERNAL ERROR] Deleting a domain element in " + update
                            + " which did not exist before in ColumnAggregatorNode " + this + " for pattern(s) "
                            + getTraceInfoPatternsEnumerated(), ex);
                }
            } else {
                // the group is in the main memory
                // at this point, it can happen that we need to initialize with a neutral accumulator
                if (oldMainAccumulator == null) {
                    oldMainAccumulator = operator.createNeutral();
                }

                final AggregateResult oldValue = operator.getAggregate(oldMainAccumulator);
                final Accumulator newMainAccumulator = operator.update(oldMainAccumulator, aggregableValue,
                        isInsertion);
                final AggregateResult newValue = operator.getAggregate(newMainAccumulator);

                if (monotone) {
                    storeIfNotNeutral(group, newMainAccumulator, memory);
                    propagate(group, oldValue, newValue, Timestamp.ZERO);
                } else {
                    final boolean wasEmpty = rederivableMemory.isEmpty();
                    if (storeIfNotNeutral(group, newMainAccumulator, rederivableMemory) && wasEmpty) {
                        currentGroup.addRederivable(this);
                    }
                    memory.remove(group);
                    propagate(group, oldValue, NEUTRAL, Timestamp.ZERO);
                }
            }
        }
    }

    @Override
    public void clear() {
        this.memory.clear();
        this.rederivableMemory.clear();
        this.childMailboxes.clear();
    }

    /**
     * Returns true if the accumulator was stored, false otherwise.
     * 
     * @since 1.6
     */
    protected boolean storeIfNotNeutral(final Tuple key, final Accumulator accumulator,
            final Map<Tuple, Accumulator> memory) {
        if (operator.isNeutral(accumulator)) {
            memory.remove(key);
            return false;
        } else {
            memory.put(key, accumulator);
            return true;
        }
    }

    @Override
    public Tuple getAggregateTuple(final Tuple key) {
        final Accumulator accumulator = getMainAccumulator(key);
        final AggregateResult aggregateResult = operator.getAggregate(accumulator);
        return tupleFromAggregateResult(key, aggregateResult);
    }

    @Override
    public AggregateResult getAggregateResult(final Tuple key) {
        final Accumulator accumulator = getMainAccumulator(key);
        return operator.getAggregate(accumulator);
    }

    /**
     * @since 1.6
     */
    protected Accumulator getMainAccumulator(final Tuple key) {
        return getAccumulator(key, memory);
    }

    /**
     * @since 1.6
     */
    protected Accumulator getRederivableAccumulator(final Tuple key) {
        return getAccumulator(key, rederivableMemory);
    }

    /**
     * @since 1.6
     */
    protected Accumulator getAccumulator(final Tuple key, final Map<Tuple, Accumulator> memory) {
        Accumulator accumulator = memory.get(key);
        if (accumulator == null) {
            return operator.createNeutral();
        } else {
            return accumulator;
        }
    }

    @Override
    public CommunicationGroup getCurrentGroup() {
        return currentGroup;
    }

    @Override
    public void setCurrentGroup(final CommunicationGroup currentGroup) {
        this.currentGroup = currentGroup;
    }

}
