/*******************************************************************************
 * Copyright (c) 2010-2017, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Zoltan Ujhelyi - initial API and implementation
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.localsearch.matcher.integration;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.viatra.query.runtime.exception.ViatraQueryException;
import org.eclipse.viatra.query.runtime.localsearch.matcher.ISearchContext;
import org.eclipse.viatra.query.runtime.localsearch.matcher.LocalSearchMatcher;
import org.eclipse.viatra.query.runtime.localsearch.matcher.MatcherReference;
import org.eclipse.viatra.query.runtime.localsearch.plan.IPlanDescriptor;
import org.eclipse.viatra.query.runtime.localsearch.plan.IPlanProvider;
import org.eclipse.viatra.query.runtime.localsearch.plan.SearchPlan;
import org.eclipse.viatra.query.runtime.localsearch.plan.SearchPlanExecutor;
import org.eclipse.viatra.query.runtime.localsearch.planner.compiler.IOperationCompiler;
import org.eclipse.viatra.query.runtime.localsearch.planner.util.SearchPlanForBody;
import org.eclipse.viatra.query.runtime.matchers.ViatraQueryRuntimeException;
import org.eclipse.viatra.query.runtime.matchers.backend.IMatcherCapability;
import org.eclipse.viatra.query.runtime.matchers.backend.IQueryBackend;
import org.eclipse.viatra.query.runtime.matchers.backend.IQueryResultProvider;
import org.eclipse.viatra.query.runtime.matchers.backend.IUpdateable;
import org.eclipse.viatra.query.runtime.matchers.backend.QueryEvaluationHint;
import org.eclipse.viatra.query.runtime.matchers.backend.QueryHintOption;
import org.eclipse.viatra.query.runtime.matchers.context.IInputKey;
import org.eclipse.viatra.query.runtime.matchers.context.IQueryBackendContext;
import org.eclipse.viatra.query.runtime.matchers.context.IQueryRuntimeContext;
import org.eclipse.viatra.query.runtime.matchers.context.IndexingService;
import org.eclipse.viatra.query.runtime.matchers.planning.QueryProcessingException;
import org.eclipse.viatra.query.runtime.matchers.psystem.PBody;
import org.eclipse.viatra.query.runtime.matchers.psystem.PConstraint;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.PositivePatternCall;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.TypeConstraint;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PParameter;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PQuery;
import org.eclipse.viatra.query.runtime.matchers.psystem.rewriters.IFlattenCallPredicate;
import org.eclipse.viatra.query.runtime.matchers.tuple.ITuple;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;
import org.eclipse.viatra.query.runtime.matchers.tuple.TupleMask;

/**
 * @author Zoltan Ujhelyi
 * @since 1.7
 *
 */
public abstract class AbstractLocalSearchResultProvider implements IQueryResultProvider {

    protected final LocalSearchBackend backend;
    protected final IQueryBackendContext backendContext;
    protected final IQueryRuntimeContext runtimeContext;
    protected final PQuery query;
    protected final QueryEvaluationHint userHints;
    protected final Map<PQuery, LocalSearchHints> hintCache = new HashMap<>();
    protected final IPlanProvider planProvider;
    private static final String PLAN_CACHE_KEY = AbstractLocalSearchResultProvider.class.getName() + "#planCache"; 
    private final Map<MatcherReference, IPlanDescriptor> planCache;
    protected final ISearchContext searchContext;

    /**
     * @since 1.5
     */
    @SuppressWarnings({ "unchecked"})
    public AbstractLocalSearchResultProvider(LocalSearchBackend backend, IQueryBackendContext context, PQuery query,
            IPlanProvider planProvider, QueryEvaluationHint userHints) {
        this.backend = backend;
        this.backendContext = context;
        this.query = query;

        this.planProvider = planProvider;
        this.userHints = userHints;
        this.runtimeContext = context.getRuntimeContext();
        this.searchContext = new ISearchContext.SearchContext(backendContext, userHints, backend.getCache());
        this.planCache = backend.getCache().getValue(PLAN_CACHE_KEY, Map.class, HashMap::new);
    }
    
