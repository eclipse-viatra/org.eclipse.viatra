/**
 * Copyright (c) 2010-2016, Grill Balázs, IncQuery Labs Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * Grill Balázs - initial API and implementation
 */
package org.eclipse.viatra.query.runtime.localsearch.planner.cost.impl;

import static org.eclipse.viatra.query.runtime.matchers.planning.helpers.StatisticsHelper.min;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.viatra.query.runtime.localsearch.matcher.integration.AbstractLocalSearchResultProvider;
import org.eclipse.viatra.query.runtime.localsearch.planner.cost.IConstraintEvaluationContext;
import org.eclipse.viatra.query.runtime.localsearch.planner.cost.ICostFunction;
import org.eclipse.viatra.query.runtime.matchers.ViatraQueryRuntimeException;
import org.eclipse.viatra.query.runtime.matchers.backend.IQueryResultProvider;
import org.eclipse.viatra.query.runtime.matchers.context.IInputKey;
import org.eclipse.viatra.query.runtime.matchers.planning.helpers.FunctionalDependencyHelper;
import org.eclipse.viatra.query.runtime.matchers.planning.helpers.StatisticsHelper;
import org.eclipse.viatra.query.runtime.matchers.psystem.IQueryReference;
import org.eclipse.viatra.query.runtime.matchers.psystem.PConstraint;
import org.eclipse.viatra.query.runtime.matchers.psystem.PVariable;
import org.eclipse.viatra.query.runtime.matchers.psystem.analysis.QueryAnalyzer;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicdeferred.AggregatorConstraint;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicdeferred.ExportedParameter;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicdeferred.ExpressionEvaluation;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicdeferred.Inequality;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicdeferred.NegativePatternCall;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicdeferred.PatternMatchCounter;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicdeferred.TypeFilterConstraint;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.AbstractTransitiveClosure;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.BinaryReflexiveTransitiveClosure;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.BinaryTransitiveClosure;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.ConstantValue;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.PositivePatternCall;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.TypeConstraint;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PParameter;
import org.eclipse.viatra.query.runtime.matchers.tuple.TupleMask;
import org.eclipse.viatra.query.runtime.matchers.util.Accuracy;
import org.eclipse.viatra.query.runtime.matchers.util.Preconditions;

/**
 * Cost function which calculates cost based on the cardinality of items in the runtime model
 * 
 * <p> To provide custom statistics, override 
 *  {@link #projectionSize(IConstraintEvaluationContext, IInputKey, TupleMask, Accuracy)} 
 *  and {@link #bucketSize(IQueryReference, IConstraintEvaluationContext, TupleMask)}.
 * 
 * @author Grill Balázs
 * @since 1.4
 */
public abstract class StatisticsBasedConstraintCostFunction implements ICostFunction {
    protected static final double MAX_COST = 250.0;

    protected static final double DEFAULT_COST = StatisticsBasedConstraintCostFunction.MAX_COST - 100.0;
            
    /**
     * @since 2.1
     */
    public static final double INVERSE_NAVIGATION_PENALTY_DEFAULT =      1.0;
    /**
     * @since 2.1
     */
    public static final double INVERSE_NAVIGATION_PENALTY_GENERIC =  0.01;
    
    
    private final double inverseNavigationPenalty;
    
    /**
     * @since 2.1
     */
    protected Optional<Double> transitiveClosureDefaultDepth() {
        return Optional.of(20.0);
    }
    
    
    /**
     * @since 2.1
     */
    public StatisticsBasedConstraintCostFunction(double inverseNavigationPenalty) {
        super();
        this.inverseNavigationPenalty = inverseNavigationPenalty;
    }
    public StatisticsBasedConstraintCostFunction() {
        this(INVERSE_NAVIGATION_PENALTY_DEFAULT);
    }

    /**
     * @deprecated call and implement {@link #projectionSize(IConstraintEvaluationContext, IInputKey, TupleMask, Accuracy)} instead
     */
    @Deprecated
    public long countTuples(final IConstraintEvaluationContext input, final IInputKey supplierKey) {
        return projectionSize(input, supplierKey, TupleMask.identity(supplierKey.getArity()), Accuracy.EXACT_COUNT).orElse(-1L);
    }
    
