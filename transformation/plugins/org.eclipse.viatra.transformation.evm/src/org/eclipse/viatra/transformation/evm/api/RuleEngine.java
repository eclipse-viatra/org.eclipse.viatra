/*******************************************************************************
 * Copyright (c) 2010-2013, Abel Hegedus, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Abel Hegedus - initial API and implementation
 *******************************************************************************/
package org.eclipse.viatra.transformation.evm.api;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.viatra.transformation.evm.api.event.ActivationState;
import org.eclipse.viatra.transformation.evm.api.event.EventFilter;
import org.eclipse.viatra.transformation.evm.api.event.EventRealm;
import org.eclipse.viatra.transformation.evm.api.resolver.ConflictResolver;
import org.eclipse.viatra.transformation.evm.api.resolver.ScopedConflictSet;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

/**
 * A rule engine is a facade for accessing the EVM, it allows
 * the retrieval of all activations and filtered access by state,
 *  specification or both. It also allows the addition or removal
 *  of rules.   
 * 
 * @author Abel Hegedus
 * 
 */
public class RuleEngine {

    private static final String FILTER_MUST_BE_SPECIFIED = "Filter must be specified";
    private static final String RULE_SPECIFICATION_MUST_BE_SPECIFIED = "Rule specification must be specified!";
    private RuleBase ruleBase;

    /**
     * Creates a rule engine for the given ruleBase
     * 
     * @param ruleBase
     */
    protected RuleEngine(final RuleBase ruleBase) {
        this.ruleBase = Objects.requireNonNull(ruleBase, "Cannot create rule engine with null ruleBase!");
    }

    /**
     * Creates a rule engine for the given ruleBase
     * 
     * @param ruleBase
     * @return the created facade
     */
    public static RuleEngine create(final RuleBase ruleBase) {
        return new RuleEngine(ruleBase);
    }

    public void setConflictResolver(ConflictResolver conflictResolver) {
        Objects.requireNonNull(conflictResolver, "Conflict resolver cannot be null!");
        ruleBase.getAgenda().setConflictResolver(conflictResolver);
    }
    
    public <EventAtom> ScopedConflictSet createScopedConflictSet(RuleSpecification<EventAtom> specification, EventFilter<? super EventAtom> eventFilter) {
        return createScopedConflictSet(ruleBase.getAgenda().getConflictSet().getConflictResolver(), ImmutableMultimap.<RuleSpecification<?>, EventFilter<?>>of(specification, eventFilter));
    }
    
    public ScopedConflictSet createScopedConflictSet(Multimap<RuleSpecification<?>, EventFilter<?>> specifications) {
        return createScopedConflictSet(ruleBase.getAgenda().getConflictSet().getConflictResolver(), specifications);
    }
    
    public ScopedConflictSet createScopedConflictSet(ConflictResolver conflictResolver, Multimap<RuleSpecification<?>, EventFilter<?>> specifications) {
        Objects.requireNonNull(conflictResolver, "Conflict resolver cannot be null!");
        Objects.requireNonNull(specifications, "Specification set cannot be null!");
        ScopedConflictSet scopedConflictSet = ruleBase.createScopedConflictSet(conflictResolver, specifications);
        return scopedConflictSet;
    }
    
    /**
     * Adds a rule specification to the RuleBase.
     *  If the rule already exists, no change occurs in the set of rules.
     *  
     * @param specification
     * @return true if the rule was added, false if it already existed
     */
    public <EventAtom> boolean addRule(
            final RuleSpecification<EventAtom> specification) {
        return addRule(specification, specification.createEmptyFilter());
    }

    /**
     * Adds a rule specification to the RuleBase with the given filter.
     * If the rule already exists, no change occurs in the set of rules.
     * 
     * @param specification
     * @param filter the partial match to be used as a filter for activations
     * @return true if the rule was added, false if it already existed
     */
    public <EventAtom> boolean addRule(
            final RuleSpecification<EventAtom> specification, EventFilter<? super EventAtom> filter) {
        Objects.requireNonNull(filter, FILTER_MUST_BE_SPECIFIED);
        Objects.requireNonNull(specification, RULE_SPECIFICATION_MUST_BE_SPECIFIED);
        RuleInstance<EventAtom> instance = ruleBase.getInstance(specification, filter);
        boolean added = false;
        if(instance == null) {
            ruleBase.instantiateRule(specification, filter);
            added = true;
        }
        return added;
    }

    /**
     * Decides whether a rule specification is already added to the RuleBase
     * without filter.
     */
    public <EventAtom> boolean containsRule(
            final RuleSpecification<EventAtom> specification) {
        return containsRule(specification, specification.createEmptyFilter());
    }

    /**
     * Decides whether a rule specification is already added to the RuleBase
     * with the specified filter.
     */
    public <EventAtom> boolean containsRule(
            final RuleSpecification<EventAtom> specification,
            EventFilter<? super EventAtom> filter) {
        return ruleBase.getRuleSpecificationMultimap().containsEntry(
                specification, filter);
    }
    
