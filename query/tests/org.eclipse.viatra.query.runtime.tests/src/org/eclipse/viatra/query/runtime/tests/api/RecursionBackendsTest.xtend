package org.eclipse.viatra.query.runtime.tests.api

import com.google.inject.Inject
import java.util.Collection
import java.util.Map
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.viatra.query.patternlanguage.emf.specification.SpecificationBuilder
import org.eclipse.viatra.query.patternlanguage.emf.tests.CustomizedEMFPatternLanguageInjectorProvider
import org.eclipse.viatra.query.patternlanguage.emf.vql.PatternModel
import org.eclipse.viatra.query.runtime.api.IPatternMatch
import org.eclipse.viatra.query.runtime.api.IQuerySpecification
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngine
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngineOptions
import org.eclipse.viatra.query.runtime.api.ViatraQueryMatcher
import org.eclipse.viatra.query.runtime.emf.EMFScope
import org.eclipse.viatra.query.runtime.matchers.backend.CommonQueryHintOptions
import org.eclipse.viatra.query.runtime.matchers.backend.QueryEvaluationHint
import org.eclipse.viatra.query.runtime.matchers.backend.QueryHintOption
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PQueries
import org.eclipse.viatra.query.runtime.matchers.psystem.rewriters.IRewriterTraceCollector
import org.eclipse.viatra.query.runtime.matchers.psystem.rewriters.MappingTraceCollector
import org.eclipse.viatra.query.runtime.matchers.psystem.rewriters.RewriterException
import org.eclipse.viatra.query.runtime.rete.matcher.ReteBackendFactory
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.testing.util.ParseHelper
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.eclipse.viatra.query.runtime.matchers.planning.QueryProcessingException
import org.eclipse.viatra.query.runtime.localsearch.matcher.integration.LocalSearchHints

@RunWith(XtextRunner) 
@InjectWith(CustomizedEMFPatternLanguageInjectorProvider)
class RecursionBackendsTest {
    @Inject package ParseHelper<PatternModel> parseHelper
    
    @Test 
    def void incrPatternSimpleRecursion() throws Exception {
        Assert.assertEquals(3, evaluateQueryCode("patternRecursive", genQueryCode("", "incremental")).size)                        
    }
    @Test(expected = QueryProcessingException) 
    def void searchPatternSimpleRecursion() throws Exception {
        evaluateQueryCode("patternRecursive", genQueryCode("", "search"))
    }
    
    @Test 
    def void incrPatternPositivelyCalledNonmutualUnmarkedRecursion() throws Exception {
        Assert.assertEquals(3, evaluateQueryCode("positiveCaller", genQueryCode("incremental", "")).size)
    }
    @Test(expected = QueryProcessingException) 
    def void searchPatternPositivelyCalledNonmutualUnmarkedRecursion() throws Exception {
        evaluateQueryCode("positiveCaller", genQueryCode("search", ""))
    }
    
    @Test 
    def void incrPatternPositivelyCalledNonmutualIncrRecursion() throws Exception {
        Assert.assertEquals(3, evaluateQueryCode("positiveCaller", genQueryCode("incremental", "incremental")).size)                        
    }
    @Test(expected = QueryProcessingException) 
    def void searchPatternPositivelyCalledNonmutualIncrRecursion() throws Exception {
        evaluateQueryCode("positiveCaller", genQueryCode("search", "incremental"))   
    }
    @Test 
    def void hybridPatternPositivelyCalledNonmutualIncrRecursion() throws Exception {
        // see https://github.com/eclipse-viatra/org.eclipse.viatra/issues/200                
        Assert.assertEquals(3, evaluateQueryCodeHybrid("positiveCaller", genQueryCode("search", "incremental")).size)   
    }
    
    @Test 
    def void incrPatternNegativelyCalledNonmutualUnmarkedRecursion() throws Exception {
        Assert.assertEquals(0, evaluateQueryCode("negativeCaller", genQueryCode("incremental", "")).size)
    }
    @Test(expected = QueryProcessingException) 
    def void searchPatternNegativelyCalledNonmutualUnmarkedRecursion() throws Exception {
        evaluateQueryCode("negativeCaller", genQueryCode("search", ""))
    }
    
