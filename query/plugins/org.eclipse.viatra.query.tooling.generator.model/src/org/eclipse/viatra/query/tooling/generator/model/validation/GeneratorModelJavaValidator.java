/*******************************************************************************
 * Copyright (c) 2010-2012, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.tooling.generator.model.validation;

import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.viatra.query.tooling.generator.model.generatorModel.GeneratorModelPackage;
import org.eclipse.viatra.query.tooling.generator.model.generatorModel.GeneratorModelReference;
import org.eclipse.xtext.validation.Check;

public class GeneratorModelJavaValidator extends AbstractGeneratorModelJavaValidator {

    private static final String OVERRIDE_MESSAGE = "The genmodel import overrides the EPackage %s from the EMF EPackage registry. Be careful as this might cause issues with the interpretative tooling.";

    @Check
    public void checkPackageOverride(GeneratorModelReference reference) {
        if (reference.getGenmodel() == null || reference.getGenmodel().getGenPackages().isEmpty()) {
            return;
        }
        org.eclipse.emf.ecore.EPackage.Registry registry = EPackage.Registry.INSTANCE;
        for (GenPackage genPackage : reference.getGenmodel().getGenPackages()) {
            EPackage ePackage = genPackage.getEcorePackage();
            String nsURI = ePackage.getNsURI();
            if (registry.containsKey(nsURI)) {
                warning(String.format(OVERRIDE_MESSAGE, nsURI),
                        GeneratorModelPackage.Literals.GENERATOR_MODEL_REFERENCE__GENMODEL,
                        GeneratorModelIssueCodes.PACKAGE_OVERRIDE_CODE);
            }
        }

    }
}
