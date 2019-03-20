/*******************************************************************************
 * Copyright (c) 2010-2015, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.patternlanguage.emf.helper;

import org.eclipse.emf.codegen.ecore.genmodel.GenClass;
import org.eclipse.emf.codegen.ecore.genmodel.GenClassifier;
import org.eclipse.emf.codegen.ecore.genmodel.GenEnum;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.xtext.common.types.JvmTypeReference;

import com.google.common.base.Strings;

/**
 * A helper class for operations over EMF Generator models
 * 
 * @author Zoltan Ujhelyi
 *
 */
public class GeneratorModelHelper {

    private GeneratorModelHelper() {}
    
    /**
     * Resolves the {@link Variable} using information from the {@link GenPackage}. Tries to find an appropriate
     * {@link GenClass} for the {@link EClassifier}. If one is found, then returns a {@link JvmTypeReference} for it's
     * qualified interface name.
     */
    public static String resolveTypeReference(GenPackage genPackage, EClassifier classifier) {
        GenClassifier genClassifier = findGenClassifier(genPackage, classifier);
        String className = null;
        if (!Strings.isNullOrEmpty(classifier.getInstanceClassName())) {
            className = classifier.getInstanceClassName();
        } else if (genClassifier instanceof GenClass) {
            className = ((GenClass) genClassifier).getQualifiedInterfaceName();
        } else if (genClassifier instanceof GenEnum) {
            className = ((GenEnum) genClassifier).getQualifiedInstanceClassName();
        } else {
            //At this point, no corresponding genpackage declaration was found; creating default type
            className = genPackage.getInterfacePackageName() + "." + classifier.getName();
        }
        return className;
    }

    private static GenClassifier findGenClassifier(GenPackage genPackage, EClassifier classifier) {
        for (GenClassifier genClassifier : genPackage.getGenClassifiers()) {
            if (classifier.equals(genClassifier.getEcoreClassifier())) {
                return genClassifier;
            }
        }
        return null;
    }

}