    protected abstract IOperationCompiler getOperationCompiler(IQueryBackendContext backendContext, LocalSearchHints configuration);
    
    private IQueryRuntimeContext getRuntimeContext() {
        return backend.getRuntimeContext();
    }

    private LocalSearchMatcher createMatcher(IPlanDescriptor plan, final ISearchContext searchContext) {
        Collection<SearchPlanExecutor> executors = StreamSupport.stream(plan.getPlan().spliterator(), false)
                .map(input -> {
                    final SearchPlan plan1 = new SearchPlan();
                    plan1.addOperations(input.getCompiledOperations());

                    return new SearchPlanExecutor(plan1, searchContext, input.getVariableKeys(),
                            input.calculateParameterMask());
                }).collect(Collectors.toList());
        return new LocalSearchMatcher(plan, executors);
    }

    private IPlanDescriptor getOrCreatePlan(MatcherReference key, IQueryBackendContext backendContext, IOperationCompiler compiler, LocalSearchHints configuration, IPlanProvider planProvider) {
        if (planCache.containsKey(key)){
            return planCache.get(key);
        } else {
            IPlanDescriptor plan = planProvider.getPlan(backendContext, compiler, configuration, key);
            planCache.put(key, plan);
            return plan;
        }
    }
    
    private IPlanDescriptor getOrCreatePlan(MatcherReference key, IPlanProvider planProvider) {
        if (planCache.containsKey(key)){
            return planCache.get(key);
        } else {
            LocalSearchHints configuration = overrideDefaultHints(key.getQuery());
            IOperationCompiler compiler = getOperationCompiler(backendContext, configuration);
            IPlanDescriptor plan = planProvider.getPlan(backendContext, compiler, configuration, key);
            planCache.put(key, plan);
            return plan;
        }
    }
    
    private LocalSearchHints overrideDefaultHints(PQuery pQuery) {
        if (hintCache.containsKey(pQuery)) {
            return hintCache.get(pQuery);
        } else {
            LocalSearchHints hint = LocalSearchHints.getDefaultOverriddenBy(
                    computeOverridingHints(pQuery));
            hintCache.put(pQuery, hint);
            return hint;
        }
    }

    /** 
     * Combine with {@link QueryHintOption#getValueOrDefault(QueryEvaluationHint)} to access 
     * hint settings not covered by {@link LocalSearchHints} 
     */
    private QueryEvaluationHint computeOverridingHints(PQuery pQuery) {
        return backendContext.getHintProvider().getQueryEvaluationHint(pQuery).overrideBy(userHints);
    }

    private Stream<MatcherReference> computeExpectedAdornments() {
        return StreamSupport.stream(overrideDefaultHints(query).getAdornmentProvider().getAdornments(query).spliterator(), false)
                .map(input -> new MatcherReference(query, input, userHints));
    }

    /**
     * Prepare this result provider. This phase is separated from the constructor to allow the backend to cache its instance before
     * requesting preparation for its dependencies.
     * @since 1.5
     */
    public void prepare() {
        try {
            runtimeContext.coalesceTraversals(new Callable<Void>() {
    
                @Override
                public Void call() throws Exception {
                    indexInitializationBeforePlanning();
                    prepareDirectDependencies();
                    runtimeContext.executeAfterTraversal(AbstractLocalSearchResultProvider.this::preparePlansForExpectedAdornments);
                    return null;
                }
            });
        } catch (InvocationTargetException e) {
            throw new QueryProcessingException("Error while building required indexes: %s", new String[]{e.getTargetException().getMessage()}, "Error while building required indexes.", query, e);
        }
    }

