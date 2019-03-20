/*******************************************************************************
 * Copyright (c) 2010-2012, Zoltan Ujhelyi, Abel Hegedus, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.addon.validation.runtime.ui;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.ui.IEditorPart;
import org.eclipse.viatra.addon.validation.runtime.ValidationUtil;

public class ValidationInitUtil {

    /**
     * Constructor hidden for utility class
     */
    private ValidationInitUtil() {

    }

    public static void initializeAdapters(IEditorPart activeEditor, Notifier root) {
        // if(adapterMap.containsKey(activeEditor)) {
        // FIXME define proper semantics for validation based on selection
        // FIXME handle already existing violations
        // adapterMap.get(activeEditor).addAll(adapters);
        // } else {
        if (!ValidationUtil.getAdapterMap().containsKey(activeEditor)) {
            ValidationUtil.addNotifier(activeEditor, root);
            ValidationUtil.registerEditorPart(activeEditor);
        }
    }

}