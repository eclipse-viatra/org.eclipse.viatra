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

package org.eclipse.viatra.query.tooling.ui;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class ViatraQueryGUIPlugin extends AbstractUIPlugin {

    /**
     * The plug-in ID
     */
    public static final String PLUGIN_ID = "org.eclipse.viatra.query.tooling.ui";

    public static final String ICON_ROOT = "navigator_root";
    public static final String ICON_MATCHER = "matcher";
    public static final String ICON_MATCH = "match";
    public static final String ICON_ERROR = "error";
    public static final String ICON_ARROW_RIGHT = "arrow_right";
    public static final String ICON_ARROW_LEFT = "arrow_left";
    public static final String ICON_PIN = "pin";
    public static final String ICON_ARROW_TOP = "arrow_top";
    public static final String ICON_ARROW_BOTTOM = "arrow_bottom";
    public static final String ICON_EPACKAGE = "epackage";
    public static final String ICON_VQL = "vql";
    /**
     * @since 1.3
     */
    public static final String ICON_PROJECT = "project";
    /**
     * @since 1.4
     */
    public static final String ICON_VIATRA = "viatra";
    /**
     * @since 2.1
     */
    public static final String ICON_BASE_OPTIONS = "base_options";
    /**
     * @since 2.1
     */
    public static final String ICON_ENGINE_OPTIONS = "engine_options";

    // The shared instance
    private static ViatraQueryGUIPlugin plugin;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     * 
     * @return the shared instance
     */
    public static ViatraQueryGUIPlugin getDefault() {
        return plugin;
    }

    @Override
    protected void initializeImageRegistry(ImageRegistry reg) {
        super.initializeImageRegistry(reg);
        reg.put(ICON_ROOT, imageDescriptorFromPlugin(JavaUI.ID_PLUGIN, "icons/full/eview16/types.png"));
        reg.put(ICON_MATCHER, imageDescriptorFromPlugin("org.eclipse.debug.ui", "icons/full/eview16/breakpoint_view.png"));
        reg.put(ICON_MATCH, imageDescriptorFromPlugin(JavaUI.ID_PLUGIN, "icons/full/obj16/methpub_obj.png"));
        reg.put(ICON_ERROR, imageDescriptorFromPlugin(PlatformUI.PLUGIN_ID, "icons/full/progress/errorstate.png"));
        reg.put(ICON_PIN, imageDescriptorFromPlugin(PLUGIN_ID, "icons/pin.gif"));
        reg.put(ICON_ARROW_RIGHT, imageDescriptorFromPlugin(PLUGIN_ID, "icons/arrow_right.gif"));
        reg.put(ICON_ARROW_LEFT, imageDescriptorFromPlugin(PLUGIN_ID, "icons/arrow_left.gif"));
        reg.put(ICON_ARROW_TOP, imageDescriptorFromPlugin(PLUGIN_ID, "icons/arrow_top.gif"));
        reg.put(ICON_ARROW_BOTTOM, imageDescriptorFromPlugin(PLUGIN_ID, "icons/arrow_bottom.gif"));
        reg.put(ICON_EPACKAGE, imageDescriptorFromPlugin(PLUGIN_ID, "icons/epackage.gif"));
        reg.put(ICON_VQL, imageDescriptorFromPlugin(PLUGIN_ID, "icons/logo2.png"));
        reg.put(ICON_VIATRA, imageDescriptorFromPlugin(PLUGIN_ID, "icons/rsz_viatra_logo.png"));
        reg.put(ICON_PROJECT, imageDescriptorFromPlugin("org.eclipse.ui.ide", "icons/full/obj16/prj_obj.png"));
        reg.put(ICON_BASE_OPTIONS, imageDescriptorFromPlugin(PLUGIN_ID, "icons/base_options.png"));
        reg.put(ICON_ENGINE_OPTIONS, imageDescriptorFromPlugin(PLUGIN_ID, "icons/engine_options.png"));
    }

    public void logException(String message, Throwable exception) {
        ILog logger = getLog();
        logger.log(new Status(IStatus.ERROR, PLUGIN_ID, message, exception));
    }
}
