/*******************************************************************************
 * Copyright (c) 2010-2016, Balazs Grill and IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.tests.api;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;

import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.viatra.query.patternlanguage.emf.vql.PatternLanguagePackage;
import org.eclipse.viatra.query.patternlanguage.emf.vql.PatternModel;
import org.eclipse.viatra.query.patternlanguage.emf.specification.SpecificationBuilder;
import org.eclipse.viatra.query.patternlanguage.emf.tests.CustomizedEMFPatternLanguageInjectorProvider;
import org.eclipse.viatra.query.runtime.api.AdvancedViatraQueryEngine;
import org.eclipse.viatra.query.runtime.api.IPatternMatch;
import org.eclipse.viatra.query.runtime.api.IQuerySpecification;
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngine;
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngineOptions;
import org.eclipse.viatra.query.runtime.api.ViatraQueryMatcher;
import org.eclipse.viatra.query.runtime.emf.EMFScope;
import org.eclipse.viatra.query.runtime.emf.types.EStructuralFeatureInstancesKey;
import org.eclipse.viatra.query.runtime.localsearch.matcher.integration.LocalSearchHints;
import org.eclipse.viatra.query.runtime.localsearch.matcher.integration.LocalSearchResultProvider;
import org.eclipse.viatra.query.runtime.matchers.backend.IQueryResultProvider;
import org.eclipse.viatra.query.runtime.matchers.backend.QueryEvaluationHint;
import org.eclipse.viatra.query.runtime.matchers.context.surrogate.SurrogateQueryRegistry;
import org.eclipse.viatra.query.runtime.rete.matcher.ReteBackendFactory;
import org.eclipse.viatra.query.runtime.rete.matcher.RetePatternMatcher;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

/**
 * Test that the {@link ViatraQueryEngine} takes {@link QueryEvaluationHint} into account when creating matchers
 * 
 * @author Balazs Grill
 *
 */
@RunWith(XtextRunner.class)
@InjectWith(CustomizedEMFPatternLanguageInjectorProvider.class)
public class QueryEvaluationsHintTest {

    @Inject
    ParseHelper<PatternModel> parseHelper;
    
    private AdvancedViatraQueryEngine engine;
    private IQuerySpecification<? extends ViatraQueryMatcher<? extends IPatternMatch>> specification;
    
    @Before
    public void setUp() throws Exception{
        String patternCode = "package org.eclipse.viatra.query.patternlanguage.emf.tests\n"
                            + "import \"http://www.eclipse.org/viatra/query/patternlanguage/emf/PatternLanguage\"\n"
                            + "pattern patternsWithName(patternObject : Pattern, nameString : java String) = {\n"
                            + " Pattern.name(patternObject, nameString);\n"
                            + "}";
        PatternModel model = parseHelper.parse(patternCode);
        specification = new SpecificationBuilder().getOrCreateSpecification(model.getPatterns().get(0));
        engine = AdvancedViatraQueryEngine.createUnmanagedEngine(new EMFScope(model));
    }
    
    @After
    public void tearDown() {
        engine.wipe();
    }
    
    /**
     * Test that an empty engine returns a local search matcher when requested
     */
    @Test
    public void testLocalSearch() throws Exception {
        ViatraQueryMatcher<?> matcher = engine.getMatcher(specification, LocalSearchHints.getDefault().build());
        IQueryResultProvider resultProvider = engine.getResultProviderOfMatcher(matcher);
        Assert.assertTrue(resultProvider + "is not local search", resultProvider instanceof LocalSearchResultProvider);
    }
    
    /**
     * Test that an empty engine returns a local search matcher when requested
     */
    @Test
    public void testEngineWideLocalSearch() throws Exception {
        QueryEvaluationHint localSearchHint = LocalSearchHints.getDefault().build();
        ViatraQueryEngineOptions options = ViatraQueryEngineOptions.defineOptions()
                .withDefaultHint(localSearchHint).build();
        ResourceSetImpl rs = new ResourceSetImpl();
        AdvancedViatraQueryEngine lsengine = AdvancedViatraQueryEngine.createUnmanagedEngine(new EMFScope(rs), options);
        ViatraQueryMatcher<?> matcher = lsengine.getMatcher(specification);
        IQueryResultProvider resultProvider = lsengine.getResultProviderOfMatcher(matcher);
        Assert.assertTrue(resultProvider + "is not local search", resultProvider instanceof LocalSearchResultProvider);
        lsengine.dispose();
    }
    
