/*******************************************************************************
 * Copyright (c) 2010-2012, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Zoltan Ujhelyi - initial API and implementation
 *******************************************************************************/

package org.eclipse.viatra.query.patternlanguage.emf.tests.composition

import com.google.inject.Inject
import com.google.inject.Injector
import org.eclipse.viatra.query.patternlanguage.emf.tests.util.AbstractValidatorTest
import org.eclipse.viatra.query.patternlanguage.emf.validation.IssueCodes
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.testing.util.ParseHelper
import org.eclipse.xtext.junit4.validation.ValidatorTester
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.eclipse.viatra.query.patternlanguage.emf.vql.PatternModel
import org.eclipse.viatra.query.patternlanguage.emf.validation.EMFPatternLanguageValidator
import org.eclipse.viatra.query.patternlanguage.emf.tests.CustomizedEMFPatternLanguageInjectorProvider

@RunWith(typeof(XtextRunner))
@InjectWith(typeof(CustomizedEMFPatternLanguageInjectorProvider))
class CompositionValidatorTest extends AbstractValidatorTest{
        
    @Inject
    ParseHelper<PatternModel> parseHelper
    @Inject
    EMFPatternLanguageValidator validator
    @Inject
    Injector injector
    
    ValidatorTester<EMFPatternLanguageValidator> tester
    
    @Before
    def void initialize() {
        tester = new ValidatorTester(validator, injector)
    }
    @Test
    def void duplicatePatterns() {
        val model = parseHelper.parse(
            'package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/viatra/query/patternlanguage/emf/PatternLanguage"

            pattern calledPattern(p : Pattern) = {
                Pattern(p);
            }

            pattern calledPattern(p : Pattern) = {
                Pattern(p);
            }'
        ) 
        tester.validate(model).assertAll(getErrorCode(IssueCodes::DUPLICATE_PATTERN_DEFINITION), getErrorCode(IssueCodes::DUPLICATE_PATTERN_DEFINITION));
    }	
    @Test
    def void duplicatePatternsIgnoreCase() {
        val model = parseHelper.parse(
            'package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/viatra/query/patternlanguage/emf/PatternLanguage"

            pattern calledPattern(p : Pattern) = {
                Pattern(p);
            }

            pattern calledpattern(p : Pattern) = {
                Pattern(p);
            }'
        ) 
        tester.validate(model).assertAll(getErrorCode(IssueCodes::DUPLICATE_PATTERN_DEFINITION), getErrorCode(IssueCodes::DUPLICATE_PATTERN_DEFINITION));
    }	
    @Test
    def void duplicateParameters() {
        val model = parseHelper.parse(
            'package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/viatra/query/patternlanguage/emf/PatternLanguage"

            pattern calledPattern(p : Pattern, p) = {
                Pattern(p);
            }

            pattern callPattern(p : Pattern) = {
                Pattern(p);
            }'
        )
        tester.validate(model).assertAll(
            getErrorCode(IssueCodes::DUPLICATE_PATTERN_PARAMETER_NAME),
            getErrorCode(IssueCodes::DUPLICATE_PATTERN_PARAMETER_NAME)
        )
    }	
    
    
   @Test
   def void duplicateParameters2() {
       val model = parseHelper.parse(
           '''package org.eclipse.incquery.patternlanguage.emf.tests
           import "http://www.eclipse.org/viatra/query/patternlanguage/emf/PatternLanguage"

           pattern calledPattern(p : Pattern, p : Pattern) = {
               Pattern(p);
           }

           pattern callPattern(p : Pattern) = {
               Pattern(p);
           }'''
       )
       tester.validate(model).assertAll(
           getErrorCode(IssueCodes::DUPLICATE_PATTERN_PARAMETER_NAME),
           getErrorCode(IssueCodes::DUPLICATE_PATTERN_PARAMETER_NAME)
       )
   }   
    
    
    @Test
    def void testTooFewParameters() {
        val model = parseHelper.parse(
            'package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/viatra/query/patternlanguage/emf/PatternLanguage"

            pattern calledPattern(p : Pattern, p2 : Pattern) = {
                Pattern(p);
                Pattern(p2);
            }

            pattern callPattern(p : Pattern) = {
                find calledPattern(p);
            }'
        )
        tester.validate(model).assertAll(
            getErrorCode(IssueCodes::WRONG_NUMBER_PATTERNCALL_PARAMETER),
            getWarningCode(IssueCodes::CARTESIAN_STRICT_WARNING)
        )
    }
    @Test
    def void testTooMuchParameters() {
        val model = parseHelper.parse(
            'package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/viatra/query/patternlanguage/emf/PatternLanguage"

            pattern calledPattern(p : Pattern) = {
                Pattern(p);
            }

            pattern callPattern(p : Pattern) = {
                find calledPattern(p, p);
            }'
        )
        tester.validate(model).assertError(IssueCodes::WRONG_NUMBER_PATTERNCALL_PARAMETER);
    }
    @Test
    def void testSymbolicParameterSafe() {
        val model = parseHelper.parse(
            'package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/viatra/query/patternlanguage/emf/PatternLanguage"

            pattern calledPattern(p : Pattern) = {
                Pattern(p);
                neg find calledPattern(p);
            }'
        )
        
        tester.validate(model).assertError(IssueCodes.RECURSIVE_PATTERN_CALL, "Negative pattern")
    }
    @Test
    def void testQuantifiedLocalVariable() {
        val model = parseHelper.parse(
            'package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/viatra/query/patternlanguage/emf/PatternLanguage"

            pattern calledPattern(p : Pattern) = {
                Pattern(p);
            }

            pattern callerPattern(c : Pattern) = {
                Pattern(c);
                Pattern(p);
                neg find calledPattern(p);
            }'
        )
        tester.validate(model).assertAll(
            getWarningCode(IssueCodes::CARTESIAN_STRICT_WARNING)
        )
    }
    @Test
    def void testNegativeCallOnlySingleUseVariables() {
        val model = parseHelper.parse(
            'package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/viatra/query/patternlanguage/emf/PatternLanguage"

            pattern calledPattern(p : Pattern) = {
                Pattern(p);
            }

            pattern callerPattern(c : Pattern) = {
                Pattern(c);
                neg find calledPattern(_);
            }'
        )
        tester.validate(model).assertAll(
            getWarningCode(IssueCodes.NEGATIVE_PATTERN_CALL_WITH_ONLY_SINGLE_USE_VARIABLES)
        )
    }
    @Test @Ignore(value = "This call is unsafe because of a negative call circle. 
                           p: Pattern is a positive reference.")
    def void testNegativeCallCircle() {
        val model = parseHelper.parse(
            'package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/viatra/query/patternlanguage/emf/PatternLanguage"

            pattern calledPattern(p : Pattern) = {
                Pattern(p);
            } or {
                neg find calledPattern(p);
            }'
        )
        tester.validate(model).assertError("");
    }
}