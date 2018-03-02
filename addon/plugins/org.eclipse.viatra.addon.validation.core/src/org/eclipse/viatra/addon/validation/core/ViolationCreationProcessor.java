/*******************************************************************************
 * Copyright (c) 2010-2014, Balint Lorand, Zoltan Ujhelyi, Abel Hegedus, Tamas Szabo, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Zoltan Ujhelyi, Abel Hegedus, Tamas Szabo - original initial API and implementation
 *   Balint Lorand - revised API and implementation
 *******************************************************************************/

package org.eclipse.viatra.addon.validation.core;

import java.util.Map;
import java.util.function.Consumer;

import org.apache.log4j.Logger;
import org.eclipse.viatra.addon.validation.core.violationkey.ViolationKey;
import org.eclipse.viatra.query.runtime.api.IPatternMatch;
import org.eclipse.viatra.query.runtime.emf.helper.ViatraQueryRuntimeHelper;

/**
 * The job is used to process retrieved matches and create violations upon listing on a constraint.
 * 
 * @author Balint Lorand
 * 
 */
public class ViolationCreationProcessor implements Consumer<IPatternMatch> {

    private Constraint constraint;
    private Map<ViolationKey, Violation> violationMap;
    private Logger logger;

    public ViolationCreationProcessor(Constraint constraint, Logger logger, Map<ViolationKey, Violation> violationMap) {
        this.constraint = constraint;
        this.logger = logger;
        this.violationMap = violationMap;
    }

    /**
     * @since 2.0
     */
    @Override
    public void accept(IPatternMatch match) {

        Map<String, Object> keyObjectMap = constraint.getSpecification().getKeyObjects(match);

        if (!keyObjectMap.isEmpty()) {
            violationMap.computeIfAbsent(constraint.getViolationKey(match), key -> {
                Violation violation = new Violation();
                violation.setConstraint(constraint);
                violation.setKeyObjects(constraint.getSpecification().getKeyObjects(match));
                violation.setMessage(ViatraQueryRuntimeHelper.getMessage(match, constraint.getSpecification()
                        .getMessageFormat()));
                violation.addMatch(match);
                return violation;
            });

        } else {
            logger.error("Error getting Violation key objects!");
        }
    }
}
