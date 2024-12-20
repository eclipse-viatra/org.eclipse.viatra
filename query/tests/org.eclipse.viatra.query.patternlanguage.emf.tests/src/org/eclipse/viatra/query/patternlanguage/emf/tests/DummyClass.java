/*******************************************************************************
 * Copyright (c) 2010-2016, Zoltan Ujhelyi and IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.patternlanguage.emf.tests;

public class DummyClass {

    private DummyClass(){}
    
    public static boolean alwaysTrue() {
        return true;
    }
    
    public static boolean alwaysFalse() {
        return false;
    }
    
    public static int hashOf(Object o) {
        return o.hashCode();
    }
}
