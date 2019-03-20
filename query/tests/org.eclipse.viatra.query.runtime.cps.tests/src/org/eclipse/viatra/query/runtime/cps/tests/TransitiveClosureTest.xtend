/*******************************************************************************
 * Copyright (c) 2014-2016 Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.cps.tests

import java.util.Collection
import org.eclipse.viatra.query.runtime.cps.tests.queries.util.StatesTCQuerySpecification
import org.eclipse.viatra.query.testing.core.api.ViatraQueryTest
import org.junit.Test
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.eclipse.emf.ecore.resource.ResourceSet
import org.eclipse.viatra.query.testing.core.ModelLoadHelper
import org.junit.Before
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.viatra.query.runtime.emf.EMFScope

@RunWith(Parameterized)
class TransitiveClosureTest {
    
    @Parameters(name = "{0}")
    def static Collection<Object[]> testData() {
        #[
            #[ "org.eclipse.viatra.query.runtime.cps.tests/models/snapshots/test_tc_bug_520194.snapshot" ],
            #[ "org.eclipse.viatra.query.runtime.cps.tests/models/snapshots/test_tc_filtered_w_match_bug_520194.snapshot" ],
            #[ "org.eclipse.viatra.query.runtime.cps.tests/models/snapshots/test_tc_filtered_wo_match_bug_520194.snapshot" ]
        ]
    }
    
    @Parameter(0)
    public String snapshot
    
    var ResourceSet set
    var EMFScope scope

    extension org.eclipse.viatra.query.runtime.cps.tests.AllBackendTypes = new org.eclipse.viatra.query.runtime.cps.tests.AllBackendTypes
    extension ModelLoadHelper = new ModelLoadHelper
    
    @Before
    def void initialize() {
        set = new ResourceSetImpl
        scope = new EMFScope(set)
    }
    
    @Test
    def void simpleTransitiveClosure() {
        ViatraQueryTest.test(StatesTCQuerySpecification.instance)
                        .on(scope)
                        .with(set.loadExpectedResultsFromUri(snapshot))
                        .withAll
                        .assertEquals
    }

}
