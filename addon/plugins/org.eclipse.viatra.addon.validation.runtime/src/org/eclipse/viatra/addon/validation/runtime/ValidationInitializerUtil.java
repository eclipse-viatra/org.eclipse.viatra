/*******************************************************************************
 * Copyright (c) 2010-2015, Abel Hegedus, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.addon.validation.runtime;

import org.apache.log4j.Logger;
import org.eclipse.viatra.addon.validation.core.ValidationEngine;
import org.eclipse.viatra.addon.validation.core.api.IConstraintSpecification;
import org.eclipse.viatra.addon.validation.core.api.IValidationEngine;
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngine;
import org.eclipse.viatra.query.runtime.api.scope.QueryScope;
import org.eclipse.viatra.query.runtime.util.ViatraQueryLoggingUtil;

/**
 * @author Abel Hegedus
 *
 */
public final class ValidationInitializerUtil {

    private ValidationInitializerUtil() {}
    
    /**
     * Initializes a new validation engine implementing the IValidationEngine interface on the provided Notifier
     * instance with the constrains specified for the given editor Id.
     * 
     * @param scope
     *            The Notifier object on which the validation engine should be initialized.
     * @param editorId
     *            An editor Id for which we wish to use the registered constraint specifications at the
     *            org.eclipse.viatra.addon.livevalidation.runtime.constraintspecification extension point.
     * @return The initialized validation engine.
     */
    public static IValidationEngine initializeValidationWithRegisteredConstraintsOnScope(QueryScope scope,
            String editorId) {
        ViatraQueryEngine engine = ViatraQueryEngine.on(scope);
        Logger logger = ViatraQueryLoggingUtil.getLogger(ValidationEngine.class);
        IValidationEngine validationEngine = ValidationEngine.builder().setEngine(engine).setLogger(logger).build();
    
        for (IConstraintSpecification constraintSpecification : ConstraintExtensionRegistry.getConstraintSpecificationsForEditorId(editorId)) {
            validationEngine.addConstraintSpecification(constraintSpecification);
        }
        validationEngine.initialize();
    
        return validationEngine;
    }

}
