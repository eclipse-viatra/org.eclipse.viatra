/*******************************************************************************
 * Copyright (c) 2010-2012, Zoltan Ujhelyi, Mark Czotter, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.viatra.query.tooling.core.generator.fragments;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.viatra.query.patternlanguage.emf.vql.Annotation;
import org.eclipse.viatra.query.patternlanguage.emf.vql.Pattern;
import org.eclipse.viatra.query.tooling.core.project.ProjectGenerationHelper;
import org.eclipse.xtext.xbase.lib.StringExtensions;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;

/**
 * A provider for {@link IGenerationFragment} classes - the fragment list is populated using the registered extensions
 * for the {@value #EXTENSIONID} extension point.
 * 
 * @author Zoltan Ujhelyi
 * 
 */
public class ExtensionBasedGenerationFragmentProvider implements IGenerationFragmentProvider {

    private static final String ANNOTATION = "annotation";

    @Inject
    private Logger logger;

    static final String EXTENSIONID = "org.eclipse.viatra.query.tooling.core.generatorFragment";
    static final String GENERIC_ATTRIBUTE = "";
    private Multimap<String, IGenerationFragment> fragments;

    @Inject
    private IWorkspaceRoot workspaceRoot;

    protected void initializeFragments() {
        fragments = ArrayListMultimap.create();
        final IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSIONID);
        for (IConfigurationElement e : config) {
            final String annotationName = e.getAttribute(ANNOTATION) != null ? e.getAttribute(ANNOTATION)
                    : GENERIC_ATTRIBUTE;
            try {
                IGenerationFragment fragment = (IGenerationFragment) e.createExecutableExtension("fragment");
                fragments.put(annotationName, fragment);
            } catch (CoreException e1) {
                logger.warn("Cannot load generator fragment from " + e.getContributor().getName(), e1);
            }
        }
    }

    @Override
    public Iterable<IGenerationFragment> getFragmentsForPattern(Pattern pattern) {
        if (fragments == null) {
            initializeFragments();
        }
        Set<IGenerationFragment> fragmentSet = new HashSet<>(fragments.get(GENERIC_ATTRIBUTE));
        for (Annotation annotation : pattern.getAnnotations()) {
            fragmentSet.addAll(fragments.get(annotation.getName()));
        }
        return fragmentSet;
    }

    @Override
    public Iterable<IGenerationFragment> getAllFragments() {
        if (fragments == null) {
            initializeFragments();
        }
        return new HashSet<>(fragments.values());
    }

    @Override
    public IProject getFragmentProject(IProject modelProject, IGenerationFragment fragment) {
        if (StringExtensions.isNullOrEmpty(fragment.getProjectPostfix())) {
            return modelProject;
        }
        String projectName = getFragmentProjectName(modelProject, fragment);
        return workspaceRoot.getProject(projectName);
    }

    private String getFragmentProjectName(IProject base, IGenerationFragment fragment) {
        String name = "";
        if (ProjectGenerationHelper.isOpenPDEProject(base)) {
            name = ProjectGenerationHelper.getBundleSymbolicName(base);
        } else {
            base.getName();
        }
        return String
                .format("%s.%s", name, fragment.getProjectPostfix());
    }

}
