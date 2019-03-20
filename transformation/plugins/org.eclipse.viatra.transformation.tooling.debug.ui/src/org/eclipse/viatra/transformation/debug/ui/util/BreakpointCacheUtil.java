/**
 * Copyright (c) 2010-2016, Peter Lunk, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.viatra.transformation.debug.ui.util;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.viatra.transformation.debug.model.breakpoint.ConditionalTransformationBreakpoint;
import org.eclipse.viatra.transformation.debug.model.breakpoint.RuleBreakpoint;
import org.eclipse.viatra.transformation.debug.ui.activator.TransformationDebugUIActivator;

import com.google.common.collect.Lists;

public class BreakpointCacheUtil {
    
    private BreakpointCacheUtil() {/*Utility class constructor*/}
    
    public static String BKP_CACHE_NAME = "/persistentbreakpoints.bpkt"; 
    
    public static String getBreakpointCacheLocation(){
        TransformationDebugUIActivator activator = new TransformationDebugUIActivator   ();
        IPath stateLocation = activator.getStateLocation();
        String location = stateLocation.toString();
        String fileLocation = location+BKP_CACHE_NAME;
        
        return fileLocation;
    }
    
    public static boolean breakpointCacheExists(){
        File file = new File(getBreakpointCacheLocation());
        return file.exists();
    }
    
    public static IBreakpoint[] filterBreakpoints(IBreakpoint[] iBreakpoints){
        List<IBreakpoint> ret = Lists.newArrayList();
        for (IBreakpoint breakpoint : iBreakpoints) {
            if(breakpoint instanceof RuleBreakpoint || breakpoint instanceof ConditionalTransformationBreakpoint){
                ret.add(breakpoint);
            }
        }
        return ret.toArray(new IBreakpoint[ret.size()]);
        
    }
}
