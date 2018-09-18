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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.viatra.query.runtime.localsearch.exceptions.LocalSearchException;
import org.eclipse.viatra.query.runtime.localsearch.matcher.CallWithAdornment;
import org.eclipse.viatra.query.runtime.localsearch.matcher.ISearchContext;
import org.eclipse.viatra.query.runtime.localsearch.matcher.LocalSearchMatcher;
import org.eclipse.viatra.query.runtime.localsearch.matcher.MatcherReference;
import org.eclipse.viatra.query.runtime.localsearch.plan.IPlanDescriptor;
import org.eclipse.viatra.query.runtime.localsearch.plan.IPlanProvider;
import org.eclipse.viatra.query.runtime.localsearch.plan.SearchPlan;
import org.eclipse.viatra.query.runtime.localsearch.plan.SearchPlanForBody;
import org.eclipse.viatra.query.runtime.localsearch.planner.compiler.IOperationCompiler;
import org.eclipse.viatra.query.runtime.matchers.ViatraQueryRuntimeException;
import org.eclipse.viatra.query.runtime.matchers.backend.IMatcherCapability;
import org.eclipse.viatra.query.runtime.matchers.backend.IQueryBackend;
import org.eclipse.viatra.query.runtime.matchers.backend.IQueryResultProvider;
import org.eclipse.viatra.query.runtime.matchers.backend.IUpdateable;
import org.eclipse.viatra.query.runtime.matchers.backend.QueryEvaluationHint;
import org.eclipse.viatra.query.runtime.matchers.backend.QueryHintOption;
import org.eclipse.viatra.query.runtime.matchers.backend.ResultProviderRequestor;
import org.eclipse.viatra.query.runtime.matchers.context.IInputKey;
import org.eclipse.viatra.query.runtime.matchers.context.IQueryBackendContext;
import org.eclipse.viatra.query.runtime.matchers.context.IQueryRuntimeContext;
import org.eclipse.viatra.query.runtime.matchers.context.IndexingService;
import org.eclipse.viatra.query.runtime.matchers.planning.QueryProcessingException;
import org.eclipse.viatra.query.runtime.matchers.psystem.IQueryReference;
import org.eclipse.viatra.query.runtime.matchers.psystem.PBody;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.PositivePatternCall;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PParameter;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PQueries;
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
     * @since 2.1
     */
    protected ResultProviderRequestor resultProviderRequestor;

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
        this.resultProviderRequestor = backend.getResultProviderRequestor(query, userHints);
        this.searchContext = new ISearchContext.SearchContext(backendContext, backend.getCache(), resultProviderRequestor);
        this.planCache = backend.getCache().getValue(PLAN_CACHE_KEY, Map.class, HashMap::new);
    }
    
    protected abstract IOperationCompiler getOperationCompiler(IQueryBackendContext backendContext, LocalSearchHints configuration);
    
    private IQueryRuntimeContext getRuntimeContext() {
        return backend.getRuntimeContext();
    }

    private LocalSearchMatcher createMatcher(IPlanDescriptor plan, final ISearchContext searchContext) {
        List<SearchPlan> executors = plan.getPlan().stream()
                .map(input -> new SearchPlan(input.getBody(), input.getCompiledOperations(), input.calculateParameterMask(),
                        input.getVariableKeys()))
                .collect(Collectors.toList());
        return new LocalSearchMatcher(searchContext, plan, executors);
    }

    private IPlanDescriptor getOrCreatePlan(MatcherReference key, IQueryBackendContext backendContext, IOperationCompiler compiler, LocalSearchHints configuration, IPlanProvider planProvider) {
        if (planCache.containsKey(key)){
            return planCache.get(key);
        } else {
            IPlanDescriptor plan = planProvider.getPlan(backendContext, compiler, 
                    resultProviderRequestor, configuration, key);
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
            IPlanDescriptor plan = planProvider.getPlan(backendContext, compiler, 
                    resultProviderRequestor, configuration, key);
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

    /**
     * Prepare this result provider. This phase is separated from the constructor to allow the backend to cache its instance before
     * requesting preparation for its dependencies.
     * @since 1.5
     */
    public void prepare() {
        try {
            runtimeContext.coalesceTraversals(() -> {
                indexInitializationBeforePlanning();
                prepareDirectDependencies();
                runtimeContext.executeAfterTraversal(AbstractLocalSearchResultProvider.this::preparePlansForExpectedAdornments);
                return null;
            });
        } catch (InvocationTargetException e) {
            throw new QueryProcessingException("Error while building required indexes: {1}", new String[]{e.getTargetException().getMessage()}, "Error while building required indexes.", query, e);
        }
    }

    protected void preparePlansForExpectedAdornments() {
        // Plan for possible adornments
        for (Set<PParameter> adornment : overrideDefaultHints(query).getAdornmentProvider().getAdornments(query)) {
            MatcherReference reference = new MatcherReference(query, adornment, userHints);
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
                for(CallWithAdornment dependency : body.getDependencies()){
                    searchContext.getMatcher(dependency);
                }
            }
        }
    }

    protected void prepareDirectDependencies() {
        // Do not prepare for any adornment at this point
        IAdornmentProvider adornmentProvider = input -> Collections.emptySet();
        QueryEvaluationHint adornmentHint = IAdornmentProvider.toHint(adornmentProvider);

        for(IQueryReference call : getDirectDependencies()){
            resultProviderRequestor.requestResultProvider(call, adornmentHint);
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
        PQueries.directlyRequiredTypesOfQuery(query, true /*only enumerables are considered for indexing */).forEach(
                inputKey -> runtimeContext.ensureIndexed(inputKey, requiredIndexingServices)
        );
    }
    
    private Set<IQueryReference> getDirectDependencies() {
        IFlattenCallPredicate flattenPredicate = overrideDefaultHints(query).getFlattenCallPredicate();
        Queue<PQuery> queue = new LinkedList<>();
        Set<PQuery> visited = new HashSet<>();
        Set<IQueryReference> result = new HashSet<>();
        queue.add(query);
        
        while(!queue.isEmpty()){
            PQuery next = queue.poll();
            visited.add(next);
            for(PBody body : next.getDisjunctBodies().getBodies()){
                for (IQueryReference call : body.getConstraintsOfType(IQueryReference.class)) {
                    if (call instanceof PositivePatternCall && 
                            flattenPredicate.shouldFlatten((PositivePatternCall) call)) 
                    {
                        PQuery dep = ((PositivePatternCall) call).getReferredQuery();
                        if (!visited.contains(dep)){
                            queue.add(dep);
                        }
                    } else {
                        result.add(call);
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
                throw new LocalSearchException("Could not index keys", e);
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
        return matcher.streamMatches(parameters).findAny().isPresent();
    }

    @Override
    public boolean hasMatch(TupleMask parameterSeedMask, ITuple parameters) {
        final LocalSearchMatcher matcher = initializeMatcher(parameterSeedMask);
        return matcher.streamMatches(parameterSeedMask, parameters).findAny().isPresent();
    }

    @Override
    public Optional<Tuple> getOneArbitraryMatch(Object[] parameters) {
        final LocalSearchMatcher matcher = initializeMatcher(parameters);
        return matcher.streamMatches(parameters).findAny();
    }

    @Override
    public Optional<Tuple> getOneArbitraryMatch(TupleMask parameterSeedMask, ITuple parameters) {
        final LocalSearchMatcher matcher = initializeMatcher(parameterSeedMask);
        return matcher.streamMatches(parameterSeedMask, parameters).findAny();
    }

    @Override
    public int countMatches(Object[] parameters) {
        final LocalSearchMatcher matcher = initializeMatcher(parameters);
        // Count returns long; casting to int - in case of integer overflow casting will throw the exception
        return (int) matcher.streamMatches(parameters).count();
    }
    
    @Override
    public int countMatches(TupleMask parameterSeedMask, ITuple parameters) {
        final LocalSearchMatcher matcher = initializeMatcher(parameterSeedMask);
        // Count returns long; casting to int - in case of integer overflow casting will throw the exception
        return (int) matcher.streamMatches(parameterSeedMask, parameters).count();
    }

    @Override
    public Stream<Tuple> getAllMatches(Object[] parameters) {
        final LocalSearchMatcher matcher = initializeMatcher(parameters);
        return matcher.streamMatches(parameters);
    }
    
    @Override
    public Stream<Tuple> getAllMatches(TupleMask parameterSeedMask, ITuple parameters) {
        final LocalSearchMatcher matcher = initializeMatcher(parameterSeedMask);
        return matcher.streamMatches(parameterSeedMask, parameters);
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
    
    /**
     * Forgets all stored plans in this result provider. If no plans are stored, nothing happens.
     * 
     * @since 2.0
     * @noreference This method is not intended to be referenced by clients; it should only used by {@link LocalSearchBackend}.
     */
    public void forgetAllPlans() {
        planCache.clear();
    }
    
    /**
     * Returns a search plan for a given adornment if exists
     * 
     * @return a search plan for the pattern with the given adornment, or null if none exists
     * @since 2.0
     * @noreference This method is not intended to be referenced by clients; it should only used by {@link LocalSearchBackend}.
     */
    public IPlanDescriptor getSearchPlan(Set<PParameter> adornment) {
        return planCache.get(new MatcherReference(query, adornment));
    }
}