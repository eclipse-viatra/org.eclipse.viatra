/*******************************************************************************
 * Copyright (c) 2010-2024, Gábor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.patternlanguage.emf.tests.basic

import com.google.inject.Inject
import com.google.inject.Injector
import org.eclipse.viatra.query.patternlanguage.emf.tests.CustomizedEMFPatternLanguageInjectorProvider
import org.eclipse.viatra.query.patternlanguage.emf.tests.util.AbstractValidatorTest
import org.eclipse.viatra.query.patternlanguage.emf.validation.EMFPatternLanguageValidator
import org.eclipse.viatra.query.patternlanguage.emf.vql.PatternModel
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.testing.util.ParseHelper
import org.junit.runner.RunWith
import org.eclipse.xtext.testing.validation.ValidatorTester
import org.junit.Before
import org.junit.Test
import org.eclipse.viatra.query.patternlanguage.emf.vql.CompareConstraint
import org.eclipse.viatra.query.patternlanguage.emf.vql.StringValue
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotEquals
import org.eclipse.emf.common.util.Diagnostic

@RunWith(typeof(XtextRunner))
@InjectWith(typeof(CustomizedEMFPatternLanguageInjectorProvider))
class StringLiteralTest extends AbstractValidatorTest {
  
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
    def testStringLiteralEscaping() {
        val model = parseHelper.parse(
            '''
            pattern funnyStringLiteral(a : java String) {
                a == "a\n\t\"b";
            }'''
        )
        tester.validate(model).assertOK
        val stringLiterals = model.patterns.flatMap[bodies].flatMap[constraints].filter(CompareConstraint).map[rightOperand].filter(StringValue)
        assertEquals(1, stringLiterals.size)
        assertEquals("a\n\t\"b", stringLiterals.head.value)
        
    }
    
    @Test
    def testStringAnnotationEscaping() {
        val model = parseHelper.parse(
            '''
            @Annotated(by = "a\n\t\"b")
            pattern funnyStringLiteral(a : java String) {
                a == "";
            }'''
        )
        assertNotEquals(Diagnostic.ERROR, tester.validate(model).diagnostic.severity) 
        val stringAnnotationParams = model.patterns.flatMap[annotations].flatMap[parameters].map[value].filter(StringValue)
        assertEquals(1, stringAnnotationParams.size)
        assertEquals("a\n\t\"b", stringAnnotationParams.head.value)
        
    }
}