    /**
     * Override this to provide custom statistics on edge/node counts.
     * New implementors shall implement this instead of {@link #countTuples(IConstraintEvaluationContext, IInputKey)}
     * @since 2.1
     */
    public Optional<Long> projectionSize(final IConstraintEvaluationContext input, final IInputKey supplierKey,
            final TupleMask groupMask, Accuracy requiredAccuracy) {
        long legacyCount = countTuples(input, supplierKey);
        return legacyCount < 0 ? Optional.empty() : Optional.of(legacyCount);
    }

    /**
     * Override this to provide custom estimates for match set bucket sizes of called patterns.
     * @since 2.1
     */
    public Optional<Double> bucketSize(final IQueryReference patternCall,
            final IConstraintEvaluationContext input, TupleMask projMask) {
        IQueryResultProvider resultProvider = input.resultProviderRequestor().requestResultProvider(patternCall, null);
        // TODO hack: use LS cost instead of true bucket size estimate
        if (resultProvider instanceof AbstractLocalSearchResultProvider) {
            double estimatedCost = ((AbstractLocalSearchResultProvider) resultProvider).estimateCost(projMask);
            return Optional.of(estimatedCost);
        } else {            
            return resultProvider.estimateAverageBucketSize(projMask, Accuracy.APPROXIMATION);
        }
    }   
    
    /**
     * Override this to provide custom estimates for match set projection sizes of called patterns.
     * @since 2.1
     */
    public Optional<Long> projectionSize(final IQueryReference patternCall,
            final IConstraintEvaluationContext input, TupleMask projMask) {
        IQueryResultProvider resultProvider = input.resultProviderRequestor().requestResultProvider(patternCall, null);
        // TODO hack: use LS cost instead of true projection size estimate
        if (resultProvider instanceof AbstractLocalSearchResultProvider) {
            AbstractLocalSearchResultProvider lsProvider = (AbstractLocalSearchResultProvider) resultProvider;
            double estimatedCost = lsProvider.estimateCost(projMask);
            double costPerMatch = lsProvider.estimateCost(TupleMask.identity(projMask.getSourceWidth()));
            double denominator = Math.max(costPerMatch, 1.0);
            double matchCountEstimate = estimatedCost / denominator;
            return Optional.of((long)Math.min((double)Long.MAX_VALUE, matchCountEstimate));
        } else {            
            return resultProvider.estimateCardinality(projMask, Accuracy.APPROXIMATION);
        }
    }   
    

    @Override
    public double apply(final IConstraintEvaluationContext input) {
        return this.calculateCost(input.getConstraint(), input);
    }

    protected double _calculateCost(final ConstantValue constant, final IConstraintEvaluationContext input) {
        return 0.0f;
    }