    protected void preparePlansForExpectedAdornments() {
        // Plan for possible adornments
        computeExpectedAdornments().forEach(reference -> {
            LocalSearchHints configuration = overrideDefaultHints(query);
            IOperationCompiler compiler = getOperationCompiler(backendContext, configuration);
            IPlanDescriptor plan = getOrCreatePlan(reference, backendContext, compiler, configuration, planProvider); 
            // Index keys
            try {
                indexKeys(plan.getIteratedKeys());
            } catch (InvocationTargetException e) {
                throw new QueryProcessingException(e.getMessage(), null, e.getMessage(), query, e);
            }
            //Prepare dependencies
            for(SearchPlanForBody body: plan.getPlan()){
                for(MatcherReference dependency : body.getDependencies()){
                    searchContext.getMatcher(dependency);
                }
            }
            
        });
        
    
    }

    protected void prepareDirectDependencies() {
        // Do not prepare for any adornment at this point
        IAdornmentProvider adornmentProvider = query -> Collections.emptySet();
        QueryEvaluationHint hints = new QueryEvaluationHint(Collections.singletonMap(LocalSearchHintOptions.ADORNMENT_PROVIDER, adornmentProvider), null);
        for(PQuery dep : getDirectPositiveDependencies()){
            backendContext.getResultProviderAccess().getResultProvider(dep, hints);
        }
    }

    /**
     * This method is called before planning start to allow indexing. It is important to note that this method is called
     * inside a coalesceTraversals block, meaning (1) it is safe to add multiple registration requests as necessary, but
     * (2) no value or statistics is available from the index.
     * 
     * @throws ViatraQueryRuntimeException
     */
    protected void indexInitializationBeforePlanning() {
        // By default, no indexing is necessary
    }
    
    /**
     * Collects and indexes all types _directly_ referred by the PQuery {@link #query}. Types indirect
     * @param requiredIndexingServices
     */
    protected void indexReferredTypesOfQuery(PQuery query, IndexingService requiredIndexingServices) {
        for (PBody body : query.getDisjunctBodies().getBodies()) {
            for (PConstraint constraint : body.getConstraints()) {
                if (constraint instanceof TypeConstraint) {
                    runtimeContext.ensureIndexed(((TypeConstraint) constraint).getSupplierKey(), requiredIndexingServices);
                }
            }
        }
    }
    
    private Set<PQuery> getDirectPositiveDependencies() {
        IFlattenCallPredicate flattenPredicate = overrideDefaultHints(query).getFlattenCallPredicate();
        Queue<PQuery> queue = new LinkedList<>();
        Set<PQuery> visited = new HashSet<>();
        Set<PQuery> result = new HashSet<>();
        queue.add(query);
        
        while(!queue.isEmpty()){
            PQuery next = queue.poll();
            visited.add(next);
            for(PBody body : next.getDisjunctBodies().getBodies()){
                for(PositivePatternCall ppc : body.getConstraintsOfType(PositivePatternCall.class)){
                    PQuery dep = ppc.getSupplierKey();
                    if (flattenPredicate.shouldFlatten(ppc)){
                        if (!visited.contains(dep)){
                            queue.add(dep);
                        }
                    }else{
                        result.add(dep);
                    }
                }
            }
        }
        return result;
    }

    private LocalSearchMatcher initializeMatcher(Object[] parameters) {
        return newLocalSearchMatcher(parameters);
    }
    
    private LocalSearchMatcher initializeMatcher(TupleMask parameterSeedMask) {
        return newLocalSearchMatcher(parameterSeedMask.transformUnique(query.getParameters()));

    }

    
    /**
     * @throws ViatraQueryRuntimeException
     */
    public LocalSearchMatcher newLocalSearchMatcher(ITuple parameters) {
        final Set<PParameter> adornment = new HashSet<>();
        for (int i = 0; i < parameters.getSize(); i++) {
            if (parameters.get(i) != null) {
                adornment.add(query.getParameters().get(i));
            }
        }
        
        return newLocalSearchMatcher(adornment);
    }
    
