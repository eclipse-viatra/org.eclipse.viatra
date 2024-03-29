/**
 * Copyright (c) 2004-2013, Abel Hegedus and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.viatra.transformation.runtime.emf.filters;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.viatra.query.runtime.api.IPatternMatch;
import org.eclipse.viatra.query.runtime.api.ViatraQueryMatcher;
import org.eclipse.viatra.transformation.evm.api.event.EventFilter;

/**
 * A EVM filter that uses a parameter-value map that can be used for multiple patterns and rules.
 * 
 * Use ParameterFilterFactory to create easily manage the mapping and create unmodifiable copies to be added to rules.</p>
 * 
 * <b>Note</b> When using the filter, parameter keys that does not match any parameter of a pattern match are ignored.
 * 
 * @author Abel Hegedus
 */
public class MatchParameterFilter implements EventFilter<IPatternMatch> {
    private Map<String, Object> filterMap;

    /**
     * @since 2.2
     */
    public MatchParameterFilter(String name, Object parameter) {
        this.filterMap = new HashMap<>();
        filterMap.put(name, parameter);
    }
    
    public MatchParameterFilter(final Map<String, Object> filterMap) {
        this.filterMap = new HashMap<>();
        this.filterMap.putAll(filterMap);
    }

    
    /**
     * @since 2.0
     */
    @SafeVarargs
    public MatchParameterFilter(final Entry<String, ?>... parameters) {
        this.filterMap = Arrays.stream(parameters).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    @Override
    public boolean isProcessable(final IPatternMatch eventAtom) {
        return eventAtom.parameterNames().stream()
                .noneMatch(it -> (filterMap.containsKey(it) && (!Objects.equals(filterMap.get(it), eventAtom.get(it)))));
    }

    public <Match extends IPatternMatch, Matcher extends ViatraQueryMatcher<Match>> Match toMatch(
            final Matcher matcher) {
        final Match match = matcher.newEmptyMatch();
        matcher.getParameterNames().stream().filter(filterMap::containsKey)
                .forEach(name -> match.set(name, filterMap.get(name)));
        return match;
    }
}
