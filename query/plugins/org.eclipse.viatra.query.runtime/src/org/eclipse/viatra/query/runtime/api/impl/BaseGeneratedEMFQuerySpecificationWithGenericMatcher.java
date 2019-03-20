/*******************************************************************************
 * Copyright (c) 2010-2017, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.api.impl;

import org.eclipse.viatra.query.runtime.api.GenericPatternMatch;
import org.eclipse.viatra.query.runtime.api.GenericPatternMatcher;
import org.eclipse.viatra.query.runtime.api.GenericQuerySpecification;
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngine;
import org.eclipse.viatra.query.runtime.api.scope.QueryScope;
import org.eclipse.viatra.query.runtime.emf.EMFScope;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PQuery;

/**
 * Provides common functionality of pattern-specific generated query specifications for without generated
 * pattern-specific match and matcher classes, including private patterns.
 * 
 * @since 1.7
 *
 */
public abstract class BaseGeneratedEMFQuerySpecificationWithGenericMatcher
        extends GenericQuerySpecification<GenericPatternMatcher> {

    public BaseGeneratedEMFQuerySpecificationWithGenericMatcher(PQuery wrappedPQuery) {
        super(wrappedPQuery);
    }

    @Override
    public Class<? extends QueryScope> getPreferredScopeClass() {
        return EMFScope.class;
    }

    @Override
    protected GenericPatternMatcher instantiate(final ViatraQueryEngine engine) {
        return defaultInstantiate(engine);
    }

    @Override
    public GenericPatternMatcher instantiate() {
        return new GenericPatternMatcher(this);
    }

    @Override
    public GenericPatternMatch newEmptyMatch() {
        return GenericPatternMatch.newEmptyMatch(this);
    }

    @Override
    public GenericPatternMatch newMatch(final Object... parameters) {
        return GenericPatternMatch.newMatch(this, parameters);
    }

}