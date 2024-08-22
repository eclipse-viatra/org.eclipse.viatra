/*******************************************************************************
 * Copyright (c) 2010-2024, BergmannG, IncQuery Labs
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.patternlanguage.emf.tests.generator

import org.junit.rules.TestName
import org.junit.Rule
import org.junit.Test
import org.eclipse.emf.ecore.EcorePackage

class TypingEdgeCasesCompilerTest extends AbstractQueryCompilerTest {
    static val TEST_PROJECT_NAME_PREFIX = "org.eclipse.viatra.query.test"
    

    static val TEST_CONTENTS_GENERIC = '''
        package test
        
        import "http://www.eclipse.org/emf/2002/Ecore"
        
        pattern foo(e: java ^java.util.Map, u: java Integer) {
            e == eval(^java.util.Collections.singletonMap(1,2));
            u == 4;
        }
    '''

    
    static val TEST_CONTENTS_INNERCLASS = '''
        package test
        
        import "http://www.eclipse.org/emf/2002/Ecore"
        
        pattern bar(e: java ^org.eclipse.emf.ecore.EcorePackage.Literals, u: java Integer) {
            e == eval(null as org.eclipse.emf.ecore.EcorePackage.Literals);
            u == 4;
        }
    '''

    
    static val TEST_CONTENTS_GENERIC_INNERCLASS = '''
        package test
        
        import "http://www.eclipse.org/emf/2002/Ecore"
        
        pattern baz(e: java ^java.util.Map.Entry, u: java Integer) {
            e == eval(^java.util.Collections.singletonMap(1,2).entrySet.head);
            u == 4;
        }
    '''
      
    static val TEST_CONTENTS_GENERIC_INNERCLASS_INFERRED = '''
        package test
        
        import "http://www.eclipse.org/emf/2002/Ecore"
        
        pattern bang(e, u: java Integer) {
            e == eval(^java.util.Collections.singletonMap(1,2).entrySet.head);
            u == 4;
        }
    '''
    static val TEST_CONTENTS_JAVACONST = '''
        package test
        
        import "http://www.eclipse.org/emf/2002/Ecore"
        
        pattern fee(e: java org.eclipse.emf.ecore.EcorePackage, u: java Integer) {
            e == java org.eclipse.emf.ecore.EcorePackage::eINSTANCE;
            u == 4;
        }
    '''
    static val TEST_CONTENTS_JAVACONST_INFERRED = '''
        package test
        
        import "http://www.eclipse.org/emf/2002/Ecore"
        
        pattern fie(e, u: java Integer) {
            e == java org.eclipse.emf.ecore.EcorePackage::eINSTANCE;
            u == 4;
        }
    '''
    static val TEST_CONTENTS_JAVACONST_ENUM = '''
        package test
        
        import "http://www.eclipse.org/emf/2002/Ecore"
        
        pattern fee(e: java org.eclipse.viatra.query.runtime.matchers.backend.QueryEvaluationHint.BackendRequirement, u: java Integer) {
            e == java org.eclipse.viatra.query.runtime.matchers.backend.QueryEvaluationHint.BackendRequirement::SPECIFIC;
            u == 4;
        }
    '''
    static val TEST_CONTENTS_JAVACONST_ENUM_INFERRED = '''
        package test
        
        import "http://www.eclipse.org/emf/2002/Ecore"
        
        pattern fie(e, u: java Integer) {
            e == java org.eclipse.viatra.query.runtime.matchers.backend.QueryEvaluationHint.BackendRequirement::SPECIFIC;
            u == 4;
        }
    '''


    @Rule
    public val name = new TestName
    
    override String calculateTestProjectName() {
        TEST_PROJECT_NAME_PREFIX + "." + name.methodName
    }
    
    @Test
    def void compileGenericClass() {
        testFileCreationAndBuild(TEST_CONTENTS_GENERIC, 0)
    }
    @Test
    def void compileInnerClass() {
        testFileCreationAndBuild(TEST_CONTENTS_INNERCLASS, 0)
    }
    @Test
    def void compileGenericInnerClass() {
        testFileCreationAndBuild(TEST_CONTENTS_GENERIC_INNERCLASS, 0)
    }
    @Test
    def void compileGenericInnerClassInferred() {
        testFileCreationAndBuild(TEST_CONTENTS_GENERIC_INNERCLASS_INFERRED, 0)
    }
    @Test
    def void compileJavaconst() {
        testFileCreationAndBuild(TEST_CONTENTS_JAVACONST, 0)
    }
    @Test
    def void compileJavaconstInferred() {
        testFileCreationAndBuild(TEST_CONTENTS_JAVACONST_INFERRED, 0)
    }
    @Test
    def void compileJavaconstEnum() {
        testFileCreationAndBuild(TEST_CONTENTS_JAVACONST_ENUM, 0)
    }
    @Test
    def void compileJavaconstEnumInferred() {
        testFileCreationAndBuild(TEST_CONTENTS_JAVACONST_ENUM_INFERRED, 0)
    }
}