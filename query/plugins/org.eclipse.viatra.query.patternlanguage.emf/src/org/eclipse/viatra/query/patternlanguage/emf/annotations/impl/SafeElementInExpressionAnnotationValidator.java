/*******************************************************************************
 * Copyright (c) 2010-2024, BergmannG, IncQuery Labs
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.patternlanguage.emf.annotations.impl;

import java.util.Optional;

import org.eclipse.viatra.query.patternlanguage.emf.annotations.IPatternAnnotationAdditionalValidator;
import org.eclipse.viatra.query.patternlanguage.emf.annotations.PatternAnnotationValidator;
import org.eclipse.viatra.query.patternlanguage.emf.validation.IIssueCallback;
import org.eclipse.viatra.query.patternlanguage.emf.validation.IssueCodes;
import org.eclipse.viatra.query.patternlanguage.emf.vql.Annotation;
import org.eclipse.viatra.query.patternlanguage.emf.vql.PatternLanguagePackage;

/**
 * Annotation information for the <pre>SafeElementInExpression</pre> annotation for VQL.
 * @author BergmannG
 * @since 2.9
 */
public class SafeElementInExpressionAnnotationValidator extends PatternAnnotationValidator {

    public static final String ANNOTATION_NAME = "SafeElementInExpression";
    private static final String ANNOTATION_DESCRIPTION = ExperimentalAnnotations.EXPERIMENTAL_ANNOTATION_FORMATTED_DISCLAIMER
            + "This annotation is for expert users only; misuse may break correctness guarantees.<p>"
            + "Patterns decorated with this annotation are permitted to use model object variables in" 
            + " check() or eval() expressions, in addition to scalar attribute variables. <p>"
            + "This annotation is a record of the promise that"
            + " any expressions in this pattern that directly use a model object (as opposed to a scalar attribute value)" 
            + " have been reviewed and proven by the query author to be deterministic functions of the object reference"
            + " (always yield the same result for the same variable substitution, even if the model is changed),"
            + " and hence result in correct semantics and dynamics.";
    
    
    public SafeElementInExpressionAnnotationValidator() {
        super(ANNOTATION_NAME, ANNOTATION_DESCRIPTION);
    }
    
    @Override
    public Optional<IPatternAnnotationAdditionalValidator> getAdditionalValidator() {
        return Optional.of((Annotation annotation, IIssueCallback validator) -> 
            validator.info(
                    ExperimentalAnnotations.EXPERIMENTAL_ANNOTATION_MESSAGE, 
                    annotation, PatternLanguagePackage.Literals.ANNOTATION__NAME, 
                    IssueCodes.EXPERIMENTAL_ANNOTATION
            )
        );
    }

}
