/*******************************************************************************
 * Copyright (c) 2010-2012, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.viatra.query.tooling.core.project;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.xtext.ui.XtextProjectHelper;

/**
 * @author Zoltan Ujhelyi
 */
public class ViatraQueryNature implements IProjectNature {

    /**
     * The project nature identifier used for defining the project nature of a VIATRA Query project.
     */
    public static final String NATURE_ID = "org.eclipse.viatra.query.projectnature"; //$NON-NLS-1$
    public static final String XTEXT_NATURE_ID = XtextProjectHelper.NATURE_ID;
    public static final String BUILDER_ID = "org.eclipse.viatra.query.tooling.ui.projectbuilder";//$NON-NLS-1$
    public static final String SRCGEN_DIR = "src-gen/"; //$NON-NLS-1$
    public static final String SRC_DIR = "src/"; //$NON-NLS-1$
    public static final String EXECUTION_ENVIRONMENT = "JavaSE-1.8"; // $NON_NLS-1$
    public static final String VQGENMODEL = "generator.vqgen";

    private IProject project;

    public IProject getProject() {
        return project;
    }

    public void setProject(IProject project) {
        this.project = project;
    }

    public void configure() throws CoreException {
        IProjectDescription desc = project.getDescription();
        ICommand[] commands = desc.getBuildSpec();
        for (int i = 0; i < commands.length; i++) {
            if (commands[i].getBuilderName().equals(BUILDER_ID)) {
                return; // Builder is already configured, returning
            }
        }

        ICommand command = desc.newCommand();
        command.setBuilderName(BUILDER_ID);
        ICommand[] newCommandList = new ICommand[commands.length + 1];
        System.arraycopy(commands, 0, newCommandList, 1, commands.length);
        newCommandList[0] = command;
        desc.setBuildSpec(newCommandList);
        project.setDescription(desc, null);
    }

    public void deconfigure() throws CoreException {
        IProjectDescription desc = project.getDescription();
        ICommand[] commands = desc.getBuildSpec();
        int index = 0;
        for (; index < commands.length; index++) {
            if (commands[index].getBuilderName().equals(BUILDER_ID)) {
                break; // Builder found
            }
        }
        if (index == commands.length) {
            return;
        }
        ICommand command = desc.newCommand();
        command.setBuilderName(BUILDER_ID);
        ICommand[] newCommandList = new ICommand[commands.length - 1];
        if (newCommandList.length > 0) {
            System.arraycopy(commands, 0, newCommandList, 0, index);
            System.arraycopy(commands, index + 1, newCommandList, index, commands.length - index - 1);
        }
        desc.setBuildSpec(newCommandList);
        project.setDescription(desc, null);
    }

}
