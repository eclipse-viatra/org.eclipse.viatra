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
 * Annotation information for the <pre>SafeNegativeRecursion</pre> annotation for VQL.
 * @author BergmannG
 * @since 2.9
 */
public class SafeNegativeRecursionAnnotationValidator extends PatternAnnotationValidator {

    
    public static final String ANNOTATION_NAME = "SafeNegativeRecursion";
    private static final String ANNOTATION_DESCRIPTION = "This annotation is used to record that"
            + " any negative, transitive or aggregating pattern calls in this pattern that cause a recursive loop among patterns" 
            + " have been manually reviewed and proven to result in correct semantics and dynamics.";
    
    
    public SafeNegativeRecursionAnnotationValidator() {
        super(ANNOTATION_NAME, ANNOTATION_DESCRIPTION);
    }

}
