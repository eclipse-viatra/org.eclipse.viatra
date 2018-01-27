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

package org.eclipse.viatra.query.tooling.ui.wizards.internal;

import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.viatra.query.tooling.core.targetplatform.ITargetPlatformMetamodelLoader;
import org.eclipse.viatra.query.tooling.ui.wizards.NewVqlFileWizardPatternConfigurationPage;

/**
 * An {@link IListAdapter} implementation for specifying pattern parameters in the wizard.
 * 
 * @author Tamas Szabo
 * 
 */
@SuppressWarnings("restriction")
public class ObjectListAdapter implements IListAdapter<ObjectParameter> {

    private ListDialogField<String> importList;
    private NewVqlFileWizardPatternConfigurationPage page;
    private ITargetPlatformMetamodelLoader metamodelLoader;

    public ObjectListAdapter(NewVqlFileWizardPatternConfigurationPage page, ListDialogField<String> importList, ITargetPlatformMetamodelLoader metamodelLoader) {
        this.importList = importList;
        this.page = page;
        this.metamodelLoader = metamodelLoader;
    }

    @Override
    public void customButtonPressed(ListDialogField<ObjectParameter> field, int index) {
        ObjectParameter parameter = new ObjectParameter();
        ObjectParameterConfigurationDialog dialog = new ObjectParameterConfigurationDialog(PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getShell(), page.getResourceSet(), importList.getElements(), metamodelLoader, parameter);
        // a unique parameter object is needed because the dialog will be disposed after the ok button is pressed
        if (index == 0) {
            // Add
            if (dialog.open() == Dialog.OK) {
                field.addElement(parameter);
            }
        } else if (index == 1) {
            // Modify
            ObjectParameter firstElement = field.getSelectedElements().get(0);
            parameter.setObject(firstElement.getObject());
            parameter.setParameterName(firstElement.getParameterName());
            if (dialog.open() == Dialog.OK) {
                firstElement.setObject(parameter.getObject());
                firstElement.setParameterName(parameter.getParameterName());
            }
        }

        field.refresh();
    }

    @Override
    public void selectionChanged(ListDialogField<ObjectParameter> field) {
        if (field.getElements().isEmpty()) {
            field.enableButton(1, false);
            page.parameterSet = false;
        } else {
            field.enableButton(1, true);
            page.parameterSet = true;
        }

        page.validatePage();
    }

    @Override
    public void doubleClicked(ListDialogField<ObjectParameter> field) {
    }
}
