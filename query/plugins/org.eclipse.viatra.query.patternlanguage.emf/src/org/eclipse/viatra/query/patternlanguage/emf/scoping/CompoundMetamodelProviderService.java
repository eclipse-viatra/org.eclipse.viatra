/*******************************************************************************
 * Copyright (c) 2010-2015, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.patternlanguage.emf.scoping;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.scoping.IScope;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * A new, delegating metamodel provider that can handle multiple different {@link IMetamodelProviderInstance}
 * implementations, and sorts them based on priority.
 * 
 * @author Zoltan Ujhelyi
 * @since 1.0
 */
@Singleton
public class CompoundMetamodelProviderService implements IMetamodelProvider {

    private List<IMetamodelProviderInstance> sortedProviders;

    @Inject
    public CompoundMetamodelProviderService(Set<IMetamodelProviderInstance> providers) {
        sortedProviders = Lists.newArrayList(providers);
        Collections.sort(sortedProviders, (o1, o2) -> o1.getPriority() - o2.getPriority());
    }

    @Override
    public IScope getAllMetamodelObjects(IScope delegateScope, EObject context) {
        IScope calculatedScope = delegateScope;
        for (IMetamodelProviderInstance instance : sortedProviders) {
            calculatedScope = instance.getAllMetamodelObjects(calculatedScope, context);
        }
        return calculatedScope;
    }

    @Override
    public EPackage loadEPackage(String uri, ResourceSet resourceSet) {
        EPackage ePackage = null;
        Iterator<IMetamodelProviderInstance> it = sortedProviders.iterator();
        while (ePackage == null && it.hasNext()) {
            ePackage = it.next().loadEPackage(uri, resourceSet);
        }
        return ePackage;
    }

    @Override
    public boolean isGeneratedCodeAvailable(EPackage ePackage, ResourceSet set) {
        boolean codeFound = false;
        Iterator<IMetamodelProviderInstance> it = sortedProviders.iterator();
        while (!codeFound && it.hasNext()) {
            codeFound = it.next().isGeneratedCodeAvailable(ePackage, set);
        }
        return codeFound;
    }

    /**
     * @since 1.5
     */
    @Override
    public String getModelPluginId(EPackage ePackage, ResourceSet set) {
        String idFound = null;
        Iterator<IMetamodelProviderInstance> it = sortedProviders.iterator();
        while (idFound == null && it.hasNext()) {
            idFound = it.next().getModelPluginId(ePackage, set);
        }
        return idFound;
    }
    
    @Override
    public String getQualifiedClassName(EClassifier classifier, EObject context) {
        String fqn = null;
        Iterator<IMetamodelProviderInstance> it = sortedProviders.iterator();
        while (Strings.isNullOrEmpty(fqn) && it.hasNext()) {
            fqn = it.next().getQualifiedClassName(classifier, context);
        }
        return fqn;
    }
}
