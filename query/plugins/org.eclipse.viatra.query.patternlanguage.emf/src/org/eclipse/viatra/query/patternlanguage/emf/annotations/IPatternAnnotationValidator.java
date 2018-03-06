/*******************************************************************************
 * Copyright (c) 2010-2012, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Zoltan Ujhelyi - initial API and implementation
 *******************************************************************************/
package org.eclipse.viatra.query.patternlanguage.emf.annotations;

import java.util.Optional;

import org.eclipse.viatra.query.patternlanguage.emf.vql.Annotation;
import org.eclipse.viatra.query.patternlanguage.emf.vql.AnnotationParameter;
import org.eclipse.viatra.query.patternlanguage.emf.vql.ValueReference;

/**
 * An interface for validating pattern {@link Annotation} objects.
 * 
 * @author Zoltan Ujhelyi
 * @since 2.0
 * 
 */
public interface IPatternAnnotationValidator {

    Iterable<String> getMissingMandatoryAttributes(Annotation annotation);

    Iterable<AnnotationParameter> getUnknownAttributes(Annotation annotation);

    /**
     * Returns whether a parameter of an annotation is mistyped
     * 
     * @param parameter
     * @return the expected class of the parameter variable
     */
    Class<? extends ValueReference> getExpectedParameterType(AnnotationParameter parameter);

    Iterable<String> getAllAvailableParameterNames();

    String getAnnotationName();

    String getDescription();

    String getDescription(String parameterName);

    boolean isDeprecated();

    boolean isDeprecated(String parameterName);

    /**
     * Provides an additional validator implementation.
     * 
     * @return the validator object
     * @since 2.0.0
     */
    Optional<IPatternAnnotationAdditionalValidator> getAdditionalValidator();
}
