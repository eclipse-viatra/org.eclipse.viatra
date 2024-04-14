/*******************************************************************************
 * Copyright (c) 2010-2016, Abel Hegedus and IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.tests.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.viatra.query.patternlanguage.emf.vql.PatternModel;
import org.eclipse.viatra.query.patternlanguage.emf.specification.SpecificationBuilder;
import org.eclipse.viatra.query.patternlanguage.emf.tests.CustomizedEMFPatternLanguageInjectorProvider;
import org.eclipse.viatra.query.runtime.api.IPatternMatch;
import org.eclipse.viatra.query.runtime.api.IQuerySpecification;
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngine;
import org.eclipse.viatra.query.runtime.api.ViatraQueryMatcher;
import org.eclipse.viatra.query.runtime.emf.EMFScope;
import org.eclipse.viatra.query.runtime.matchers.backend.QueryEvaluationHint.BackendRequirement;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

@RunWith(XtextRunner.class)
@InjectWith(CustomizedEMFPatternLanguageInjectorProvider.class)
public class QueryEngineTest {

    @Inject
    ParseHelper<PatternModel> parseHelper;
    
    /**
     * Test that duplicate FQNs are allowed by the engine.
     * This test ensures that we detect any changes in this semantic.
     * See bug 496638
     */
    @Test
    public void duplicateFQNTest() throws Exception {
        ResourceSetImpl rs = new ResourceSetImpl();
        ViatraQueryEngine engine = ViatraQueryEngine.on(new EMFScope(rs));
        String patternCode = "package org.eclipse.viatra.query.patternlanguage.emf.tests\n"
                            + "import \"http://www.eclipse.org/viatra/query/patternlanguage/emf/PatternLanguage\"\n"
                            + "pattern p(p : Pattern) = {\n"
                            + " Pattern(p);\n"
                            + "}";
        PatternModel model = parseHelper.parse(patternCode);
        PatternModel model2 = parseHelper.parse(patternCode);
        
        IQuerySpecification<? extends ViatraQueryMatcher<? extends IPatternMatch>> specification = new SpecificationBuilder().getOrCreateSpecification(model.getPatterns().get(0));
        IQuerySpecification<? extends ViatraQueryMatcher<? extends IPatternMatch>> specification2 = new SpecificationBuilder().getOrCreateSpecification(model2.getPatterns().get(0));
        
        ViatraQueryMatcher<? extends IPatternMatch> matcher = engine.getMatcher(specification);
        ViatraQueryMatcher<? extends IPatternMatch> matcher2 = engine.getMatcher(specification2);
        assertTrue(matcher != matcher2);
    }
    
    @Test
    public void testUnusualParameterTypes() throws Exception {
        ResourceSetImpl rs = new ResourceSetImpl();
        ViatraQueryEngine engine = ViatraQueryEngine.on(new EMFScope(rs));
        String patternCode = "package org.eclipse.viatra.query.patternlanguage.emf.tests\n"
                            + "import \"http://www.eclipse.org/viatra/query/patternlanguage/emf/PatternLanguage\"\n"
                            + "pattern p(m: java ^java.util.Map, e: java ^java.util.Map.Entry, c, u: java Integer) = {\n"
                            + " m == eval(^java.util.Collections.singletonMap(1,2));\n"
                            + " e == eval(m.entrySet.head);\n"
                            + " c == java org.eclipse.viatra.query.runtime.matchers.backend.QueryEvaluationHint.BackendRequirement::SPECIFIC;\n"
                            + " u == 4;\n"
                            + "}";
        PatternModel model = parseHelper.parse(patternCode);
        
        IQuerySpecification<? extends ViatraQueryMatcher<? extends IPatternMatch>> specification = new SpecificationBuilder().getOrCreateSpecification(model.getPatterns().get(0));
        
        ViatraQueryMatcher<? extends IPatternMatch> matcher = engine.getMatcher(specification);
        assertEquals(1, matcher.countMatches());
        IPatternMatch match = matcher.getOneArbitraryMatch().get();
        assertEquals(Collections.singleton(Collections.singletonMap(1,2)), matcher.getAllValues("m"));
        assertTrue(match.get("e") instanceof Map.Entry);
        assertEquals(1, ((Map.Entry)match.get("e")).getKey());
        assertEquals(BackendRequirement.SPECIFIC, match.get("c"));
        
    }
   
}