    protected double _calculateCost(final TypeConstraint constraint, final IConstraintEvaluationContext input) {
        final Collection<PVariable> freeMaskVariables = input.getFreeVariables();
        final Collection<PVariable> boundMaskVariables = input.getBoundVariables();
        IInputKey supplierKey = constraint.getSupplierKey();
        long arity = supplierKey.getArity();

        if ((arity == 1)) {
            // unary constraint
            return calculateUnaryConstraintCost(constraint, input);
        } else if ((arity == 2)) {
            // binary constraint
            PVariable srcVariable = ((PVariable) constraint.getVariablesTuple().get(0));
            PVariable dstVariable = ((PVariable) constraint.getVariablesTuple().get(1));
            boolean isInverse = false;
            // Check if inverse navigation is needed along the edge
            if ((freeMaskVariables.contains(srcVariable) && boundMaskVariables.contains(dstVariable))) {
                isInverse = true;
            }
            double binaryExtendCost = calculateBinaryCost(supplierKey, srcVariable, dstVariable, isInverse, input);
            // Make inverse navigation slightly more expensive than forward navigation
            // See https://bugs.eclipse.org/bugs/show_bug.cgi?id=501078
            return (isInverse) ? binaryExtendCost + inverseNavigationPenalty : binaryExtendCost;
        } else {
            // n-ary constraint
            throw new UnsupportedOperationException("Cost calculation for arity " + arity + " is not implemented yet");
        }
    }

    
    /**
     * @deprecated use/implement {@link #calculateBinaryCost(IInputKey, PVariable, PVariable, boolean, IConstraintEvaluationContext)} instead
     */
    @Deprecated
    protected double calculateBinaryExtendCost(final IInputKey supplierKey, final PVariable srcVariable,
            final PVariable dstVariable, final boolean isInverse, long edgeCount /* TODO remove */,
            final IConstraintEvaluationContext input) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * @since 2.1
     */
    protected double calculateBinaryCost(final IInputKey supplierKey, final PVariable srcVariable,
            final PVariable dstVariable, final boolean isInverse, 
            final IConstraintEvaluationContext input) {
        final Collection<PVariable> freeMaskVariables = input.getFreeVariables();
        final PConstraint constraint = input.getConstraint();
        
//        IQueryMetaContext metaContext = input.getRuntimeContext().getMetaContext();
//        Collection<InputKeyImplication> implications = metaContext.getImplications(supplierKey);

        Optional<Long> edgeUpper = projectionSize(input,   supplierKey,    TupleMask.identity(2),          Accuracy.BEST_UPPER_BOUND);
        Optional<Long> srcUpper  = projectionSize(input,   supplierKey,    TupleMask.selectSingle(0, 2),   Accuracy.BEST_UPPER_BOUND);
        Optional<Long> dstUpper  = projectionSize(input,   supplierKey,    TupleMask.selectSingle(1, 2),   Accuracy.BEST_UPPER_BOUND);

        if (freeMaskVariables.contains(srcVariable) && freeMaskVariables.contains(dstVariable)) {
            Double branchCount = edgeUpper.map(Long::doubleValue).orElse(
                    srcUpper.map(Long::doubleValue).orElse(DEFAULT_COST)
                    *
                    dstUpper.map(Long::doubleValue).orElse(DEFAULT_COST)
            );      
            return branchCount;
            
        } else {
            
            Optional<Long> srcLower  = projectionSize(input,   supplierKey,    TupleMask.selectSingle(0, 2),   Accuracy.BEST_LOWER_BOUND);
            Optional<Long> dstLower  = projectionSize(input,   supplierKey,    TupleMask.selectSingle(1, 2),   Accuracy.BEST_LOWER_BOUND);
            
            List<Optional<Long>> nodeLower = Arrays.asList(srcLower, dstLower);
            List<Optional<Long>> nodeUpper = Arrays.asList(srcUpper, dstUpper);
            
            int from = isInverse ? 1 : 0;
            int to   = isInverse ? 0 : 1;
            
            Optional<Double> costEstimate = Optional.empty();
            
            if (!freeMaskVariables.contains(srcVariable) && !freeMaskVariables.contains(dstVariable)) {
                // both variables bound, this is a simple check
                costEstimate = min(costEstimate, 0.9);
            } // TODO use bucket size estimation in the runtime context
            costEstimate = min(costEstimate, 
                edgeUpper.flatMap((edges) -> 
                nodeLower.get(from).map((fromNodes) ->
                    // amortize edges over start nodes
                    (fromNodes == 0) ? 0.0 : (((double) edges) / fromNodes)
            )));
            if (navigatesThroughFunctionalDependencyInverse(input, constraint)) {
                costEstimate = min(costEstimate, 
                        nodeUpper.get(to).flatMap((toNodes) -> 
                        nodeLower.get(from).map((fromNodes) ->
                        // due to a reverse functional dependency, the destination count is an upper bound for the edge count
                        (fromNodes == 0) ? 0.0 : ((double) toNodes) / fromNodes
                    )));
            }
            if (! edgeUpper.isPresent()) {
                costEstimate = min(costEstimate, 
                        nodeUpper.get(to).flatMap((toNodes) -> 
                        nodeLower.get(from).map((fromNodes) ->
                        // If count is 0, no such element exists in the model, so there will be no branching
                        // TODO rethink, why dstNodeCount / srcNodeCount instead of dstNodeCount? 
                        // The universally valid bound would be something like sparseEdgeEstimate = dstNodeCount + 1.0
                        // If we assume sparseness, we can reduce it by a SPARSENESS_FACTOR (e.g. 0.1). 
                        // Alternatively, discount dstNodeCount * srcNodeCount on a SPARSENESS_EXPONENT (e.g 0.75) and then amortize over srcNodeCount.
                        fromNodes != 0 ? Math.max(1.0, ((double) toNodes) / fromNodes) : 1.0
                    )));
            } 
            if (navigatesThroughFunctionalDependency(input, constraint)) {
                // At most one destination value
                costEstimate = min(costEstimate, 1.0); 
            }
            
            return costEstimate.orElse(DEFAULT_COST);

        }
    }

