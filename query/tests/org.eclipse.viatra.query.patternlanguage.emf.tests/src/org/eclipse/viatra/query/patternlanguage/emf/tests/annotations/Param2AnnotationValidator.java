/*******************************************************************************
 * Copyright (c) 2010-2018, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.patternlanguage.emf.tests.annotations;

import org.eclipse.viatra.query.patternlanguage.emf.annotations.PatternAnnotationParameter;
import org.eclipse.viatra.query.patternlanguage.emf.annotations.PatternAnnotationValidator;

public class Param2AnnotationValidator extends PatternAnnotationValidator {

    private static final PatternAnnotationParameter P1_PARAM = new PatternAnnotationParameter("p1",
            /*type*/ null,
            /*desc*/ null,
            /*multiple*/ false,
            /*mandatory*/ true);
    
    private static final PatternAnnotationParameter P2_PARAM = new PatternAnnotationParameter("p2",
            /*type*/ null,
            /*desc*/ null,
            /*multiple*/ false,
            /*mandatory*/ false);
    
    public Param2AnnotationValidator() {
        super("Param2", "", P1_PARAM, P2_PARAM);
    }

}
