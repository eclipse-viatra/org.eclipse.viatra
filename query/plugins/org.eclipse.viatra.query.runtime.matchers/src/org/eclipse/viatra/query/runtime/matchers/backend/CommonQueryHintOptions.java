/*******************************************************************************
 * Copyright (c) 2010-2017, Grill Balázs, IncQueryLabs
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.matchers.backend;

import org.eclipse.viatra.query.runtime.matchers.psystem.rewriters.ExternalQueryPlanProvider;
import org.eclipse.viatra.query.runtime.matchers.psystem.rewriters.IRewriterTraceCollector;
import org.eclipse.viatra.query.runtime.matchers.psystem.rewriters.NopTraceCollector;

/**
 * Query evaluation hints applicable to any engine
 * @since 1.6
 *
 */
public final class CommonQueryHintOptions {
    
    private CommonQueryHintOptions() {
        // Hiding constructor for utility class
    }
    
    /**
     * This hint instructs the query backends to record trace information into the given trace collector
     */
    public static final QueryHintOption<IRewriterTraceCollector> normalizationTraceCollector = 
            hintOption("normalizationTraceCollector", NopTraceCollector.INSTANCE);
    
    /**
     * This hint allows to plug in an external query planner.
     * 
     * @since 2.8
     */
    public static final QueryHintOption<ExternalQueryPlanProvider> externalQueryPlanProvider = 
            hintOption("externalQueryPlanProvider", null);

    
    // internal helper for conciseness
    private static <T> QueryHintOption<T> hintOption(String hintKeyLocalName, T defaultValue) {
        return new QueryHintOption<>(CommonQueryHintOptions.class, hintKeyLocalName, defaultValue);
    }

}
