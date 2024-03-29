/*******************************************************************************
 * Copyright (c) 2004-2013, Istvan David, Zoltan Ujhelyi and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.transformation.runtime.emf.rules.eventdriven;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.viatra.query.runtime.api.IPatternMatch;
import org.eclipse.viatra.query.runtime.api.IQuerySpecification;
import org.eclipse.viatra.query.runtime.api.ViatraQueryMatcher;
import org.eclipse.viatra.transformation.evm.api.ActivationLifeCycle;
import org.eclipse.viatra.transformation.evm.api.Job;
import org.eclipse.viatra.transformation.evm.api.RuleSpecification;
import org.eclipse.viatra.transformation.evm.api.event.EventFilter;
import org.eclipse.viatra.transformation.evm.specific.Jobs;
import org.eclipse.viatra.transformation.evm.specific.Rules;
import org.eclipse.viatra.transformation.evm.specific.crud.CRUDActivationStateEnum;
import org.eclipse.viatra.transformation.runtime.emf.rules.ITransformationRule;

public class EventDrivenTransformationRule<Match extends IPatternMatch, Matcher extends ViatraQueryMatcher<Match>>
        implements ITransformationRule<Match, Matcher> {
    private final String name;
    private final IQuerySpecification<Matcher> precondition;
    private final RuleSpecification<Match> ruleSpecification;
    private final EventFilter<? super Match> filter;

    /**
     * @since 2.0
     */
    public EventDrivenTransformationRule(String name, IQuerySpecification<Matcher> precondition,
            Map<CRUDActivationStateEnum, ? extends Consumer<Match>> stateActions, ActivationLifeCycle lifeCycle,
            EventFilter<? super Match> filter) {
        this.name = name;
        Set<Job<Match>> jobs = new HashSet<>();
        boolean createdJobAdded = false;
        for (Entry<CRUDActivationStateEnum, ? extends Consumer<Match>> stateAction : stateActions.entrySet()) {
            CRUDActivationStateEnum state = stateAction.getKey();
            Consumer<Match> action = stateAction.getValue();

            jobs.add(Jobs.newStatelessJob(state, action));
            if (state == CRUDActivationStateEnum.CREATED) {
                createdJobAdded = true;
            }
        }
        if (!createdJobAdded) {
            jobs.add(Jobs.newNopJob(CRUDActivationStateEnum.CREATED));
        }
        
        this.precondition = precondition;
        ruleSpecification = Rules.newMatcherRuleSpecification(precondition, lifeCycle, jobs, name);
        this.filter = filter;
    }

    public EventDrivenTransformationRule(EventDrivenTransformationRule<Match, Matcher> rule, EventFilter<? super Match> filter) {
        this.name = rule.name;
        this.precondition = rule.precondition;
        this.ruleSpecification = rule.ruleSpecification;
        this.filter = filter;
    }
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public RuleSpecification<Match> getRuleSpecification() {
        return ruleSpecification;
    }

    @Override
    public IQuerySpecification<Matcher> getPrecondition() {
        return precondition;
    }

    @Override
    public EventFilter<? super Match> getFilter() {
        if (filter == null) {
            return ruleSpecification.createEmptyFilter();
        } else {
            return filter;
        }
    }
}
