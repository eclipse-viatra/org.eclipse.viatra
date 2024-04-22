/*******************************************************************************
 * Copyright (c) 2010-2024, BergmannG, IncQuery Labs
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.patternlanguage.emf.annotations.impl;

import org.eclipse.viatra.query.patternlanguage.emf.annotations.PatternAnnotationValidator;

/**
 * Annotation information for the <pre>SafeRecursion</pre> annotation for VQL.
 * @author BergmannG
 * @since 2.9
 */
public class SafeRecursionAnnotationValidator extends PatternAnnotationValidator {

    
    public static final String ANNOTATION_NAME = "SafeRecursion";
    private static final String ANNOTATION_DESCRIPTION = "<b>[EXPERIMENTAL] [EXPERT USERS ONLY]</b> " 
            + "This annotation is for expert users only; misuse may break correctness guarantees.<p>"
            + "Patterns decorated with this annotation are permitted to recurse through"
            + " negative, aggregating or transitive pattern calls. <p>"
            + "This annotation is a record of the promise that"
            + " any negative, transitive or aggregating pattern calls in this pattern that cause a recursive loop among patterns" 
            + " have been manually reviewed and proven by the query author to result in correct semantics and dynamics, i.e."
            + " that there is no contradictory recursion between individual tuples.";
    
    
    public SafeRecursionAnnotationValidator() {
        super(ANNOTATION_NAME, ANNOTATION_DESCRIPTION);
    }

}
