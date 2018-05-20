/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Tamas Szabo, Zoltan Ujhelyi - initial API and implementation
 *******************************************************************************/

package org.eclipse.viatra.query.tooling.ui.wizards.internal.operations;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.viatra.query.tooling.core.project.ProjectGenerationHelper;
import org.eclipse.xtext.builder.EclipseOutputConfigurationProvider;

public class CreateProjectOperation extends WorkspaceModifyOperation {
    private final IProject projectHandle;
    private final IProjectDescription description;
    private final List<String> dependencies;
    private final EclipseOutputConfigurationProvider outputConfigurationProvider;

    public CreateProjectOperation(IProject projectHandle, IProjectDescription description, List<String> dependencies, EclipseOutputConfigurationProvider outputConfigurationProvider) {
        this.projectHandle = projectHandle;
        this.description = description;
        this.dependencies = dependencies;
        this.outputConfigurationProvider = outputConfigurationProvider;
    }

    protected void execute(IProgressMonitor monitor) throws CoreException {
        ProjectGenerationHelper.createProject(description, projectHandle, dependencies, monitor);
        ProjectGenerationHelper.ensureSourceFolder(projectHandle, outputConfigurationProvider.getOutputConfigurations(), monitor);
    }
}