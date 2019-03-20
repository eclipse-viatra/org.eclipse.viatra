/*******************************************************************************
 * Copyright (c) 2010-2015, Marton Bur, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.localsearch.matcher.integration;

import org.eclipse.viatra.query.runtime.matchers.backend.IMatcherCapability;
import org.eclipse.viatra.query.runtime.matchers.backend.IQueryBackend;
import org.eclipse.viatra.query.runtime.matchers.backend.IQueryBackendFactory;
import org.eclipse.viatra.query.runtime.matchers.backend.QueryEvaluationHint;
import org.eclipse.viatra.query.runtime.matchers.context.IQueryBackendContext;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PQuery;

/**
 * @author Marton Bur, Zoltan Ujhelyi
 * @since 2.0
 *
 */
public enum LocalSearchEMFBackendFactory implements IQueryBackendFactory {
    
    
    INSTANCE;
    
    /**
     * @since 1.5
     */
    @Override
    public IQueryBackend create(IQueryBackendContext context) {
        return new LocalSearchBackend(context) {
            
            @Override
            protected AbstractLocalSearchResultProvider initializeResultProvider(PQuery query, QueryEvaluationHint hints) {
                return new LocalSearchResultProvider(this, context, query, planProvider, hints);
            }
            
            @Override
            public IQueryBackendFactory getFactory() {
                return INSTANCE;
            }
        };
    }
    
    @Override
    public Class<? extends IQueryBackend> getBackendClass() {
        return LocalSearchBackend.class;
    }

    /**
     * @since 1.4
     */
    @Override
    public IMatcherCapability calculateRequiredCapability(PQuery query, QueryEvaluationHint hint) {
        return LocalSearchHints.parse(hint);
    }

    @Override
    public boolean isCaching() {
        return false;
    }

}
