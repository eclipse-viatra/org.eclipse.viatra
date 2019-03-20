/*******************************************************************************
 * Copyright (c) 2010-2012, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.patternlanguage.emf.scoping;

import java.util.Collection;
import java.util.Set;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.ResourceSet;

import com.google.common.collect.Sets;
import com.google.inject.Singleton;

@Singleton
public class MetamodelProviderService extends BaseMetamodelProviderService implements IMetamodelProviderInstance {

    @Override
    public String getIdentifier() {
        return "registry";
    }

    @Override
    public int getPriority() {
        return 1;
    }
    
    @Override
    public EPackage loadEPackage(String packageUri, ResourceSet resourceSet) {
        if (EPackage.Registry.INSTANCE.containsKey(packageUri)) {
            return EPackage.Registry.INSTANCE.getEPackage(packageUri);
        } else {
            return null;
        }
    }

    @Override
    protected Collection<String> getProvidedMetamodels() {
            Set<String> nsURISet = Sets.newHashSet(EPackage.Registry.INSTANCE.keySet());
            return nsURISet;
        
    }

    @Override
    protected String doGetQualifiedClassName(EClassifier classifier, EObject context) {
        return null;
    }
    
}
