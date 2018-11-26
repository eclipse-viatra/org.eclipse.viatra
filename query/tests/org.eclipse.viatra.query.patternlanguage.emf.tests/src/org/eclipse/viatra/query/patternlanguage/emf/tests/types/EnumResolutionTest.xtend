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

package org.eclipse.viatra.query.patternlanguage.emf.tests.types

import org.eclipse.xtext.testing.XtextRunner
import org.junit.runner.RunWith
import org.eclipse.xtext.testing.InjectWith
import com.google.inject.Inject
import org.eclipse.xtext.testing.util.ParseHelper
import org.eclipse.xtext.testing.validation.ValidationTestHelper
import org.junit.Test
import org.eclipse.viatra.query.patternlanguage.emf.vql.PatternModel
import static org.junit.Assert.*
import org.eclipse.viatra.query.patternlanguage.emf.vql.PatternLanguagePackage
import org.eclipse.xtext.diagnostics.Diagnostic
import org.eclipse.viatra.query.patternlanguage.emf.vql.PathExpressionConstraint
import org.eclipse.viatra.query.patternlanguage.emf.vql.EnumValue
import org.eclipse.emf.codegen.ecore.genmodel.GenModelPackage
import org.eclipse.viatra.query.patternlanguage.emf.tests.CustomizedEMFPatternLanguageInjectorProvider
import org.eclipse.viatra.query.patternlanguage.emf.helper.PatternLanguageHelper

@RunWith(typeof(XtextRunner))
@InjectWith(typeof(CustomizedEMFPatternLanguageInjectorProvider))
class EnumResolutionTest {
    @Inject
    ParseHelper<PatternModel> parseHelper
    
    @Inject extension ValidationTestHelper
    
    @Test
    def eEnumResolutionSuccess() {
        val model = parseHelper.parse('
            package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/emf/2002/GenModel"

            pattern resolutionTest(Model) = {
                GenModel(Model);
                GenModel.runtimeVersion(Model, ::EMF23);
            }
        ')
        model.assertNoErrors
        val pattern = model.patterns.get(0)
        val constraint = pattern.bodies.get(0).constraints.get(1) as PathExpressionConstraint
        val type = PatternLanguageHelper.getPathExpressionTailType(constraint).get
        assertEquals(type.refname.EType, GenModelPackage$Literals::GEN_RUNTIME_VERSION)
        val value = constraint.dst as EnumValue
        assertEquals(value.literal, GenModelPackage$Literals::GEN_RUNTIME_VERSION.getEEnumLiteral("EMF23"))		
    }
    
    @Test
    def eQualifiedEnumResolutionSuccess() {
        val model = parseHelper.parse('
            package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/emf/2002/GenModel"

            pattern resolutionTest(Model) = {
                GenModel.runtimeVersion(Model, GenRuntimeVersion::EMF23);
            }
        ')
        model.assertNoErrors
        val pattern = model.patterns.get(0)
        val constraint = pattern.bodies.get(0).constraints.get(0) as PathExpressionConstraint
        val type = PatternLanguageHelper.getPathExpressionEMFTailType(constraint).get
        assertEquals(type, GenModelPackage$Literals::GEN_RUNTIME_VERSION)
        val value = constraint.dst as EnumValue
        assertEquals(value.literal, GenModelPackage$Literals::GEN_RUNTIME_VERSION.getEEnumLiteral("EMF23"))		
    }
    
    @Test
    def eEnumResolutionMissingLiteral() {
        val model = parseHelper.parse('''
            package org.eclipse.viatra.query.patternlanguage.emf.tests
           import "http://www.eclipse.org/emf/2002/GenModel"

            pattern resolutionTest(Model : GenModel) = {
                GenModel.runtimeVersion(Model, GenRuntimeVersion::);
            }
        ''')
        model.assertError(PatternLanguagePackage$Literals::PATH_EXPRESSION_CONSTRAINT,
            Diagnostic::SYNTAX_DIAGNOSTIC, ")")    
    }
    
    @Test
    def eEnumResolutionInvalidLiteral() {
        val model = parseHelper.parse('
            package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/emf/2002/GenModel"

            pattern resolutionTest(Model) = {
                GenModel(Model);
                GenModel.runtimeVersion(Model, ::NOTEXIST);
            }
        ')
        model.assertError(PatternLanguagePackage$Literals::ENUM_VALUE,
            Diagnostic::LINKING_DIAGNOSTIC, "NOTEXIST")
    }
    @Test
    def eEnumResolutionNotEnum() {
        val model = parseHelper.parse('
            package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/emf/2002/GenModel"

            pattern resolutionTest(Model) = {
                GenModel(Model);
                GenModel.copyrightText(Model, ::EMF23);
            }
        ')
        //XXX With better type inference this error message should be replaced
        model.assertError(PatternLanguagePackage$Literals::ENUM_VALUE,
            Diagnostic::LINKING_DIAGNOSTIC, "EMF23")
    }
        
    @Test
    def eEnumResolutionMissingQualifier() {
        val model = parseHelper.parse('
            package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/emf/2002/GenModel"


            pattern runtimeVersion(Version) = {
                GenRuntimeVersion(Version);
            }

            pattern call() = {
                find runtimeVersion(::EMF24);
            }
        ')
        model.assertError(PatternLanguagePackage$Literals::ENUM_VALUE,
            Diagnostic::LINKING_DIAGNOSTIC, "EMF24")
    }
    
    @Test
    def validateIncorrectEnumWithEquality() {
        val model = parseHelper.parse('
            package org.eclipse.viatra.query.patternlanguage.emf.tests
            import "http://www.eclipse.org/emf/2002/GenModel"

            pattern resolutionTest(Model) = {
                GenModel(Model);
                GenModel.runtimeVersion(Model, Version);
                Version == ::EMF23;
            }
        ')
        model.assertError(PatternLanguagePackage$Literals::ENUM_VALUE,
            Diagnostic::LINKING_DIAGNOSTIC, "EMF23")
    }
    
}