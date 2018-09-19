/*******************************************************************************
 * Copyright (c) 2010-2016, Grill Balázs, IncQuery Labs Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Grill Balázs - initial API and implementation
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.localsearch.operations.check;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.viatra.query.runtime.localsearch.MatchingFrame;
import org.eclipse.viatra.query.runtime.localsearch.matcher.ISearchContext;
import org.eclipse.viatra.query.runtime.localsearch.operations.CheckOperationExecutor;
import org.eclipse.viatra.query.runtime.localsearch.operations.IPatternMatcherOperation;
import org.eclipse.viatra.query.runtime.localsearch.operations.ISearchOperation;
import org.eclipse.viatra.query.runtime.localsearch.operations.util.CallInformation;
import org.eclipse.viatra.query.runtime.matchers.backend.IQueryResultProvider;
import org.eclipse.viatra.query.runtime.matchers.psystem.aggregations.IMultisetAggregationOperator;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicdeferred.AggregatorConstraint;
import org.eclipse.viatra.query.runtime.matchers.tuple.VolatileModifiableMaskedTuple;

/**
 * Calculates the aggregated value of a column based on the given {@link AggregatorConstraint}
 * 
 * @author Balázs Grill
 * @since 1.4
 * @noextend This class is not intended to be subclassed by clients.
 */
public class AggregatorCheck implements ISearchOperation, IPatternMatcherOperation {

    private class Executor extends CheckOperationExecutor {
        
        private final VolatileModifiableMaskedTuple maskedTuple;
        private IQueryResultProvider matcher;
        
        public Executor() {
            super();
            this.maskedTuple = new VolatileModifiableMaskedTuple(information.getThinFrameMask());
        }
        
        @Override
        public void onInitialize(MatchingFrame frame, ISearchContext context) {
            super.onInitialize(frame, context);
            maskedTuple.updateTuple(frame);
            matcher = context.getMatcher(information.getCallWithAdornment());
        }

        @Override
        protected boolean check(MatchingFrame frame, ISearchContext context) {
            IMultisetAggregationOperator<?, ?, ?> operator = aggregator.getAggregator().getOperator();
            Object result = aggregate(operator, aggregator.getAggregatedColumn(), frame);
            return result == null ? false : Objects.equals(frame.getValue(position), result);
        }

        @SuppressWarnings("unchecked")
        private <Domain, Accumulator, AggregateResult> AggregateResult aggregate(
                IMultisetAggregationOperator<Domain, Accumulator, AggregateResult> operator, int aggregatedColumn,
                MatchingFrame initialFrame) {
            maskedTuple.updateTuple(initialFrame);
            final Stream<Domain> valueStream = matcher.getAllMatches(information.getParameterMask(), maskedTuple)
                    .map(match -> (Domain) match.get(aggregatedColumn));
            return operator.aggregateStream(valueStream);
        }
        
        @Override
        public ISearchOperation getOperation() {
            return AggregatorCheck.this;
        }
    }
    
    private final int position;
    private final AggregatorConstraint aggregator;
    private final CallInformation information;
    
    /**
     * @since 1.7
     */
    public AggregatorCheck(CallInformation information, AggregatorConstraint aggregator, int position) {
        super();
        this.information = information;
        this.position = position;
        this.aggregator = aggregator;
    }

    @Override
    public ISearchOperationExecutor createExecutor() {
        return new Executor();
    }

    @Override
    public List<Integer> getVariablePositions() {
        return Collections.singletonList(position);
    }
    
    @Override
    public String toString() {
        return toString(Object::toString);
    }
    
    @Override
    public String toString(Function<Integer, String> variableMapping) {
        return "check     "+variableMapping.apply(position)+" = " + aggregator.getAggregator().getOperator().getName() + " find " + information.toString(variableMapping);
    }

    @Override
    public CallInformation getCallInformation() {
        return information;
    }
    
}