    /**
     * @since 1.7
     */
    protected boolean navigatesThroughFunctionalDependency(final IConstraintEvaluationContext input,
            final PConstraint constraint) {
        return navigatesThroughFunctionalDependency(input, constraint, input.getBoundVariables(), input.getFreeVariables());
    }
    /**
     * @since 2.1
     */
    protected boolean navigatesThroughFunctionalDependencyInverse(final IConstraintEvaluationContext input,
            final PConstraint constraint) {
        return navigatesThroughFunctionalDependency(input, constraint, input.getFreeVariables(), input.getBoundVariables());
    }
    /**
     * @since 2.1
     */
    protected boolean navigatesThroughFunctionalDependency(final IConstraintEvaluationContext input,
            final PConstraint constraint, Collection<PVariable> determining, Collection<PVariable> determined) {
        final QueryAnalyzer queryAnalyzer = input.getQueryAnalyzer();
        final Map<Set<PVariable>, Set<PVariable>> functionalDependencies = queryAnalyzer
                .getFunctionalDependencies(Collections.singleton(constraint), false);
        final Set<PVariable> impliedVariables = FunctionalDependencyHelper.closureOf(determining,
                functionalDependencies);
        return ((impliedVariables != null) && impliedVariables.containsAll(determined));
    }
    
    protected double calculateUnaryConstraintCost(final TypeConstraint constraint,
            final IConstraintEvaluationContext input) {
        PVariable variable = (PVariable) constraint.getVariablesTuple().get(0);
        if (input.getBoundVariables().contains(variable)) {
            return 0.9;
        } else {
            return countTuples(input, constraint.getSupplierKey()) + 1.0;
        }
    }

    protected double _calculateCost(final ExportedParameter exportedParam, final IConstraintEvaluationContext input) {
        return 0.0;
    }

    protected double _calculateCost(final TypeFilterConstraint exportedParam,
            final IConstraintEvaluationContext input) {
        return 0.0;
    }

    protected double _calculateCost(final PositivePatternCall patternCall, final IConstraintEvaluationContext input) {
        // TODO distinguish branching factor and evaluation cost
        TupleMask projMask = computeCallMask(patternCall, input);
        return bucketSize(patternCall, input, projMask).orElse(DEFAULT_COST);        
    }
    
    /**
     * @since 2.1
     */
    protected TupleMask computeCallMask(final IQueryReference patternCall, final IConstraintEvaluationContext input) {
        final List<Integer> boundPositions = new ArrayList<>();
        final List<PParameter> parameters = patternCall.getReferredQuery().getParameters();
        for (int i = 0; (i < parameters.size()); i++) {
            final PVariable variable = (PVariable) patternCall.getActualParametersTuple().get(i);
            if (input.getBoundVariables().contains(variable)) boundPositions.add(i);
        }
        TupleMask projMask = TupleMask.fromSelectedIndices(parameters.size(), boundPositions);
        return projMask;
    }


    /**
     * @since 1.7
     */
    protected double _calculateCost(final ExpressionEvaluation evaluation, final IConstraintEvaluationContext input) {
        return _calculateCost((PConstraint)evaluation, input);
    }
    
    /**
     * @since 1.7
     */
    protected double _calculateCost(final Inequality inequality, final IConstraintEvaluationContext input) {
        return _calculateCost((PConstraint)inequality, input);
    }
    
    /**
     * @since 1.7
     */
    protected double _calculateCost(final AggregatorConstraint aggregator, final IConstraintEvaluationContext input) {
        // TODO distinguish branching factor and evaluation cost
        TupleMask projMask = computeCallMask(aggregator, input);
        return bucketSize(aggregator, input, projMask).orElse(DEFAULT_COST);        
    }
    
    /**
     * @since 1.7
     */
    protected double _calculateCost(final NegativePatternCall call, final IConstraintEvaluationContext input) {
        // TODO distinguish branching factor and evaluation cost
        TupleMask projMask = computeCallMask(call, input);
        return bucketSize(call, input, projMask).orElse(DEFAULT_COST);        
    }
    