    /**
     * Test that an empty engine returns a rete matcher when requested
     */
    @Test
    public void testRete() throws Exception {
        ViatraQueryMatcher<?> matcher = engine.getMatcher(specification, new QueryEvaluationHint(null, ReteBackendFactory.INSTANCE));
        IQueryResultProvider resultProvider = engine.getResultProviderOfMatcher(matcher);
        Assert.assertTrue(resultProvider + "is not rete", resultProvider instanceof RetePatternMatcher);
    }
    
    /**
     * Test that no local search matcher is created when a Rete is already initialized
     */
    @Test
    public void testReteOverridesLS() throws Exception {
        // First, initialize a Rete matcher
        engine.getMatcher(specification, new QueryEvaluationHint(null, ReteBackendFactory.INSTANCE));
        // Then try to request a local search matcher
        ViatraQueryMatcher<?> matcher = engine.getMatcher(specification, LocalSearchHints.getDefault().build());
        IQueryResultProvider resultProvider = engine.getResultProviderOfMatcher(matcher);
        Assert.assertTrue(resultProvider + "is not rete", resultProvider instanceof RetePatternMatcher);
    }
    
    /**
     * Test that local search ignores surrogates by default
     */
    @Test
    public void testSurrogateUsageLSDefault() throws Exception {
        tryWithSurrogate(() -> {
            Assert.assertEquals(
                    "With consultSurrogates set to false, LS should use EMF getters instead of surrogate queries",
                    Collections.singleton("patternsWithName"), 
                    getAllPatternNames(LocalSearchHints.getDefault().build())
            );
            return null;
        });
    }
    /**
     * Test that local search ignores surrogates if hinted to do so
     */
    @Test
    public void testSurrogateUsageLSNoConsult() throws Exception {
        tryWithSurrogate(() -> {
            Assert.assertEquals(
                    "With default parameters, LS should use EMF getters instead of surrogate queries",
                    Collections.singleton("patternsWithName"), 
                    getAllPatternNames(LocalSearchHints.getDefault().setConsultSurrogates(false).build())
            );
            return null;
        });
    }
    /**
     * Test that local search uses surrogates if hinted to do so
     */
    @Test
    public void testSurrogateUsageLSConsult() throws Exception {
        tryWithSurrogate(() -> {
            Assert.assertEquals(
                    "With consultSurrogates set to true, LS should use EMF getters instead of surrogate queries",
                    Collections.singleton("patternObject"), 
                    getAllPatternNames(LocalSearchHints.getDefault().setConsultSurrogates(true).build())
            );
            return null;
        });
    }
    /**
     * Test Rete uses surrogates always 
     */
    @Test
    public void testSurrogateUsageRete() throws Exception {
        tryWithSurrogate(() -> {
            Assert.assertEquals(
                    "Rete should always use surrogate queries",
                    Collections.singleton("patternObject"), 
                    getAllPatternNames(new QueryEvaluationHint(null, ReteBackendFactory.INSTANCE))
            );
            return null;
        });
    }

    private Set<Object> getAllPatternNames(QueryEvaluationHint hints) {
        return engine.getMatcher(specification, hints).getAllValues("nameString");
    }
    
    private void tryWithSurrogate(Callable payload) throws Exception {
        String surrogatePatternCode = "package org.eclipse.viatra.query.patternlanguage.emf.tests.surrogates\n"
                + "import \"http://www.eclipse.org/viatra/query/patternlanguage/emf/PatternLanguage\"\n"
                + "pattern surrogate(p : Pattern, name : java String) = {\n"
                + " Pattern(p);\n"
                + " name == \"patternObject\";\n"
                + "}";
        PatternModel surrogateQueriesModel = parseHelper.parse(surrogatePatternCode);
        IQuerySpecification<? extends ViatraQueryMatcher<? extends IPatternMatch>> surrogate = 
                new SpecificationBuilder().getOrCreateSpecification(surrogateQueriesModel.getPatterns().get(0));
        EStructuralFeatureInstancesKey surrogateKey = new EStructuralFeatureInstancesKey(PatternLanguagePackage.eINSTANCE.getPattern_Name());
        
        SurrogateQueryRegistry.instance().addDynamicSurrogateQueryForFeature(surrogateKey, surrogate.getInternalQueryRepresentation());
        try {
            payload.call();
        } finally {
            SurrogateQueryRegistry.instance().removeDynamicSurrogateQueryForFeature(surrogateKey);
        }
    }
    

    
}