    @Test 
    def void incrPatternNegativelyCalledNonmutualIncrRecursion() throws Exception {
        Assert.assertEquals(0, evaluateQueryCode("negativeCaller", genQueryCode("incremental", "incremental")).size)                        
    }
    @Test(expected = QueryProcessingException) 
    def void searchPatternNegativelyCalledNonmutualIncrRecursion() throws Exception {
        evaluateQueryCode("negativeCaller", genQueryCode("search", "incremental"))   
    }
    @Test 
    def void hybridPatternNegativelyCalledNonmutualIncrRecursion() throws Exception {
        // see https://github.com/eclipse-viatra/org.eclipse.viatra/issues/200                
        Assert.assertEquals(0, evaluateQueryCodeHybrid("negativeCaller", genQueryCode("search", "incremental")).size)   
    }
    
    protected def String genQueryCode(String callerModifier, String recursivePatternModifier) '''
        package org.eclipse.viatra.query.patternlanguage.emf.tests
        import "http://www.eclipse.org/viatra/query/patternlanguage/emf/PatternLanguage"
        
        // this is not recursive, just calls a recursive query
        // so this SHOULD be OK with the incremental backend, and 
        //  conditionally OK with the hybrid search backend if the called recursive pattern has the incremental modifier (hence avoiding the flattener)
        //  otherwise should fail flattening
        «callerModifier» pattern positiveCaller(value: java Integer) {
          find patternRecursive(value);
        }
        
        // this is not recursive, just calls a recursive query
        // so this SHOULD be OK with the incremental backend, and 
        //  conditionally OK with the hybrid search backend if the called recursive pattern has the incremental modifier (hence avoiding the flattener)
        //  otherwise should pass flattening but fail downstream in the LS backend (as opposed to positiveCaller)
        «callerModifier» pattern negativeCaller(value: java Integer) {
          value == 1;
          neg find patternRecursive(value);
        }
        
        // this is actually recursive, which should be fine for an incremental pattern
        «recursivePatternModifier» pattern patternRecursive(value: java Integer) {
          value == 2;
        } or {
          value == eval(doubleValue / 2);
          find patternRecursive(doubleValue);
        }
    '''
    
    protected def Collection<? extends IPatternMatch> evaluateQueryCode(String mainPatternName, String patternCode) {
        return evaluateQueryCode(mainPatternName, patternCode, ViatraQueryEngineOptions.^default)    
    }

    protected def Collection<? extends IPatternMatch> evaluateQueryCodeHybrid(String mainPatternName, String patternCode) {
        val localSearchHints = LocalSearchHints.getDefaultGenericHybrid()
        val localSearchQueryOptions = localSearchHints.build()
        val queryEngineOptions = ViatraQueryEngineOptions
            .defineOptions()
            .withDefaultHint(localSearchQueryOptions)
            .withDefaultBackend(localSearchQueryOptions.queryBackendFactory)
            .withDefaultSearchBackend(localSearchQueryOptions.queryBackendFactory)
            .build()
        return evaluateQueryCode(mainPatternName, patternCode, queryEngineOptions)    
    }

    protected def Collection<? extends IPatternMatch> evaluateQueryCode(String mainPatternName, String patternCode, ViatraQueryEngineOptions queryEngineOptions) {
        var ResourceSetImpl rs = new ResourceSetImpl()                    
        var ViatraQueryEngine engine = ViatraQueryEngine.on(new EMFScope(rs), queryEngineOptions)
        
        val PatternModel model = parseHelper.parse(patternCode)
        val mainPattern = model.getPatterns().filter[name == mainPatternName].head
        val IQuerySpecification<? extends ViatraQueryMatcher<? extends IPatternMatch>> specification = new SpecificationBuilder().getOrCreateSpecification(mainPattern)
        val ViatraQueryMatcher<? extends IPatternMatch> matcher = engine.getMatcher(specification)
        matcher.allMatches
    }
    
    
}