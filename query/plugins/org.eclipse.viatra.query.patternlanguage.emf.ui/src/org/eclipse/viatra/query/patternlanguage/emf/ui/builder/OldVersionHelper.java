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
package org.eclipse.viatra.query.patternlanguage.emf.ui.builder;

import java.util.Map;
import java.util.NoSuchElementException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.viatra.query.patternlanguage.emf.vql.Pattern;
import org.eclipse.xtext.ui.resource.IResourceSetProvider;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

/**
 * @author Zoltan Ujhelyi
 *
 */
public class OldVersionHelper {

    Map<IPath, URI> copiedURIMap = Maps.newHashMap();
    Map<IProject, ResourceSet> resourceSetMap = Maps.newHashMap();

    /**
     * Predicate to decide whether a classpath entry is the corresponding source folder for a path
     *
     */
    public static final class SourceFolderFinder implements Predicate<IClasspathEntry> {
        private final IPath relativePath;

        private SourceFolderFinder(IPath relativePath) {
            this.relativePath = relativePath;
        }

        @Override
        public boolean apply(IClasspathEntry entry) {
            if (entry == null) {
                return false;
            }
            boolean isSourceEntry = entry.getContentKind() == IPackageFragmentRoot.K_SOURCE;
            if (isSourceEntry) {
                return entry.getPath().isPrefixOf(relativePath);
            } else {
                return false;
            }
        }
    }

    @Inject
    private IWorkspaceRoot workspaceRoot;
    @Inject
    private IResourceSetProvider provider;

    private ResourceSet getResourceSet(IProject project) {
        if (!resourceSetMap.containsKey(project)) {
            ResourceSet set = provider.get(project);
            resourceSetMap.put(project, set);
            return set;
        }
        return resourceSetMap.get(project);
    }

    private URI getCopiedURI(IProject project, IPath relativePath) throws JavaModelException {
        try {
            if (!copiedURIMap.containsKey(relativePath)) {
                IJavaProject javaProject = JavaCore.create(project);
                IClasspathEntry sourceEntry = Iterators.find(
                        Iterators.forArray(javaProject.getResolvedClasspath(true)),
                        new SourceFolderFinder(relativePath));
                IPath outputLocation = sourceEntry.getOutputLocation();
                if (outputLocation == null) {
                    outputLocation = javaProject.getOutputLocation();
                }
                IPath path = outputLocation.append(relativePath.makeRelativeTo(sourceEntry.getPath()));
                URI copiedURI = (project.getWorkspace().getRoot().findMember(path) != null) ? URI
                        .createPlatformResourceURI(path.toString(), true) : null;
                if (copiedURI != null) {
                    copiedURIMap.put(relativePath, copiedURI);
                }
                return copiedURI;
            }
            return copiedURIMap.get(relativePath);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    /**
     * Finds the version of the pattern stored by the previous run of the VIATRA builder.
     * </p>
     * 
     * <strong>Note</strong>: This code should be called only when the proxyURI represents something in an existing
     * project. It is not a problem is the proxyURI is unresolvable, but the project part has to exist.
     * @since 2.0
     */
    public Pattern findPattern(URI proxyURI) throws JavaModelException {
        return findOldVersion(proxyURI);
    }
    
    /**
     * Finds the version of the object stored by the previous run of the VIATRA builder.
     * </p>
     * 
     * <strong>Note</strong>: This code should be called only when the proxyURI represents something in an existing
     * project. It is not a problem is the proxyURI is unresolvable, but the project part has to exist.
     * @since 1.6
     */
    @SuppressWarnings("unchecked")
    public <T extends EObject> T findOldVersion(URI proxyURI) throws JavaModelException {
        final IPath absolutePath = new Path(proxyURI.toPlatformString(true));
        
        if (absolutePath.segmentCount() == 0) {
            return null;
        }
        IProject project = workspaceRoot.getProject(absolutePath.segment(0));
        ResourceSet set = getResourceSet(project);

        URI copiedURI = getCopiedURI(project, absolutePath);
        if (copiedURI != null) {
            Resource res = set.getResource(copiedURI, true);
            String fragment = proxyURI.fragment();
            return (T) res.getEObject(fragment);
        }
        return null;
    }
}
