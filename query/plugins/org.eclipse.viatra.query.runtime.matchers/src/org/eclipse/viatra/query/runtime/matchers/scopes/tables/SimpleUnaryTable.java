/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQuery Labs Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Gabor Bergmann - initial API and implementation
 *******************************************************************************/

package org.eclipse.viatra.query.runtime.matchers.scopes.tables;

import java.util.Collections;
import java.util.Optional;

import org.eclipse.viatra.query.runtime.matchers.context.IInputKey;
import org.eclipse.viatra.query.runtime.matchers.tuple.ITuple;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;
import org.eclipse.viatra.query.runtime.matchers.tuple.TupleMask;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuples;
import org.eclipse.viatra.query.runtime.matchers.util.Accuracy;
import org.eclipse.viatra.query.runtime.matchers.util.CollectionsFactory;
import org.eclipse.viatra.query.runtime.matchers.util.Direction;
import org.eclipse.viatra.query.runtime.matchers.util.IMemory;

/**
 * Simple value set.
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is no guarantee that this API will
 * work or that it will remain the same.
 *
 * @since 2.0
 * @author Gabor Bergmann
 */
public class SimpleUnaryTable<Value> extends AbstractIndexTable implements ITableWriterUnary.Table<Value> {

    protected IMemory<Value> values = CollectionsFactory.createMultiset(); // TODO use SetMemory if unique

    private boolean unique;

    /**
     * @param unique
     *            client promises to only insert a given tuple with multiplicity one
     */
    public SimpleUnaryTable(IInputKey inputKey, ITableContext tableContext, boolean unique) {
        super(inputKey, tableContext);
        this.unique = unique;
        if (1 != inputKey.getArity())
            throw new IllegalArgumentException(inputKey.toString());
    }

    @Override
    public void write(Direction direction, Value value) {
        if (direction == Direction.INSERT) {
            boolean changed = values.addOne(value);
            if (unique && !changed) {
                String msg = String.format(
                        "Error: trying to add duplicate value %s to the unique set %s. This indicates some errors in underlying model representation.",
                        value, getInputKey().getPrettyPrintableName());
                logError(msg);
            }
            if (changed && emitNotifications) {
                deliverChangeNotifications(Tuples.staticArityFlatTupleOf(value), true);
            }
        } else { // DELETE
            boolean changed = values.removeOne(value);
            if (unique && !changed) {
                String msg = String.format(
                        "Error: trying to remove duplicate value %s from the unique set %s. This indicates some errors in underlying model representation.",
                        value, getInputKey().getPrettyPrintableName());
                logError(msg);
            }
            if (changed && emitNotifications) {
                deliverChangeNotifications(Tuples.staticArityFlatTupleOf(value), false);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean containsTuple(ITuple seed) {
        return values.containsNonZero((Value) seed.get(0));
    }

    @Override
    public int countTuples(TupleMask seedMask, ITuple seed) {
        if (seedMask.getSize() == 0) { // unseeded
            return values.size();
        } else {
            @SuppressWarnings("unchecked")
            Value value = (Value) seed.get(0);
            return values.containsNonZero(value) ? 1 : 0;
        }
    }


    @Override
    public Optional<Long> estimateProjectionSize(TupleMask groupMask, Accuracy requiredAccuracy) {
        // always exact count
        if (groupMask.getSize() == 0) {
            return values.isEmpty() ? Optional.of(0L) : Optional.of(1L);
        } else {
            return Optional.of((long)values.size());
        }
    }

    
    @Override
    public Iterable<Tuple> enumerateTuples(TupleMask seedMask, ITuple seed) {
        if (seedMask.getSize() == 0) { // unseeded
            return () -> values.distinctValues().stream().map(Tuples::staticArityFlatTupleOf).iterator();
        } else {
            @SuppressWarnings("unchecked")
            Value value = (Value) seed.get(0);
            if (values.containsNonZero(value))
                return Collections.singleton(Tuples.staticArityFlatTupleOf(value));
            else
                return Collections.emptySet();
        }
    }

    @Override
    public Iterable<? extends Object> enumerateValues(TupleMask seedMask, ITuple seed) {
        if (seedMask.getSize() == 0) { // unseeded
            return values;
        } else {
            throw new IllegalArgumentException(seedMask.toString());
            // @SuppressWarnings("unchecked")
            // Value value = (Value) seed.get(0);
            // if (values.contains(value))
            // return Collections.singleton(value);
            // else
            // return Collections.emptySet();
        }
    }

}
