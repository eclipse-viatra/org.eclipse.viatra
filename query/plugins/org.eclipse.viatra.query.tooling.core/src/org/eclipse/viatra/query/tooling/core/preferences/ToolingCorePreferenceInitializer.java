/*******************************************************************************
 * Copyright (c) 2017, Abel Hegedus, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.tooling.core.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.viatra.query.tooling.core.generator.ViatraQueryGeneratorPlugin;

/**
 * Class used to initialize default preference values for VIATRA Query Tooling Core.
 * 
 * @noreference This internal class is not intended to be referenced by clients.
 */
public class ToolingCorePreferenceInitializer extends AbstractPreferenceInitializer {

    /**
     * @noreference This method is not intended to be referenced by clients.
     */
    public void initializeDefaultPreferences() {
        IPreferenceStore store = ViatraQueryGeneratorPlugin.INSTANCE.getPreferenceStore();
        store.setDefault(ToolingCorePreferenceConstants.P_DISABLE_TARGET_PLATFORM_METAMODEL_INDEX_UPDATE, false);
    }

}
