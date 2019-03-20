/*******************************************************************************
 * Copyright (c) 2010-2013, Abel Hegedus, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.transformation.evm.specific.event;

import org.eclipse.viatra.query.runtime.api.IPatternMatch;
import org.eclipse.viatra.query.runtime.api.IQuerySpecification;
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngine;
import org.eclipse.viatra.query.runtime.api.ViatraQueryMatcher;
import org.eclipse.viatra.query.runtime.matchers.util.Preconditions;
import org.eclipse.viatra.transformation.evm.api.event.AbstractRuleInstanceBuilder;
import org.eclipse.viatra.transformation.evm.api.event.EventFilter;
import org.eclipse.viatra.transformation.evm.api.event.EventRealm;
import org.eclipse.viatra.transformation.evm.api.event.EventSourceSpecification;

/**
 * @author Abel Hegedus
 *
 */
public class ViatraQueryEventSourceSpecification<Match extends IPatternMatch> implements EventSourceSpecification<Match> {

    private IQuerySpecification<? extends ViatraQueryMatcher<Match>> querySpecification;
    private final EventFilter<Match> EMPTY_FILTER = new ViatraQueryMatchEventFilter<Match>();
    
    protected ViatraQueryEventSourceSpecification(IQuerySpecification<? extends ViatraQueryMatcher<Match>> factory) {
        Preconditions.checkArgument(factory != null, "Cannot create source definition for null querySpecification!");
        this.querySpecification = factory;
    }

    @Override
    public EventFilter<Match> createEmptyFilter() {
        return EMPTY_FILTER;
    }
    
    /**
     * @return the querySpecification
     */
    public IQuerySpecification<? extends ViatraQueryMatcher<Match>> getQuerySpecification() {
        return querySpecification;
    }
    
    protected ViatraQueryMatcher<Match> getMatcher(ViatraQueryEngine engine) {
        ViatraQueryMatcher<Match> matcher = querySpecification.getMatcher(engine);
        return matcher;
    }
    
    @Override
    public AbstractRuleInstanceBuilder<Match> getRuleInstanceBuilder(EventRealm realm) {
        return new ViatraQueryRuleInstanceBuilder<Match>((ViatraQueryEventRealm) realm, this);
    }
    
}
