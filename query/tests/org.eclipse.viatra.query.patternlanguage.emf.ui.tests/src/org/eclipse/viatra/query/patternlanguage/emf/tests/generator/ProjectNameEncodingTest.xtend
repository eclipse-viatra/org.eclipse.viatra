/*******************************************************************************
 * Copyright (c) 2010-2024, Gábor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.patternlanguage.emf.tests.generator

import org.eclipse.core.resources.IFile
import org.eclipse.core.runtime.Path
import org.junit.Test

/**
 * Test case for https://github.com/eclipse-viatra/org.eclipse.viatra/issues/154
 */
class ProjectNameEncodingTest extends AbstractQueryCompilerTest {
    
    override calculateTestProjectName() {
        "This project is for testing spaces in project names"
    }
    
    @Test
    def void compileParameterlessPattern() {
        val testFile = testFileCreationAndBuild('''
        package test
        
        import "http://www.eclipse.org/emf/2002/Ecore"
        
        pattern testPattern() {
            EClass(x);
        }
        ''', 1 /* Bundle symbolic name contains illegal characters.  */)
        
        val srcGen = testFile.project.getFolder(new Path("src-gen"))
        
        val foundJavaFiles = <IFile>newArrayList
        srcGen.accept[proxy | if ("java" == proxy.fileExtension) foundJavaFiles += proxy as IFile; true ]
        
        assertEquals(2, foundJavaFiles.size)
        assertTrue(foundJavaFiles.exists[name == "Test.java"])
        assertTrue(foundJavaFiles.exists[name == "TestPattern.java"])
        
    }
    
}