    /**
     * @throws ViatraQueryRuntimeException
     */
    public LocalSearchMatcher newLocalSearchMatcher(Object[] parameters) {
        final Set<PParameter> adornment = new HashSet<>();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i] != null) {
                adornment.add(query.getParameters().get(i));
            }
        }
    
        return newLocalSearchMatcher(adornment);
    }

    private LocalSearchMatcher newLocalSearchMatcher(final Set<PParameter> adornment) {
        final MatcherReference reference = new MatcherReference(query, adornment, userHints);
        
        IPlanDescriptor plan = getOrCreatePlan(reference, planProvider);
        if (overrideDefaultHints(reference.getQuery()).isUseBase()){
            try {
                indexKeys(plan.getIteratedKeys());
            } catch (InvocationTargetException e) {
                throw new ViatraQueryException("Could not index keys","Could not index keys", e);
            }
        }
        
        LocalSearchMatcher matcher = createMatcher(plan, searchContext);
        matcher.addAdapters(backend.getAdapters());
        return matcher;
    }

    private void indexKeys(final Iterable<IInputKey> keys) throws InvocationTargetException {
        final IQueryRuntimeContext qrc = getRuntimeContext();
        qrc.coalesceTraversals(new Callable<Void>() {
    
            @Override
            public Void call() throws Exception {
                for(IInputKey key : keys){
                    if (key.isEnumerable()) {
                        qrc.ensureIndexed(key, IndexingService.INSTANCES);
                    }
                }
                return null;
            }
        });
    }

    @Override
    public boolean hasMatch(Object[] parameters) {
        final LocalSearchMatcher matcher = initializeMatcher(parameters);
        return matcher.hasMatch(parameters);
    }

    @Override
    public boolean hasMatch(TupleMask parameterSeedMask, ITuple parameters) {
        final LocalSearchMatcher matcher = initializeMatcher(parameterSeedMask);
        return matcher.hasMatch(parameterSeedMask, parameters);
    }

    @Override
    public Tuple getOneArbitraryMatch(Object[] parameters) {
        final LocalSearchMatcher matcher = initializeMatcher(parameters);
        return matcher.getOneArbitraryMatch(parameters);
    }

    @Override
    public Tuple getOneArbitraryMatch(TupleMask parameterSeedMask, ITuple parameters) {
        final LocalSearchMatcher matcher = initializeMatcher(parameterSeedMask);
        return matcher.getOneArbitraryMatch(parameterSeedMask, parameters);
    }

    @Override
    public int countMatches(Object[] parameters) {
        final LocalSearchMatcher matcher = initializeMatcher(parameters);
        return matcher.countMatches(parameters);
    }
    
    @Override
    public int countMatches(TupleMask parameterSeedMask, ITuple parameters) {
        final LocalSearchMatcher matcher = initializeMatcher(parameterSeedMask);
        return matcher.countMatches(parameterSeedMask, parameters);
    }

    @Override
    public Collection<? extends Tuple> getAllMatches(Object[] parameters) {
        final LocalSearchMatcher matcher = initializeMatcher(parameters);
        return matcher.getAllMatches(parameters);
    }
    
    @Override
    public Iterable<? extends Tuple> getAllMatches(TupleMask parameterSeedMask, ITuple parameters) {
        final LocalSearchMatcher matcher = initializeMatcher(parameterSeedMask);
        return matcher.getAllMatches(parameterSeedMask, parameters);
    }

    @Override
    public IQueryBackend getQueryBackend() {
        return backend;
    }

    @Override
    public void addUpdateListener(IUpdateable listener, Object listenerTag, boolean fireNow) {
        // throw new UnsupportedOperationException(UPDATE_LISTENER_NOT_SUPPORTED);
    }

    @Override
    public void removeUpdateListener(Object listenerTag) {
        // throw new UnsupportedOperationException(UPDATE_LISTENER_NOT_SUPPORTED);
    }

    /**
     * @since 1.4
     */
    public IMatcherCapability getCapabilites() {
        LocalSearchHints configuration = overrideDefaultHints(query);
        return configuration;
    }

}