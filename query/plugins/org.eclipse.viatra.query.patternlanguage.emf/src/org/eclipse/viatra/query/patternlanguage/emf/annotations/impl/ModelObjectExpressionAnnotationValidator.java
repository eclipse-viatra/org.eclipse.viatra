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
public class ModelObjectExpressionAnnotationValidator extends PatternAnnotationValidator {
    
    public static final String ANNOTATION_ID = "SafeModelObjectExpression";
    private static final String ANNOTATION_DESCRIPTION = "EXPERT USERS ONLY. EXPERIMENTAL ANNOTATION, support may be removed from subsequent versions without notice. " +
            "Patterns annotated with this are permitted to use model object variables in check() or eval() expressions, in addition to scalar attribute variables. " +
            "WARNING: in this case static analysis cannot prove that the expression is deterministic (always yields the same result for the same variable substitution, even if the model is changed); " + 
            "the query author is burdened with proving that the model objects are only processed in ways that yield a constant, deterministic result irrespective of model or environment changes.";
    
    
    public ModelObjectExpressionAnnotationValidator() {
        super(ANNOTATION_ID, ANNOTATION_DESCRIPTION, /*deprecated=*/true);
    }

}