    /**
     * @since 1.7
     */
    protected double _calculateCost(final PatternMatchCounter counter, final IConstraintEvaluationContext input) {
        return _calculateCost((PConstraint)counter, input);
    }
    
    /**
     * @since 1.7
     */
    protected double _calculateCost(final BinaryTransitiveClosure closure, final IConstraintEvaluationContext input) {
        return doCalculate(closure, input);
    }


    /**
     * @since 2.0
     */
    protected double _calculateCost(final BinaryReflexiveTransitiveClosure closure, final IConstraintEvaluationContext input) {
        return doCalculate(closure, input);
    }
    
    /**
     * @since 2.1
     */
    protected double doCalculate(final AbstractTransitiveClosure closure, final IConstraintEvaluationContext input) {
        // TODO distinguish branching factor and evaluation cost
        TupleMask projMask = computeCallMask(closure, input);
        Optional<Double> edgeCount = bucketSize(closure, input, TupleMask.empty(2));
        Optional<Long> srcCount = projectionSize(closure, input, TupleMask.selectSingle(0, 2));
        Optional<Long> trgCount = projectionSize(closure, input, TupleMask.selectSingle(1, 2));
        Optional<Double> estimate = Optional.empty();
        // TODO use src/target type or projection size
        estimate = min(estimate, 
                edgeCount.map((edges) -> edges*edges));
        estimate = min(estimate, 
                transitiveClosureDefaultDepth().flatMap((depth) -> 
                    srcCount.flatMap((sources) -> 
                        edgeCount.map((edges) -> 
                                ((double)edges)*sources*depth))));
        estimate = min(estimate, 
                transitiveClosureDefaultDepth().flatMap((depth) -> 
                    trgCount.flatMap((targets) -> 
                        edgeCount.map((edges) -> 
                                ((double)edges)*targets*depth))));
        if (0 != projMask.getSize()) {
            estimate = min(estimate, 
                    transitiveClosureDefaultDepth().flatMap((depth) -> 
                        edgeCount.map((edges) -> 
                                ((double)edges)*depth)));
        }
        return estimate.orElse(StatisticsBasedConstraintCostFunction.DEFAULT_COST);
    }
    
    /**
     * Default cost calculation strategy
     */
    protected double _calculateCost(final PConstraint constraint, final IConstraintEvaluationContext input) {
        if (input.getFreeVariables().isEmpty()) {
            return 1.0;
        } else {
            return StatisticsBasedConstraintCostFunction.DEFAULT_COST;
        }
    }

    /**
     * @throws ViatraQueryRuntimeException
     */
    public double calculateCost(final PConstraint constraint, final IConstraintEvaluationContext input) {
        Preconditions.checkArgument(constraint != null, "Set constraint value correctly");
        if (constraint instanceof ExportedParameter) {
            return _calculateCost((ExportedParameter) constraint, input);
        } else if (constraint instanceof TypeFilterConstraint) {
            return _calculateCost((TypeFilterConstraint) constraint, input);
        } else if (constraint instanceof ConstantValue) {
            return _calculateCost((ConstantValue) constraint, input);
        } else if (constraint instanceof PositivePatternCall) {
            return _calculateCost((PositivePatternCall) constraint, input);
        } else if (constraint instanceof TypeConstraint) {
            return _calculateCost((TypeConstraint) constraint, input);
        } else if (constraint instanceof ExpressionEvaluation) {
            return _calculateCost((ExpressionEvaluation) constraint, input);
        } else if (constraint instanceof Inequality) {
            return _calculateCost((Inequality) constraint, input);
        } else if (constraint instanceof AggregatorConstraint) {
            return _calculateCost((AggregatorConstraint) constraint, input);
        } else if (constraint instanceof NegativePatternCall) {
            return _calculateCost((NegativePatternCall) constraint, input);
        } else if (constraint instanceof PatternMatchCounter) {
            return _calculateCost((PatternMatchCounter) constraint, input);
        } else if (constraint instanceof BinaryTransitiveClosure) {
            return _calculateCost((BinaryTransitiveClosure) constraint, input);
        } else if (constraint instanceof BinaryReflexiveTransitiveClosure) {
            return _calculateCost((BinaryReflexiveTransitiveClosure) constraint, input);
        } else {
            // Default cost calculation
            return _calculateCost(constraint, input);
        }
    }
}