    /**
     * 
     * @return a copy of the multimap containing all activations
     */
    public Multimap<ActivationState, Activation<?>> getActivations() {
        return ImmutableMultimap.copyOf(ruleBase.getAgenda().getActivations());
    }

    /**
     * 
     * @return the next enabled activation if exists, selected by the conflict resolver
     */
    public Activation<?> getNextActivation() {
        return ruleBase.getAgenda().getConflictSet().getNextActivation();
    }

    /**
     * 
     * @return an immutable set of conflicting activations
     */
    public Set<Activation<?>> getConflictingActivations() {
        return Collections.unmodifiableSet(new HashSet<>(ruleBase.getAgenda().getConflictSet().getConflictingActivations()));
    }
    
    /**
     * 
     * @param state
     * @return an immutable set of the activations in the given state
     */
    public Set<Activation<?>> getActivations(final ActivationState state) {
        Objects.requireNonNull(state, "Activation state must be specified!");
        return Collections.unmodifiableSet(new HashSet<>(ruleBase.getAgenda().getActivations(state)));
    }

    /**
     * 
     * @param specification
     * @return the immutable set of activations of the given specification
     */
    public <EventAtom> Set<Activation<EventAtom>> getActivations(final RuleSpecification<EventAtom> specification) {
        return getActivations(specification, specification.createEmptyFilter());
    }

    /**
     * 
     * @param specification
     * @param filter
     * @return the immutable set of activations of the given filtered specification
     */
    public <EventAtom> Set<Activation<EventAtom>> getActivations(final RuleSpecification<EventAtom> specification, EventFilter<? super EventAtom> filter) {
        Objects.requireNonNull(specification, RULE_SPECIFICATION_MUST_BE_SPECIFIED);
        Objects.requireNonNull(filter, FILTER_MUST_BE_SPECIFIED);
        return Collections.unmodifiableSet(new HashSet<>(ruleBase.getInstance(specification, filter).getAllActivations()));
    }

    /**
     * 
     * @param specification
     * @param state
     * @return the immutable set of activations of the given specification
     * with the given state
     */
    public <EventAtom> Set<Activation<EventAtom>> getActivations(final RuleSpecification<EventAtom> specification,
            final ActivationState state) {
        return getActivations(specification, specification.createEmptyFilter(), state);
    }

    /**
     * 
     * @param specification 
     * @param filter 
     * @param state
     * @return the immutable set of activations of the given specification
     * with the given state
     */
    public <EventAtom> Set<Activation<EventAtom>> getActivations(
            final RuleSpecification<EventAtom> specification, EventFilter<? super EventAtom> filter, final ActivationState state) {
        Objects.requireNonNull(specification, RULE_SPECIFICATION_MUST_BE_SPECIFIED);
        Objects.requireNonNull(state, "Activation state must be specified!");
        Objects.requireNonNull(filter, FILTER_MUST_BE_SPECIFIED);
        return Collections.unmodifiableSet(new HashSet<>(ruleBase.getInstance(specification, filter).getActivations(state)));
    }
    
    /**
     * @return the immutable set of rules in the EVM
     */
    public Multimap<RuleSpecification<?>, EventFilter<?>> getRuleSpecificationMultimap(){
        return ImmutableMultimap.copyOf(ruleBase.getRuleSpecificationMultimap());
    }

    /**
     * Removes the given not-filtered rule from the EVM.
     * 
     * @param specification
     * @return true, if the rule existed
     */
    public <EventAtom> boolean removeRule(final RuleSpecification<EventAtom> specification) {
        Objects.requireNonNull(specification, RULE_SPECIFICATION_MUST_BE_SPECIFIED);
        return ruleBase.removeRule(specification, specification.createEmptyFilter());
    }

    /**
     * Removes the given filtered rule from the EVM.
     * 
     * @param specification
     * @param filter the partial match used as a filter
     * @return true, if the rule existed
     */
    public <EventAtom> boolean removeRule(
            final RuleSpecification<EventAtom> specification, EventFilter<? super EventAtom> filter) {
        Objects.requireNonNull(specification, RULE_SPECIFICATION_MUST_BE_SPECIFIED);
        Objects.requireNonNull(filter, FILTER_MUST_BE_SPECIFIED);
        return ruleBase.removeRule(specification, filter);
    }

    protected RuleBase getRuleBase() {
        return ruleBase;
    }
    
    /**
     * 
     * @return the event realm of the rule base
     */
    public EventRealm getEventRealm() {
        return ruleBase.getEventRealm();
    }
    
    public Logger getLogger() {
        return ruleBase.getLogger();
    }

    /**
     * Disposes of the rule base.
     */
    public void dispose() {
        ruleBase.dispose();
    }
}
