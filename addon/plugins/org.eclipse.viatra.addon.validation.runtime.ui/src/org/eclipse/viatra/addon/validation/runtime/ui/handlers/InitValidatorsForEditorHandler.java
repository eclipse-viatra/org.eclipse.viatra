/*******************************************************************************
 * Copyright (c) 2010-2012, Zoltan Ujhelyi, Abel Hegedus, Tamas Szabo, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Zoltan Ujhelyi, Abel Hegedus, Tamas Szabo - initial API and implementation
 *******************************************************************************/
package org.eclipse.viatra.addon.validation.runtime.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.viatra.addon.validation.runtime.ui.ValidationInitUtil;
import org.eclipse.viatra.query.runtime.api.IModelConnectorTypeEnum;
import org.eclipse.viatra.query.runtime.ui.modelconnector.AdapterUtil;
import org.eclipse.viatra.query.runtime.ui.modelconnector.IModelConnector;

public class InitValidatorsForEditorHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IEditorPart editorPart = HandlerUtil.getActiveEditor(event);
        IModelConnector modelConnector = AdapterUtil.getModelConnectorFromIEditorPart(editorPart);
        ResourceSet resourceSet = (ResourceSet) modelConnector.getNotifier(IModelConnectorTypeEnum.RESOURCESET);
        if (resourceSet != null) {
            ValidationInitUtil.initializeAdapters(editorPart, resourceSet);
        }
        return null;
    }

}