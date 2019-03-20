/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.tooling.debug.common;

import java.lang.reflect.Field;

import org.eclipse.viatra.query.runtime.util.ViatraQueryLoggingUtil;

public class ViatraQueryDebugUtil {
    
    private ViatraQueryDebugUtil() {/*Utility class constructor*/}
    
    /**
     * Returns the value of the {@link Field} with name fieldName in the given {@link Object}. This method uses Java
     * Reflection.
     * 
     * @param obj
     *            the instance
     * @param fieldName
     *            the name of the field
     * @return the value of the field
     */
    public static Object getField(Object obj, String fieldName) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            ViatraQueryLoggingUtil.getLogger(ViatraQueryDebugUtil.class).error("Couldn't retrieve the value of the field!", e);
            return null;
        }
    }

}
