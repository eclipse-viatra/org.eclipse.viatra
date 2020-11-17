/*******************************************************************************
 * Copyright (c) 2010-2015, Csaba Debreceni, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.viatra.transformation.views.core;

import java.util.Collection;
import java.util.UUID;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngine;
import org.eclipse.viatra.query.runtime.base.api.NavigationHelper;
import org.eclipse.viatra.query.runtime.emf.EMFScope;
import org.eclipse.viatra.query.runtime.matchers.ViatraQueryRuntimeException;
import org.eclipse.viatra.query.runtime.util.ViatraQueryLoggingUtil;
import org.eclipse.viatra.transformation.evm.api.ExecutionSchema;
import org.eclipse.viatra.transformation.evm.specific.ExecutionSchemas;
import org.eclipse.viatra.transformation.evm.specific.Schedulers;
import org.eclipse.viatra.transformation.views.traceability.Traceability;
import org.eclipse.viatra.transformation.views.traceability.TraceabilityFactory;

/**
 * This class is responsible for initializing an execution schema and jobs to be executed. It also prepares the
 * {@link ResourceSet}.
 * 
 * @author Csaba Debreceni
 *
 */
public class ViewModelManager {

    private static final String TRACEBILITY_RESOURCE = "org.eclipse.viatra.transformation.views.traceability.TraceabilityResource";

    private ViatraQueryEngine engine;
    private ExecutionSchema executionSchema;
    private Collection<ViewModelRule> rules;

    private Traceability traceability = TraceabilityFactory.eINSTANCE.createTraceability();
    private String traceabilityId = UUID.randomUUID().toString();

    /**
     * Initialize the manager.
     * 
     * @throws ViatraQueryRuntimeException
     */
    public void initialize() {

        prepareBaseNotifier();
        traceability.setId(traceabilityId);

        executionSchema = ExecutionSchemas.createViatraQueryExecutionSchema(engine,
                Schedulers.getQueryEngineSchedulerFactory(engine));
        
        for (ViewModelRule rule : rules) {
            rule.initialize(traceabilityId);
            rule.getTracedSpecification().getMatcher(engine);
            rule.getReferencedSpecification().getMatcher(engine);
            rule.getBaseSpecification().getMatcher(engine);
            rule.createRuleSpecification(executionSchema);
        }

        ViatraQueryLoggingUtil.getLogger(getClass()).info("View model manager initialized");
        executionSchema.startUnscheduledExecution();
        ViatraQueryLoggingUtil.getLogger(getClass()).info("Unscheduled execution started");
    }

    /**
     * Prepare the base notifier. Sets the ResourceSet and adds the traceability Resource.
     * @throws ViatraQueryRuntimeException
     */
    private void prepareBaseNotifier() {

        Collection<? extends Notifier> notifiers = getNotifiers();
        Resource resource = null;

        for(Notifier notifier : notifiers) {
            if(notifier instanceof Resource) {
                Resource r = (Resource) notifier;
                if(checkTraceabilityResource(r)) {
                    resource = r;
                    break;
                }
            }
            if(notifier instanceof ResourceSet) {
                ResourceSet resourceSet = (ResourceSet) notifier;
                for (Resource r : resourceSet.getResources()) {
                    if(checkTraceabilityResource(r)) {
                        resource = r;
                        break;
                    }
                }
            }
            
        }
        if (resource == null) {
            resource = addTraceabilityResource();
        }
        
        resource.getContents().add(traceability);
    }

    private boolean checkTraceabilityResource(Resource r) {
        return r.getURI().toString().equals(getTraceabilityResourceId());
    }
    
    public ViatraQueryEngine getEngine() {
        return engine;
    }

    private Resource addTraceabilityResource() {
        ResourceSet resourceSet = new ResourceSetImpl();
        Resource resource = resourceSet.createResource(URI.createURI(getTraceabilityResourceId()));
        NavigationHelper helper = EMFScope.extractUnderlyingEMFIndex(engine);
        helper.addRoot(resourceSet);
        
        return resource;
    }
    
    public void setEngine(ViatraQueryEngine engine) {
        if (!(engine.getScope() instanceof EMFScope)) {
            ViatraQueryLoggingUtil.getLogger(ViewModelManager.class).error(
                    "Only EMFScope is supported currently for ViatraQueryEngine");
            return;
        }
        this.engine = engine;
    }

    private Collection<? extends Notifier> getNotifiers() {
        return ((EMFScope) engine.getScope()).getScopeRoots();
    }

    public void setRules(Collection<ViewModelRule> rules) {
        this.rules = rules;
    }

    public Traceability getTraceability() {
        return traceability;
    }

    public void dispose() {
        executionSchema.dispose();
        EcoreUtil.delete(traceability);
    }
    
    private String getTraceabilityResourceId() {
        return TRACEBILITY_RESOURCE;
    }
}
