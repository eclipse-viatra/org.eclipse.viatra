/*******************************************************************************
 * Copyright (c) 2010-2014, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.patternlanguage.emf.ui.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.viatra.query.patternlanguage.emf.util.SimpleClassLoaderProvider;
import org.eclipse.viatra.query.patternlanguage.emf.vql.Pattern;
import org.eclipse.viatra.query.runtime.exception.ViatraQueryException;
import org.eclipse.xtext.util.IResourceScopeCache;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author Zoltan Ujhelyi
 * @noreference This class is not intended to be referenced by clients.
 */
@Singleton
public class JavaProjectClassLoaderProvider extends SimpleClassLoaderProvider implements IWorkspaceUtilities{

    @Inject
    private IWorkspaceRoot root;
    
    @Inject
    private IResourceScopeCache cache;

    @Override
    public ClassLoader getClassLoader(EObject ctx) {
        if (null == ctx || null == ctx.eResource()) {
            return super.getClassLoader(ctx);
        }
        // Caching is necessary not just for performance, 
        // but also in order to force the same source class URI to be loaded by the same ClassLoader 
        // (hence resulting in the same Class<T> object) 
        // for all parts of a Pattern.
        // 
        // Caveat: the classpath may change without anybody touching this resource. 
        // However, a simple editing of this VQL resource, or a clean&build, will fix the stale classpath problem, while there is no simple fix for the alternative. 
        return cache.get(JavaProjectClassLoaderProvider.class, ctx.eResource(), () -> {
            try {
                IFile file = getIFile(ctx);
                ClassLoader l;
                if (file != null && file.exists()) {
                    l = getClassLoader(file);
                    if (l == null) {
                        throw new ViatraQueryException(String.format("No classloader found for context object %s.", ctx), "No classloader found.");
                    }
                } else {
                    l = super.getClassLoader(ctx);
                }
                return l;
            } catch (Exception e) {
                throw new ViatraQueryException(String.format("Cannot initialize classloader for context object %s because %s",
                        ctx, e.getMessage()), "Cannot initialize classloader", e);
            }
        });
    }
    @Override
    public IFile getIFile(Pattern pattern) {
        return getIFile((EObject)pattern);
    }

    public IFile getIFile(EObject ctx) {
        if (ctx != null) {
            Resource resource = ctx.eResource();
            if (resource != null) {
                URI uri = resource.getURI();
                String scheme = uri.scheme();
                if ("platform".equals(scheme) && uri.segmentCount() > 1 && "resource".equals(uri.segment(0))) {
                    StringBuilder platformResourcePath = new StringBuilder();
                    for (int j = 1, size = uri.segmentCount(); j < size; ++j) {
                        platformResourcePath.append('/');
                        platformResourcePath.append(uri.segment(j));
                    }
                    return root.getFile(new Path(platformResourcePath.toString()));
                }
            }
        }
        return null;
    }

    /**
     * Returns a {@link ClassLoader} that is capable of loading classes defined in the project of the input file, or in
     * any dependencies of that project.
     *
     * @param file
     * @return {@link ClassLoader}
     */
    public ClassLoader getClassLoader(IFile file) throws CoreException, MalformedURLException {
        if (file != null && file.exists()) {
            IProject project = file.getProject();
            IJavaProject jp = JavaCore.create(project);
            String[] classPathEntries = JavaRuntime.computeDefaultRuntimeClassPath(jp);
            List<URL> classURLs = getClassesAsURLs(classPathEntries);
            URL[] urls = classURLs.toArray(new URL[classURLs.size()]);
            URLClassLoader loader = URLClassLoader.newInstance(urls, jp.getClass().getClassLoader());
            return loader;
        }
        return null;
    }

    private List<URL> getClassesAsURLs(String[] classPathEntries) throws MalformedURLException {
        List<URL> urlList = new ArrayList<>();
        for (int i = 0; i < classPathEntries.length; i++) {
            String entry = classPathEntries[i];
            IPath path = new Path(entry);
            URL url = path.toFile().toURI().toURL();
            urlList.add(url);
        }
        return urlList;
    }
}
