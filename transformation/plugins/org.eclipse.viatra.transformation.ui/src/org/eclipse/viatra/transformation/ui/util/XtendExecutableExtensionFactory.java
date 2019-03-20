/**
 * Copyright (c) 2010-2016, Peter Lunk, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.viatra.transformation.ui.util;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

@SuppressWarnings("restriction")
public class XtendExecutableExtensionFactory extends org.eclipse.xtend.ide.XtendExecutableExtensionFactory {

    @Override
    protected Bundle getBundle() {
        return Platform.getBundle("org.eclipse.viatra.transformation.ui");
    }

}
