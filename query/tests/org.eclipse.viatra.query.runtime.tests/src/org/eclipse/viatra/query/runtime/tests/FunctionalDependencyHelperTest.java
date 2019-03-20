/*******************************************************************************
 * Copyright (c) 2010-2013, Adam Dudas, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.tests;

import static org.eclipse.viatra.query.runtime.matchers.planning.helpers.FunctionalDependencyHelper.closureOf;
import static org.eclipse.viatra.query.runtime.matchers.planning.helpers.FunctionalDependencyHelper.projectDependencies;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.viatra.query.runtime.matchers.planning.helpers.FunctionalDependencyHelper;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Tests for {@link FunctionalDependencyHelper}.
 * 
 * @author Adam Dudas
 * 
 */
public class FunctionalDependencyHelperTest {

    private static final Set<Object> emptySet = ImmutableSet.<Object> of();
    private static final Map<Set<Object>, Set<Object>> emptyMap = ImmutableMap.<Set<Object>, Set<Object>> of();
    private static final Object a = new Object();
    private static final Object b = new Object();
    private static final Object c = new Object();
    private static final Object d = new Object();
    private static final Object e = new Object();
    private static final Map<Set<Object>, Set<Object>> testDependencies = ImmutableMap.<Set<Object>, Set<Object>> of(
            ImmutableSet.of(a, b), ImmutableSet.of(c), // AB -> C
            ImmutableSet.of(a), ImmutableSet.of(d), // A -> D
            ImmutableSet.of(d), ImmutableSet.of(e), // D -> E
            ImmutableSet.of(a, c), ImmutableSet.of(b)); // AC -> B

    @Test
    public void testClosureOfEmptyAttributeSetEmptyDependencySet() {
        assertEquals(emptySet, closureOf(emptySet, emptyMap));
    }

    @Test
    public void testClosureOfEmptyAttributeSet() {
        assertEquals(emptySet, closureOf(emptySet, testDependencies));
    }

    @Test
    public void testClosureOfEmptyDependencySet() {
        Set<Object> X = ImmutableSet.of(a, b, c, d);
        assertEquals(X, closureOf(X, emptyMap));
    }

    @Test
    public void testClosureOf() {
        assertEquals(ImmutableSet.of(a, d, e), closureOf(ImmutableSet.of(a), testDependencies));
        assertEquals(ImmutableSet.of(a, b, c, d, e), closureOf(ImmutableSet.of(a, b), testDependencies));
        assertEquals(ImmutableSet.of(a, b, c, d, e), closureOf(ImmutableSet.of(a, c), testDependencies));
        assertEquals(ImmutableSet.of(b), closureOf(ImmutableSet.of(b), testDependencies));
        assertEquals(ImmutableSet.of(d, e), closureOf(ImmutableSet.of(d), testDependencies));
    }
    
    @Test
    public void testProject() {
        assertEquals(Collections.emptyMap(), projectDependencies(testDependencies, ImmutableSet.of(a)));
        assertEquals(ImmutableMap.of(Collections.singleton(a), ImmutableSet.of(a, e)), 
                projectDependencies(testDependencies, ImmutableSet.of(a, c, e)));
    }

}
