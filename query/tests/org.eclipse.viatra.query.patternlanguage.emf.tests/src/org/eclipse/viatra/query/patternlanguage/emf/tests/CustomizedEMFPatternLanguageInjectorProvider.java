/*******************************************************************************
 * Copyright (c) 2010-2017, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * This file was generated from EMFPatternLanguage.xtext
 *******************************************************************************/
package org.eclipse.viatra.query.patternlanguage.emf.tests;

import org.eclipse.viatra.query.patternlanguage.emf.EMFPatternLanguageRuntimeModule;
import org.eclipse.viatra.query.patternlanguage.emf.annotations.ExtensionBasedAnnotationValidatorLoader;
import org.eclipse.viatra.query.patternlanguage.emf.annotations.IAnnotationValidatorLoader;
import org.eclipse.viatra.query.patternlanguage.emf.validation.whitelist.EclipseExtensionBasedWhitelistProvider;
import org.eclipse.viatra.query.patternlanguage.emf.validation.whitelist.IPureWhitelistExtensionProvider;

public class CustomizedEMFPatternLanguageInjectorProvider extends EMFPatternLanguageInjectorProvider {

    @Override
    protected EMFPatternLanguageRuntimeModule createRuntimeModule() {
		// make it work also with Maven/Tycho and OSGI
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=493672
		return new EMFPatternLanguageRuntimeModule() {
			@Override
			public ClassLoader bindClassLoaderToInstance() {
				return CustomizedEMFPatternLanguageInjectorProvider.class
						.getClassLoader();
			}

            @Override
            public Class<? extends IPureWhitelistExtensionProvider> bindIPureWhitelistExtensionProvider() {
                return EclipseExtensionBasedWhitelistProvider.class;
            }
			
            @Override
            public Class<? extends IAnnotationValidatorLoader> bindAnnotationValidatorLoader() {
                return ExtensionBasedAnnotationValidatorLoader.class;
            }
		};
	}
}
