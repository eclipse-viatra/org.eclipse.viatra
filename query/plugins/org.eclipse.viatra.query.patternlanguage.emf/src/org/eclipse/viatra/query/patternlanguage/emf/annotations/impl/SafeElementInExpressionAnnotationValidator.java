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
 * Annotation information for the <pre>SafeElementInExpression</pre> annotation for VQL.
 * @author BergmannG
 * @since 2.9
 */
public class SafeElementInExpressionAnnotationValidator extends PatternAnnotationValidator {

    public static final String ANNOTATION_NAME = "SafeElementInExpression";
    private static final String ANNOTATION_DESCRIPTION = "<b>[EXPERIMENTAL]</b> " 
            + "This annotation is experimental. It might be removed or changed in future versions.<p>"
            + "This annotation is used to record that"
            + " any expressions in this pattern that directly use a model object (as opposed to a scalar attribute value)" 
            + " have been reviewed and proven to be deterministic functions of the object reference,"
            + " and hence result in correct semantics and dynamics.";
    
    
    public SafeElementInExpressionAnnotationValidator() {
        super(ANNOTATION_NAME, ANNOTATION_DESCRIPTION);
    }

}
