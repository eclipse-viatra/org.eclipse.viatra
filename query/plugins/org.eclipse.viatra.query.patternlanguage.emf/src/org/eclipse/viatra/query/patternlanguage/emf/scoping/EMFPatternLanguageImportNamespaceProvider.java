/*******************************************************************************
 * Copyright (c) 2010-2014, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Zoltan Ujhelyi - initial API and implementation
 *******************************************************************************/
package org.eclipse.viatra.query.patternlanguage.emf.scoping;

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.viatra.query.patternlanguage.emf.vql.PatternImport;
import org.eclipse.viatra.query.patternlanguage.emf.vql.VQLImportSection;
import org.eclipse.viatra.query.patternlanguage.emf.helper.PatternLanguageHelper;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.impl.ImportNormalizer;
import org.eclipse.xtext.xbase.scoping.XImportSectionNamespaceScopeProvider;
import org.eclipse.xtext.xtype.XImportSection;

import com.google.common.collect.Lists;

/**
 * @author Zoltan Ujhelyi
 * 
 */
@SuppressWarnings("restriction")
public class EMFPatternLanguageImportNamespaceProvider extends XImportSectionNamespaceScopeProvider {

    private static final QualifiedName VIATRA_AGGREGATORS = QualifiedName.create("org","eclipse","viatra","query","runtime","matchers","aggregators");
    
    /**
     * @since 1.4
     */
    @Override
    protected List<ImportNormalizer> getImplicitImports(boolean ignoreCase) {
        return Lists.<ImportNormalizer>newArrayList(
                doCreateImportNormalizer(JAVA_LANG, true, false),
                doCreateImportNormalizer(XBASE_LIB, true, false),
                doCreateImportNormalizer(VIATRA_AGGREGATORS, true, false));
    }
    
    @Override
    protected IScope internalGetScope(IScope parent, IScope globalScope, EObject context, EReference reference) {
        if (context instanceof PatternImport) {
            return globalScope;
        }
        return super.internalGetScope(parent, globalScope, context, reference);
    }

    @Override
    protected List<ImportNormalizer> getImportedNamespaceResolvers(XImportSection importSection, boolean ignoreCase) {
        List<ImportNormalizer> parentNormalizers = super.getImportedNamespaceResolvers(importSection, ignoreCase);
        List<PatternImport> patternImportDeclarations;
        if (importSection instanceof VQLImportSection) {
            patternImportDeclarations = ((VQLImportSection) importSection).getPatternImport();
        } else {
            patternImportDeclarations = Lists.newArrayList();
        }
        List<ImportNormalizer> result = Lists.newArrayListWithExpectedSize(patternImportDeclarations.size()
                + parentNormalizers.size());
        for (PatternImport imp : patternImportDeclarations) {
            ImportNormalizer resolver = createImportedNamespaceResolver(
                    PatternLanguageHelper.getFullyQualifiedName(imp.getPattern()), ignoreCase);
            if (resolver != null) {
                result.add(resolver);
            }
        }
        result.addAll(parentNormalizers);
        return result;
    }
}
