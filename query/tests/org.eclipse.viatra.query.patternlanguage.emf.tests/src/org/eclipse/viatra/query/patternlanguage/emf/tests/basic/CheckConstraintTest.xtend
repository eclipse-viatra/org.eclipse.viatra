/*******************************************************************************
 * Copyright (c) 2010-2012, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.patternlanguage.emf.tests.basic

import com.google.inject.Inject
import com.google.inject.Injector
import org.eclipse.viatra.query.patternlanguage.emf.vql.PatternModel
import org.eclipse.viatra.query.patternlanguage.emf.validation.IssueCodes
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.testing.util.ParseHelper
import org.eclipse.xtext.testing.validation.ValidationTestHelper
import org.eclipse.xtext.testing.validation.ValidatorTester
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.eclipse.viatra.query.patternlanguage.emf.tests.util.AbstractValidatorTest
import org.eclipse.viatra.query.patternlanguage.emf.validation.EMFPatternLanguageValidator
import org.eclipse.viatra.query.patternlanguage.emf.tests.CustomizedEMFPatternLanguageInjectorProvider
import org.eclipse.emf.common.util.Diagnostic
import static org.junit.Assert.assertNotEquals

@RunWith(typeof(XtextRunner))
@InjectWith(typeof(CustomizedEMFPatternLanguageInjectorProvider))
class CheckConstraintTest extends AbstractValidatorTest {

    @Inject
    ParseHelper<PatternModel> parseHelper

    @Inject
    EMFPatternLanguageValidator validator

    @Inject
    Injector injector

    ValidatorTester<EMFPatternLanguageValidator> tester

    @Inject extension ValidationTestHelper

    @Before
    def void initialize() {
        tester = new ValidatorTester(validator, injector)
    }

    @Test
    def whitelistedMethodCheck1() {
        val model = parseHelper.parse('
            package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/emf/2002/Ecore"

            pattern name(D : java Double) = {
                EDouble(D);
                check(^java::lang::Math::abs(D) > 10.5);
            }
        ')
        model.assertNoErrors
        tester.validate(model).assertOK
    }
    
    @Test
    def whitelistedMethodCheck2() {
        val model = parseHelper.parse('
            package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/emf/2002/Ecore"

            pattern name(D : java Double) = {
                EDouble(D);
                check(Math::max(0,D) < 3);
            }
        ')
        model.assertNoErrors
        tester.validate(model).assertOK
    }
    @Test
    def whitelistedMethodCheck3() {
        val model = parseHelper.parse('
            package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/emf/2002/Ecore"

            pattern name(S : java String) = {
                EString(S);
                check(Integer.parseInt(S) < 3);
            }
        ')
        model.assertNoErrors
        tester.validate(model).assertOK
    }
    @Test
    def whitelistedMethodCheck4() {
        val model = parseHelper.parse('
            package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/emf/2002/Ecore"

            pattern name(S : java String) = {
                EString(S);
                check(S.contains("abc"));
            }
        ')
        model.assertNoErrors
        tester.validate(model).assertOK
    }
    @Test
    def whitelistedMethodCheck5() {
        val model = parseHelper.parse('
            package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/emf/2002/Ecore"

            pattern name(S : java String) = {
                EClass.name(_, name);
                S == eval(String.format("Name: %s", name));
            }
        ')
        model.assertNoErrors
        tester.validate(model).assertOK
    }
    @Test
    def whitelistedClassCheck() {
        val model = parseHelper.parse('''
            package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/emf/2002/Ecore"

            pattern name(D : java Double) = {
                EDouble(D);
                check (org::eclipse::viatra::query::patternlanguage::emf::tests::DummyClass::alwaysTrue());
            }
        ''')
        model.assertNoErrors
        tester.validate(model).assertOK
    }
    @Test
    def whitelistedImportedClassCheck() {
        val model = parseHelper.parse('''
            package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/emf/2002/Ecore"
            import java org.eclipse.viatra.query.patternlanguage.emf.tests.DummyClass

            pattern name(D : java Double) = {
                EDouble(D);
                check (DummyClass::alwaysFalse());
            }
        ''')
        model.assertNoErrors
        tester.validate(model).assertOK
    }
    @Test
    def nonwhitelistedImportedToplevelCheck() {
        val model = parseHelper.parse('''
            package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/emf/2002/Ecore"
            import java org.eclipse.viatra.query.patternlanguage.emf.tests.DummyClass2

            pattern name(D : java Double) = {
                EDouble(D);
                check (DummyClass2::alwaysFalse());
            }
        ''')
        model.assertNoErrors
        tester.validate(model).assertAll(
            getWarningCode(IssueCodes::CHECK_WITH_IMPURE_JAVA_CALLS)
        )
    }

    @Test
    def nonWhitelistedCheck() {
        val model = parseHelper.parse('
            package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/emf/2002/Ecore"

            pattern name(L : java Long) = {
                ELong(L);
                check(^java::util::Calendar::getInstance().getTime().getTime() > L);
            }
        ')
        model.assertNoErrors
        tester.validate(model).assertAll(
            getWarningCode(IssueCodes::CHECK_WITH_IMPURE_JAVA_CALLS),
            getWarningCode(IssueCodes::CHECK_WITH_IMPURE_JAVA_CALLS),
            getWarningCode(IssueCodes::CHECK_WITH_IMPURE_JAVA_CALLS)
        )
    }
    
    @Test
    def evalReturnAsParameterCheck() {
        val model = parseHelper.parse('''
            package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/emf/2002/Ecore"

            pattern evalTest(cl : EClass, name : java String) {
                EClass.name(cl, name);
                name == eval(name);
            }
        ''')
        tester.validate(model).assertAll(
            getErrorCode(IssueCodes::EVAL_INCORRECT_RETURNVALUE)
        )
    }
    
    @Test
    def evalReturnAsParameterCheck2() {
        val model = parseHelper.parse('''
            package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/emf/2002/Ecore"

            pattern evalTest(cl : EClass, name : java String) {
                EClass.name(cl, name);
                name != eval(name);
            }
        ''')
        model.assertNoErrors
        tester.validate(model).assertAll(
            getWarningCode(IssueCodes::EVAL_INCORRECT_RETURNVALUE)
        )
    }
    
    @Test
    def evalReturnAsParameterCheck3() {
        val model = parseHelper.parse('''
            package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/emf/2002/Ecore"

            pattern evalTest(cl : EClass, n : java String) {
                EClass.name(cl, name);
                n == eval(name);
            }
        ''')
        model.assertNoErrors
        tester.validate(model).assertOK
    }

    @Test
    def evalUnwindValidReturnType() {
        val model = parseHelper.parse('''
            package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/emf/2002/Ecore"

            pattern evalTest(n : java Integer) {
                n == eval unwind (newHashSet(1, 3, 5, 7));
            }
        ''')
        model.assertNoErrors
        tester.validate(model).assertOK
    }
    
    @Test
    def evalUnwindRequiresSetArgumentCheck() {
        val model = parseHelper.parse('''
            package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/emf/2002/Ecore"

            incremental pattern evalTest(n : java Integer) {
                n == eval unwind (newArrayList(1, 3, 5, 7));
            }
        ''')
        tester.validate(model).assertAll(
            getErrorCode(IssueCodes::EVAL_INCORRECT_RETURNVALUE)
        )
    }
    
    @Test
    def elementInExpressionCheck() {
        val model = parseHelper.parse('''
            package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/emf/2002/Ecore"

            pattern evalTest(cl : EClass, hash : java Integer) {
                EClass(cl);
                hash == eval(DummyClass::hashOf(cl));
            }
        ''')
        tester.validate(model).assertDiagnostic(Diagnostic.ERROR,
            IssueCodes::CHECK_CONSTRAINT_SCALAR_VARIABLE_ERROR,
            "Only simple EDataTypes are allowed in check() and eval()"
        )
    }
    
    @Test
    def elementInExpressionSuppressedCheck() {
        val model = parseHelper.parse('''
            package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/emf/2002/Ecore"

            @SafeElementInExpression
            pattern evalTest(cl : EClass, hash : java Integer) {
                EClass(cl);
                hash == eval(DummyClass::hashOf(cl));
            }
        ''')
        val results = tester.validate(model)
        results.assertDiagnostic(Diagnostic.INFO, IssueCodes::EXPERIMENTAL_ANNOTATION, "experimental")
        
        assertNotEquals(Diagnostic.ERROR, results.diagnostic.severity)
    }

}