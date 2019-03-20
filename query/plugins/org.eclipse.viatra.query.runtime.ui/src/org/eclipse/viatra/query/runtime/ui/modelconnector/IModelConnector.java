/*******************************************************************************
 * Copyright (c) 2010-2012, Andras Okros, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.ui.modelconnector;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.viatra.query.runtime.api.IModelConnectorTypeEnum;

/**
 * This interface provides all API calls for a connector that can provide an instance model. Implementations of this
 * interface should contain the editor specific handling of loadModel, unloadModel, getNotifier and showLocation
 * methods.
 */
public interface IModelConnector {

    /**
     * Loads the instance model into the model connector with the given type.
     * 
     * @param modelConnectorTypeEnum
     *            The model type which should be loaded.
     */
    public abstract void loadModel(IModelConnectorTypeEnum modelConnectorTypeEnum);

    /**
     * Unloads the instance model from the model connector.
     */
    public abstract void unloadModel();

    /**
     * @param modelConnectorTypeEnum
     * @return A Notifier implementation for the given IModelConnectorType.
     */
    public Notifier getNotifier(IModelConnectorTypeEnum modelConnectorTypeEnum);

    /**
     * @param locationObjects
     *            Shows the location of these objects inside the specific editor.
     */
    public abstract void showLocation(Object[] locationObjects);
    
    /**
     * @return a workbench part (view, editor) which is the owner of the model adapted by the model connector
     */
    public IWorkbenchPart getOwner();

    /**
     * Returns the objects currently selected at the adapted model. If there are both domain and view model elements in
     * the adapted model, it is expected that the domain model elements are returned.
     * 
     * @return a non-null, but possibly empty collection of model objects
     */
    public Collection<EObject> getSelectedEObjects();
}
