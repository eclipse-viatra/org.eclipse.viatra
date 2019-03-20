/*******************************************************************************
 * Copyright (c) 2004-2011 Abel Hegedus and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.addon.querybasedfeatures.runtime;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.viatra.addon.querybasedfeatures.runtime.handler.QueryBasedFeatures;
import org.eclipse.viatra.query.runtime.api.IPatternMatch;
import org.eclipse.viatra.query.runtime.api.IQuerySpecification;
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngine;
import org.eclipse.viatra.query.runtime.api.ViatraQueryMatcher;
import org.eclipse.viatra.query.runtime.emf.EMFScope;
import org.eclipse.viatra.query.runtime.exception.ViatraQueryException;
import org.eclipse.viatra.query.runtime.registry.IQuerySpecificationRegistry;
import org.eclipse.viatra.query.runtime.registry.IQuerySpecificationRegistryEntry;
import org.eclipse.viatra.query.runtime.registry.QuerySpecificationRegistry;
import org.eclipse.viatra.query.runtime.util.ViatraQueryLoggingUtil;

/**
 * Utility class for instantiating query-based feature handlers ({@link IQueryBasedFeatureHandler}).
 * 
 * @author Abel Hegedus
 */
public final class QueryBasedFeatureHelper {
    
    // QueryResultMultiMap based implementation:
    // - one Execution Schema for one ViatraQueryEngine
    // - one QueryResultMultiMap for one feature and model pair
    // - handler can be stateless? simply processes updates from multimap
    
    /**
     * Weak hash map for keeping the created
     */
    private static final Map<Notifier, Map<EStructuralFeature, WeakReference<IQueryBasedFeatureHandler>>> FEATURE_MAP = new WeakHashMap<Notifier, Map<EStructuralFeature, WeakReference<IQueryBasedFeatureHandler>>>();

    /**
     * Constructor hidden for static utility class
     */
    private QueryBasedFeatureHelper() {
    }

    /**
     * Decide what {@link Notifier} to use as the scope of the {@link ViatraQueryMatcher} underlying the created
     * {@link IQueryBasedFeatureHandler}.
     * 
     * <p>
     * Optimally, the {@link ResourceSet} is reachable and most other matchers will use it as well.
     * 
     * <p>
     * Otherwise, the {@link Resource} is used if the model is not inside a resource set.
     * 
     * <p>
     * If none of the above are reachable, the container hierarchy is traversed for a top element.
     * 
     * <p>
     * Finally, the source itself is returned.
     * 
     * @param source
     *            the source object that initializes the handler
     * @return the topmost reachable Notifier from the source
     */
    public static Notifier prepareNotifierForSource(EObject source) {
        if (source != null) {
            Resource eResource = source.eResource();
            if (eResource != null) {
                ResourceSet resourceSet = eResource.getResourceSet();
                if (resourceSet != null) {
                    return resourceSet;
                } else {
                    return eResource;
                }
            } else {
                EObject top = source;
                while (top.eContainer() != null) {
                    top = top.eContainer();
                }
                if(!top.equals(source)) {
                    return prepareNotifierForSource(top);
                }
            }
        }
        return source;
    }

