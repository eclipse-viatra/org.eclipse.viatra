/*******************************************************************************
 * Copyright (c) 2010-2023, Gabor Bergmann, IncQuery Labs
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.patternlanguage.emf.annotations.impl;

import org.eclipse.viatra.query.patternlanguage.emf.annotations.PatternAnnotationValidator;

/**
 * @author BergmannG
 * @since 2.8
 *
 */
public class NegativeRecursionAnnotationValidator extends PatternAnnotationValidator {
    
    public static final String ANNOTATION_ID = "SafeNegativeRecursion";
    private static final String ANNOTATION_DESCRIPTION = "EXPERT USERS ONLY. Patterns annotated with this are permitted to recurse through negative calls. " +
            "WARNING: in this case static analysis cannot prove that the recursion is stratifiable (safe to compute); " + 
            "the query author is burdened with proving that there is no contradictory recursion between tuples.";
    
    
    public NegativeRecursionAnnotationValidator() {
        super(ANNOTATION_ID, ANNOTATION_DESCRIPTION);
    }

}
