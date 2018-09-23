/*******************************************************************************
 * Copyright (c) 2010-2014, Balint Lorand, Zoltan Ujhelyi, Abel Hegedus, Tamas Szabo, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Zoltan Ujhelyi, Abel Hegedus, Tamas Szabo - original initial API and implementation
 *   Balint Lorand - revised API and implementation
 *******************************************************************************/

package org.eclipse.viatra.addon.validation.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.viatra.addon.validation.core.api.ConstraintSpecification;
import org.eclipse.viatra.addon.validation.core.api.IConstraint;
import org.eclipse.viatra.addon.validation.core.api.IConstraintSpecification;
import org.eclipse.viatra.addon.validation.core.api.IValidationEngine;
import org.eclipse.viatra.addon.validation.core.listeners.ValidationEngineListener;
import org.eclipse.viatra.query.runtime.api.IPatternMatch;
import org.eclipse.viatra.query.runtime.api.IQuerySpecification;
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngine;
import org.eclipse.viatra.query.runtime.api.ViatraQueryMatcher;
import org.eclipse.viatra.query.runtime.matchers.util.Preconditions;
import org.eclipse.viatra.transformation.evm.api.ExecutionSchema;
import org.eclipse.viatra.transformation.evm.api.Job;
import org.eclipse.viatra.transformation.evm.api.RuleSpecification;
import org.eclipse.viatra.transformation.evm.api.Scheduler.ISchedulerFactory;
import org.eclipse.viatra.transformation.evm.specific.ExecutionSchemas;
import org.eclipse.viatra.transformation.evm.specific.Jobs;
import org.eclipse.viatra.transformation.evm.specific.Lifecycles;
import org.eclipse.viatra.transformation.evm.specific.Rules;
import org.eclipse.viatra.transformation.evm.specific.Schedulers;
import org.eclipse.viatra.transformation.evm.specific.crud.CRUDActivationStateEnum;

/**
 * This class uses an {@link ViatraQueryEngine} for tracking violations of {@link ConstraintSpecification}s.
 * 
 * Use the {@link #builder()} method for setting up a new instance through the {@link ValidationEngineBuilder}. 
 * 
 * @author Abel Hegedus
 *
 */
public class ValidationEngine implements IValidationEngine {

    private Logger logger;

    private ViatraQueryEngine queryEngine;

    protected ViatraQueryEngine getQueryEngine() {
        return queryEngine;
    }

    private ExecutionSchema executionSchema;

    protected ExecutionSchema getExecutionSchema() {
        return executionSchema;
    }
    
    public static ValidationEngineBuilder builder() {
        return ValidationEngineBuilder.create();
    }
    
    protected ValidationEngine(ViatraQueryEngine engine, Logger logger) {
        Preconditions.checkArgument(engine != null, "Engine cannot be null");
        Preconditions.checkArgument(logger != null, "Logger cannot be null");
        this.queryEngine = engine;
        this.logger = logger;
        
        this.constraintMap = new HashMap<IConstraintSpecification, Constraint>();
        this.listeners = new HashSet<ValidationEngineListener>();
        ISchedulerFactory schedulerFactory = Schedulers.getQueryEngineSchedulerFactory(queryEngine);
        this.executionSchema = ExecutionSchemas.createViatraQueryExecutionSchema(queryEngine, schedulerFactory);
    }

    @Override
    public void initialize() {
        executionSchema.startUnscheduledExecution();
    }

    @Override
    public void dispose() {
        executionSchema.dispose();
        constraintMap.clear();
        listeners.clear();
    }

    private Map<IConstraintSpecification, Constraint> constraintMap;

    @Override
    public Set<IConstraint> getConstraints() {
        return Collections.unmodifiableSet(new HashSet<>(constraintMap.values()));
    }

    @Override
    public IConstraint addConstraintSpecification(IConstraintSpecification constraintSpecification) {
        Constraint constraint = new Constraint(constraintSpecification, this, logger);
        if (constraintMap.put(constraintSpecification, constraint) == null) {
            notifyListenersConstraintRegistered(constraint);
        }
        return constraint;
    }

    @Override
    public IConstraint removeConstraintSpecification(IConstraintSpecification constraintSpecification) {
        Constraint constraint = constraintMap.get(constraintSpecification);
        removeRuleSpecificationFromExecutionSchema(constraint);
        if (constraintMap.remove(constraintSpecification) != null) {
            notifyListenersConstraintDeregistered(constraint);
        }
        return constraint;
    }

    @SuppressWarnings("unchecked")
    protected boolean addRuleSpecificationToExecutionSchema(Constraint constraint) {
        Set<Job<IPatternMatch>> jobs = new HashSet<>();
        jobs.add(Jobs.newErrorLoggingJob(
                Jobs.newStatelessJob(CRUDActivationStateEnum.CREATED, new MatchAppearanceJob(constraint, logger))));
        jobs.add(Jobs.newErrorLoggingJob(
                Jobs.newStatelessJob(CRUDActivationStateEnum.DELETED, new MatchDisappearanceJob(constraint, logger))));
        jobs.add(Jobs.newErrorLoggingJob(
                Jobs.newStatelessJob(CRUDActivationStateEnum.UPDATED, new MatchUpdateJob(constraint, logger))));
        IQuerySpecification<? extends ViatraQueryMatcher<IPatternMatch>> querySpecification = (IQuerySpecification<? extends ViatraQueryMatcher<IPatternMatch>>) constraint
                .getSpecification().getQuerySpecification();
        RuleSpecification<IPatternMatch> rule = Rules.newMatcherRuleSpecification(querySpecification,
                Lifecycles.getDefault(true, true), jobs);
        constraint.setRuleSpecification(rule);
        boolean added = executionSchema.addRule(rule);
        executionSchema.startUnscheduledExecution();
        return added;
    }

    protected boolean removeRuleSpecificationFromExecutionSchema(Constraint constraint) {
        RuleSpecification<IPatternMatch> ruleSpecification = constraint.getRuleSpecification();
        if (ruleSpecification != null) {
            constraint.setRuleSpecification(null);
            return executionSchema.removeRule(ruleSpecification);
        }
        return false;
    }

    private Set<ValidationEngineListener> listeners;

    @Override
    public Set<ValidationEngineListener> getListeners() {
        return Collections.unmodifiableSet(new HashSet<>(listeners));
    }

    @Override
    public boolean addListener(ValidationEngineListener listener) {
        return listeners.add(listener);
    }

    @Override
    public boolean removeListener(ValidationEngineListener listener) {
        return listeners.remove(listener);
    }

    protected void notifyListenersConstraintRegistered(Constraint constraint) {
        for (ValidationEngineListener listener : listeners) {
            listener.constraintRegistered(constraint);
        }
    }

    protected void notifyListenersConstraintDeregistered(Constraint constraint) {
        for (ValidationEngineListener listener : listeners) {
            listener.constraintDeregistered(constraint);
        }
    }

}