    /**
     * Returns the {@link IQueryBasedFeatureHandler} for the given {@link EStructuralFeature} in the given
     * {@link Notifier}. If the handler does not exist yet, it is also initialized, before being returned.
     * 
     * <p>
     * The required matcher is initialized using the pattern fully qualified name passed as a parameter.
     * 
     * @param notifier
     *            the exact notifier to use for the handler initialization
     * @param feature
     *            the feature that is managed by the handler
     * @param patternFQN
     *            the fully qualified name of the pattern used by the handler
     * @param sourceParamName
     *            the name of the parameter in the pattern that represents the source end of the feature
     * @param targetParamName
     *            the name of the parameter in the pattern that represents the target end of the feature
     * @param kind
     *            the {@link QueryBasedFeatureKind} that is used by the handler
     * @param keepCache
     *            specifies whether the handler uses an internal cache for feature values. Only possible with single and
     *            many reference kinds
     * @return the query-based feature handler that manages the feature values
     */
    public static IQueryBasedFeatureHandler getQueryBasedFeatureHandler(Notifier notifier, EStructuralFeature feature,
            String patternFQN, String sourceParamName, String targetParamName, QueryBasedFeatureKind kind,
            boolean keepCache) {

        Map<EStructuralFeature, WeakReference<IQueryBasedFeatureHandler>> features = FEATURE_MAP.get(notifier);
        if (features == null) {
            features = new HashMap<EStructuralFeature, WeakReference<IQueryBasedFeatureHandler>>();
            FEATURE_MAP.put(notifier, features);
        }
        WeakReference<IQueryBasedFeatureHandler> weakReference = features.get(feature);

        IQueryBasedFeatureHandler derivedFeature = weakReference == null ? null : weakReference.get();
        if (derivedFeature != null) {
            return derivedFeature;
        }

        QueryBasedFeature newFeature = createQueryBasedFeature(feature, kind, keepCache);
        if(newFeature == null) {
            ViatraQueryLoggingUtil.getLogger(QueryBasedFeatureHelper.class).error("Handler initialization failed, feature kind " + kind + " not supported!");
            return null;
        }
        
        QueryBasedFeatureHandler queryBasedFeatureHandler = new QueryBasedFeatureHandler(newFeature);
        features.put(feature, new WeakReference<IQueryBasedFeatureHandler>(queryBasedFeatureHandler));

        IQuerySpecificationRegistry registry = QuerySpecificationRegistry.getInstance();
        IQuerySpecificationRegistryEntry registryEntry = registry.getDefaultView().getEntry(patternFQN);
        @SuppressWarnings("unchecked")
        IQuerySpecification<? extends ViatraQueryMatcher<IPatternMatch>> querySpecification = (IQuerySpecification<? extends ViatraQueryMatcher<IPatternMatch>>) registryEntry.get();
        if (querySpecification != null) {
            try {
                ViatraQueryMatcher<IPatternMatch> matcher = querySpecification.getMatcher(ViatraQueryEngine.on(new EMFScope(notifier)));
                newFeature.initialize(matcher, sourceParamName, targetParamName);
                newFeature.startMonitoring();
            } catch (ViatraQueryException e) {
                ViatraQueryLoggingUtil.getLogger(QueryBasedFeatureHelper.class).error("Handler initialization failed", e);
                return null;
            }
        } else {
            ViatraQueryLoggingUtil
                    .getLogger(QueryBasedFeatureHelper.class)
                    .error(String.format("Handler initialization failed, query specification is null for %s. Make sure to include your VIATRA Query project with the query definitions in the configuration.", patternFQN));
        }

        return queryBasedFeatureHandler;
    }

    protected static QueryBasedFeature createQueryBasedFeature(EStructuralFeature feature, QueryBasedFeatureKind kind,
            boolean keepCache) {
        QueryBasedFeature newFeature = null;
        switch(kind) {
            case SINGLE_REFERENCE:
                newFeature = QueryBasedFeatures.newSingleValueFeature(feature, keepCache);
                break;
            case MANY_REFERENCE:
                newFeature = QueryBasedFeatures.newMultiValueFeatue(feature, keepCache);
                break;
            case SUM:
                newFeature = QueryBasedFeatures.newSumFeature(feature);
                break;
            case ITERATION:
                // fall-through
            default:
                break;
        }
        return newFeature;
    }

    /**
     * Returns the {@link IQueryBasedFeatureHandler} for the given {@link EStructuralFeature} in the given
     * {@link Notifier}. If the handler does not exist yet, it is also initialized, before being returned.
     * 
     * <p>
     * The required matcher is initialized using the pattern fully qualified name passed as a parameter.
     * 
     * <p>
     * Calls
     * {@link #getQueryBasedFeatureHandler(Notifier, EStructuralFeature, String, String, String, QueryBasedFeatureKind, boolean)}
     * with keepCache = true.
     * 
     * @param notifier
     *            the exact notifier to use for the handler initialization
     * @param feature
     *            the feature that is managed by the handler
     * @param patternFQN
     *            the fully qualified name of the pattern used by the handler
     * @param sourceParamName
     *            the name of the parameter in the pattern that represents the source end of the feature
     * @param targetParamName
     *            the name of the parameter in the pattern that represents the target end of the feature
     * @param kind
     *            the {@link QueryBasedFeatureKind} that is used by the handler
     * @return the query-based feature handler that manages the feature values
     */
    public static IQueryBasedFeatureHandler getQueryBasedFeatureHandlerOnNotifier(Notifier notifier,
            EStructuralFeature feature, String patternFQN, String sourceParamName, String targetParamName,
            QueryBasedFeatureKind kind) {
        return getQueryBasedFeatureHandler(notifier, feature, patternFQN, sourceParamName, targetParamName, kind, true);
    }

