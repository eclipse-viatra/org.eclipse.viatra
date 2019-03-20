/*******************************************************************************
 * Copyright (c) 2010-2014, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.localsearch.matcher;

import java.util.Collections;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.viatra.query.runtime.base.api.IndexingLevel;
import org.eclipse.viatra.query.runtime.base.api.NavigationHelper;
import org.eclipse.viatra.query.runtime.localsearch.matcher.integration.IAdornmentProvider;
import org.eclipse.viatra.query.runtime.matchers.ViatraQueryRuntimeException;
import org.eclipse.viatra.query.runtime.matchers.backend.IQueryResultProvider;
import org.eclipse.viatra.query.runtime.matchers.backend.ResultProviderRequestor;
import org.eclipse.viatra.query.runtime.matchers.context.IQueryBackendContext;
import org.eclipse.viatra.query.runtime.matchers.context.IQueryRuntimeContext;
import org.eclipse.viatra.query.runtime.matchers.util.ICache;
import org.eclipse.viatra.query.runtime.matchers.util.IProvider;

/**
 * The {@link ISearchContext} interface allows search operations to reuse platform services such as the indexer.
 * 
 * @author Zoltan Ujhelyi
 * @noreference This interface is not intended to be referenced by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 *
 */
public interface ISearchContext {
    
    /**
     * Provides access to the generic query runtime context of the current engine
     * @since 1.7
     */
    IQueryRuntimeContext getRuntimeContext();
    
    /**
     * @param classes
     * @param dataTypes
     * @param features
     */
    void registerObservedTypes(Set<EClass> classes, Set<EDataType> dataTypes, Set<EStructuralFeature> features);
    
    /**
     * Returns a matcher for a selected query specification.
     * 
     * @throws ViatraQueryRuntimeException
     * @since 1.5
     */
    IQueryResultProvider getMatcher(CallWithAdornment dependency);
    
    /**
     * Allows search operations to cache values through the entire lifecycle of the local search backend. The values are
     * calculated if not cached before using the given provider, or returned from the cache accordingly.
     * 
     * @since 1.7
     */
    <T> T accessBackendLevelCache(Object key, Class<? extends T> clazz, IProvider<T> valueProvider);
    
    /**
     * Returns the engine-specific logger
     * 
     * @since 2.0
     */
    Logger getLogger();
    
    /**
     * @noreference This class is not intended to be referenced by clients.
     * @noimplement This interface is not intended to be implemented by clients.
     * @noextend This interface is not intended to be extended by clients.
     */
    public class SearchContext implements ISearchContext {

        private final NavigationHelper navigationHelper;
        private final IQueryRuntimeContext runtimeContext;
        
        private final ICache backendLevelCache;
        private final Logger logger;
        private final ResultProviderRequestor resultProviderRequestor;
        
        /**
         * Initializes a search context using an arbitrary backend context
         */
        public SearchContext(IQueryBackendContext backendContext, ICache backendLevelCache, 
                ResultProviderRequestor resultProviderRequestor) {
            this.resultProviderRequestor = resultProviderRequestor;
            this.runtimeContext = backendContext.getRuntimeContext();
            this.logger = backendContext.getLogger();
            this.navigationHelper = null;
            
            this.backendLevelCache = backendLevelCache;
        }

        public void registerObservedTypes(Set<EClass> classes, Set<EDataType> dataTypes, Set<EStructuralFeature> features) {
            if (this.navigationHelper.isInWildcardMode()) {
                // In wildcard mode, everything is registered (+ register throws an exception)
                return;
            }
            this.navigationHelper.registerObservedTypes(classes, dataTypes, features, IndexingLevel.FULL);
        }
        
        /**
         * @throws ViatraQueryRuntimeException
         * @since 2.1
         */
        @Override
        public IQueryResultProvider getMatcher(CallWithAdornment dependency) {
            // Inject adornment for referenced pattern
            IAdornmentProvider adornmentProvider = query -> {
                if (query.equals(dependency.getReferredQuery())){
                    return Collections.singleton(dependency.getAdornment());
                }
                return Collections.emptySet();
            };
            return resultProviderRequestor.requestResultProvider(dependency.getCall(), 
                    IAdornmentProvider.toHint(adornmentProvider));
        }

        @Override
        public <T> T accessBackendLevelCache(Object key, Class<? extends T> clazz, IProvider<T> valueProvider) {
            return backendLevelCache.getValue(key, clazz, valueProvider);
        }

        public IQueryRuntimeContext getRuntimeContext() {
            return runtimeContext;
        }
        
        @Override
        public Logger getLogger() {
            return logger;
        }
        
    }
}
