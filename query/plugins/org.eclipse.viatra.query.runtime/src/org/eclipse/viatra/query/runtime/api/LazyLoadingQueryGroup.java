/*******************************************************************************
 * Copyright (c) 2010-2016, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Zoltan Ujhelyi - initial API and implementation
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.api;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.viatra.query.runtime.api.impl.BaseQueryGroup;
import org.eclipse.viatra.query.runtime.matchers.util.Preconditions;
import org.eclipse.viatra.query.runtime.util.ViatraQueryLoggingUtil;

/**
 * Initializes a query group from a set of query providers. The query providers are not executed until the queries
 * themselves are asked in the {@link #getSpecifications()} method.
 * 
 * @author Zoltan Ujhelyi
 * @since 1.3
 *
 */
public class LazyLoadingQueryGroup extends BaseQueryGroup {

    private final Set<? extends Supplier<IQuerySpecification<?>>> providers;
    private Set<IQuerySpecification<?>> specifications = null;

    /**
     * @param providers a non-null set to initialize the group
     */
    public LazyLoadingQueryGroup(Set<? extends Supplier<IQuerySpecification<?>>> providers) {
        Preconditions.checkArgument(providers != null, "The set of providers must not be null");
        this.providers = providers;
    }

    /**
     * @param providers a non-null set to initialize the group
     */
    public static IQueryGroup of(Set<? extends Supplier<IQuerySpecification<?>>> querySpecifications) {
        return new LazyLoadingQueryGroup(querySpecifications);
    }

    @Override
    public Set<IQuerySpecification<?>> getSpecifications() {
        if (specifications == null) {
            try {
                specifications = providers.stream().filter(Objects::nonNull).map(Supplier::get).filter(Objects::nonNull).collect(Collectors.toSet());
            } catch (Exception e) {
                // TODO maybe store in issue list and provide better error reporting in general
                String errorMessage = "Exception occurred while accessing query specification from provider: " + e.getMessage();
                ViatraQueryLoggingUtil.getLogger(getClass()).error(errorMessage);
                return Collections.emptySet();
            }
        }
        return specifications;
    }

}