    /**
     * Returns the {@link IQueryBasedFeatureHandler} for the given {@link EStructuralFeature} on the source or the
     * topmost {@link Notifier} reachable from the source. If the handler does not exist yet, it is also initialized,
     * before being returned.
     * 
     * <p>
     * The required matcher is initialized using the pattern fully qualified name passed as a parameter.
     * 
     * <p>
     * Calls
     * {@link #getQueryBasedFeatureHandler(Notifier, EStructuralFeature, String, String, String, QueryBasedFeatureKind, boolean)}.
     * 
     * @param source
     *            the source object used for the handler initialization (used for determining the notifier for the
     *            underlying matcher)
     * @param feature
     *            the feature that is managed by the handler
     * @param patternFQN
     *            the fully qualified name of the pattern used by the handler
     * @param sourceParamName
     *            the name of the parameter in the pattern that represents the source end of the feature
     * @param targetParamName
     *            the name of the parameter in the pattern that represents the target end of the feature
     * @param kind
     *            the {@link QueryBasedFeatureKind} that is used by the handler
     * @param keepCache
     *            specifies whether the handler uses an internal cache for feature values. Only possible with single and
     *            many reference kinds
     * @param useSourceAsNotifier
     *            if true, the source is used as the notifier for the matcher initialization
     * @return the query-based feature handler that manages the feature values
     */
    public static IQueryBasedFeatureHandler getQueryBasedFeatureHandler(EObject source, EStructuralFeature feature,
            String patternFQN, String sourceParamName, String targetParamName, QueryBasedFeatureKind kind,
            boolean keepCache, boolean useSourceAsNotifier) {
        Notifier notifier = source;
        if (!useSourceAsNotifier) {
            notifier = prepareNotifierForSource(source);
        }
        return getQueryBasedFeatureHandler(notifier, feature, patternFQN, sourceParamName, targetParamName, kind,
                keepCache);
    }

    /**
     * Returns the {@link IQueryBasedFeatureHandler} for the given {@link EStructuralFeature} on the topmost
     * {@link Notifier} reachable from the source. If the handler does not exist yet, it is also initialized, before
     * being returned.
     * 
     * <p>
     * The required matcher is initialized using the pattern fully qualified name passed as a parameter.
     * 
     * <p>
     * Calls
     * {@link #getQueryBasedFeatureHandler(EObject, EStructuralFeature, String, String, String, QueryBasedFeatureKind, boolean, boolean)}.
     * 
     * @param source
     *            the source object used for the handler initialization (used for determining the notifier for the
     *            underlying matcher)
     * @param feature
     *            the feature that is managed by the handler
     * @param patternFQN
     *            the fully qualified name of the pattern used by the handler
     * @param sourceParamName
     *            the name of the parameter in the pattern that represents the source end of the feature
     * @param targetParamName
     *            the name of the parameter in the pattern that represents the target end of the feature
     * @param kind
     *            the {@link QueryBasedFeatureKind} that is used by the handler
     * @return the query-based feature handler that manages the feature values
     */
    public static IQueryBasedFeatureHandler getQueryBasedFeatureHandler(EObject source, EStructuralFeature feature,
            String patternFQN, String sourceParamName, String targetParamName, QueryBasedFeatureKind kind) {
        return getQueryBasedFeatureHandler(source, feature, patternFQN, sourceParamName, targetParamName, kind, true,
                false);
    }
}
