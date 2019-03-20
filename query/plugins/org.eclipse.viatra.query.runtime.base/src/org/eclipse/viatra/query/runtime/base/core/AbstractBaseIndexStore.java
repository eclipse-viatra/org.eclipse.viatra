/*******************************************************************************
 * Copyright (c) 2010-2017, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.base.core;

import org.apache.log4j.Logger;
import org.eclipse.viatra.query.runtime.base.api.BaseIndexOptions;

/**
 * @since 1.6
 */
public class AbstractBaseIndexStore {

    protected final NavigationHelperImpl navigationHelper;
    protected final Logger logger;
    protected final BaseIndexOptions options;
    
    public AbstractBaseIndexStore(NavigationHelperImpl navigationHelper, Logger logger) {
        this.navigationHelper = navigationHelper;
        this.logger = logger;
        this.options = navigationHelper.getBaseIndexOptions();
    }
    
    protected void logNotificationHandlingError(String msg) {
        if (options.isStrictNotificationMode()) {
            // This will cause e.g. query engine to become tainted
            navigationHelper.notifyFatalListener(msg, new IllegalStateException(msg));
        } else {
            logger.error(msg);
        }
    }
}
