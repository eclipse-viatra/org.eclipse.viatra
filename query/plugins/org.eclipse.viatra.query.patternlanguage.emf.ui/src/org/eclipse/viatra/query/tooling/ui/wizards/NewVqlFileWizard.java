/*******************************************************************************
 * Copyright (c) 2010-2012, Zoltan Ujhelyi, Tamas Szabo, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Zoltan Ujhelyi, Tamas Szabo - initial API and implementation
 *******************************************************************************/

package org.eclipse.viatra.query.tooling.ui.wizards;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import org.eclipse.viatra.query.patternlanguage.emf.ui.EMFPatternLanguageUIPlugin;
import org.eclipse.viatra.query.patternlanguage.emf.vql.ClassType;
import org.eclipse.viatra.query.patternlanguage.emf.vql.PatternLanguageFactory;
import org.eclipse.viatra.query.patternlanguage.emf.vql.PackageImport;
import org.eclipse.viatra.query.patternlanguage.emf.vql.PatternModel;
import org.eclipse.viatra.query.patternlanguage.emf.vql.ExecutionType;
import org.eclipse.viatra.query.patternlanguage.emf.vql.Modifiers;
import org.eclipse.viatra.query.patternlanguage.emf.vql.Parameter;
import org.eclipse.viatra.query.patternlanguage.emf.vql.Pattern;
import org.eclipse.viatra.query.patternlanguage.emf.vql.PatternBody;
import org.eclipse.viatra.query.tooling.ui.wizards.internal.ObjectParameter;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * A wizard implementation used to create new eiq files.
 * 
 * @author Tamas Szabo
 * 
 */
public class NewVqlFileWizard extends Wizard implements INewWizard {

    private static final String NEW_EMF_INC_QUERY_QUERY_DEFINITION_FILE = "Create a new VIATRA Query Definition file.";
    private NewVqlFileWizardContainerConfigurationPage page1;
    private NewVqlFileWizardPatternConfigurationPage page2;
    private ISelection selection;
    private IWorkbench workbench;
    private final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

    @Inject
    private Injector injector;

    public NewVqlFileWizard() {
        super();
        setNeedsProgressMonitor(true);
    }

    @Override
    public void addPages() {
        page1 = new NewVqlFileWizardContainerConfigurationPage();
        page1.init((IStructuredSelection) selection);
        page1.setDescription(NEW_EMF_INC_QUERY_QUERY_DEFINITION_FILE);
        page2 = new NewVqlFileWizardPatternConfigurationPage();
        injector.injectMembers(page2);
        addPage(page1);
        addPage(page2);
        setForcePreviousAndNextButtons(false);
    }

    @Override
    public boolean performFinish() {
        final String containerName = page1.getContainerName();
        final String fileName = page1.getFileName();

        // replace dots with slash in the path
        final String packageName = page1.getPackageName().replaceAll("\\.", "/");
        final String patternName = page2.getPatternName();
        final List<EPackage> imports = page2.getImports();
        final List<ObjectParameter> parameters = page2.getParameters();

        IRunnableWithProgress op = new IRunnableWithProgress() {
            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException {
                try {
                    monitor.beginTask("Creating " + fileName, 1);
                    IFile file = createEiqFile(containerName, fileName, packageName, patternName, imports, parameters);
                    BasicNewResourceWizard.selectAndReveal(file,
                            workbench.getActiveWorkbenchWindow());
                    IDE.openEditor(workbench.getActiveWorkbenchWindow().getActivePage(), file, true);
                    monitor.worked(1);
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            }
        };
        try {
            getContainer().run(false, false, op);
        } catch (InterruptedException e) {
            // This is never thrown as of false cancelable parameter of getContainer().run
            Thread.currentThread().interrupt();
            return false;
        } catch (InvocationTargetException e) {
            Throwable realException = e.getTargetException();
            EMFPatternLanguageUIPlugin.getInstance().logException(
                    "Cannot create Query Definition file: " + realException.getMessage(), realException);
            MessageDialog.openError(getShell(), "Error", realException.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.selection = selection;
        this.workbench = workbench;
    }

    private IFile createEiqFile(String containerName, String fileName, String packageName, String patternName,
            List<EPackage> imports, List<ObjectParameter> parameters) throws IOException, CoreException {
        IResource containerResource = root.findMember(new Path(containerName));
        ResourceSet resourceSet = page2.getResourceSet();

        IPath filePath = containerResource.getFullPath().append(packageName).append(fileName);
        IFile file = root.getFile(filePath);
        String fullPath = filePath.toString();

        URI fileURI = URI.createPlatformResourceURI(fullPath, false);
        Resource resource = resourceSet.createResource(fileURI);

        PatternModel pm = PatternLanguageFactory.eINSTANCE.createPatternModel();

        // Setting package name
        if (packageName != null && !packageName.isEmpty()) {
            pm.setPackageName(packageName.replace("/", "."));
        }

        pm.setImportPackages(PatternLanguageFactory.eINSTANCE.createVQLImportSection());
        // Setting imports
        for (EPackage importedPackage : imports) {
            PackageImport importDecl = PatternLanguageFactory.eINSTANCE.createPackageImport();
            importDecl.setEPackage(importedPackage);
            pm.getImportPackages().getPackageImport().add(importDecl);
        }

        // Creating pattern
        if (patternName != null && patternName.length() > 0) {
            Pattern pattern = PatternLanguageFactory.eINSTANCE.createPattern();
            pattern.setName(patternName);
            PatternBody body = PatternLanguageFactory.eINSTANCE.createPatternBody();
            pattern.getBodies().add(body);

            // Setting modifiers
            Modifiers modifiers = PatternLanguageFactory.eINSTANCE.createModifiers();
            modifiers.setPrivate(false);
            modifiers.setExecution(ExecutionType.UNSPECIFIED);
            pattern.setModifiers(modifiers);
            
            // Setting pattern parameters
            for (ObjectParameter parameter : parameters) {
                Parameter var = PatternLanguageFactory.eINSTANCE.createParameter();
                var.setName(parameter.getParameterName());

                ClassType classType = PatternLanguageFactory.eINSTANCE.createClassType();
                // it is enough to set only the class name for the class type
                classType.setClassname(parameter.getObject());
                var.setType(classType);
                pattern.getParameters().add(var);
            }

            pm.getPatterns().add(pattern);
        }
        resource.getContents().add(pm);

        resource.save(Collections.emptyMap());
        containerResource.refreshLocal(0, new NullProgressMonitor());
        return file;
    }
}