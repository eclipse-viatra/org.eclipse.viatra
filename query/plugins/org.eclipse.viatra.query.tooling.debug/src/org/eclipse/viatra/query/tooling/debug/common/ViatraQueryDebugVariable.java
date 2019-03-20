/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.tooling.debug.common;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.jdt.internal.debug.core.model.JDIDebugTarget;
import org.eclipse.jdt.internal.debug.core.model.JDIValue;
import org.eclipse.jdt.internal.debug.core.model.JDIVariable;
import org.eclipse.viatra.query.runtime.util.ViatraQueryLoggingUtil;

import com.sun.jdi.Type;
import com.sun.jdi.Value;

/**
 * A VIATRA Query Debug variable which has a specific JDI value attached.  
 * Variables will be displayed in the Eclipse Debug View.
 * 
 * @author Tamas Szabo (itemis AG)
 *
 */
@SuppressWarnings("restriction")
public class ViatraQueryDebugVariable extends JDIVariable {

    private IValue modelValue;
    
    public ViatraQueryDebugVariable(JDIDebugTarget target) {
        super(target);
    }

    @Override
    public String getSignature() throws DebugException {
        try {
            return ((JDIValue) getValue()).getSignature();
        } catch (RuntimeException e) {
            ViatraQueryLoggingUtil.getLogger(ViatraQueryDebugVariable.class).error("getSignature call has failed!", e);
            return null;
        }
    }

    @Override
    public String getGenericSignature() throws DebugException {
        try {
            return ((JDIValue) getValue()).getGenericSignature();
        } catch (RuntimeException e) {
            ViatraQueryLoggingUtil.getLogger(ViatraQueryDebugVariable.class).error("getGenericSignature call has failed!", e);
            return null;
        }
    }

    @Override
    public String getName() throws DebugException {
        if (getValue() instanceof ViatraQueryDebugValue) {
            return ((ViatraQueryDebugValue) getValue()).getLabel();
        }
        return null;
    }

    @Override
    public String getReferenceTypeName() throws DebugException {
        try {
            return ((JDIValue) getValue()).getReferenceTypeName();
        } catch (RuntimeException e) {
            ViatraQueryLoggingUtil.getLogger(ViatraQueryDebugVariable.class).error("getReferenceTypeName call has failed!", e);
            return null;
        }
    }

    @Override
    protected Value retrieveValue() throws DebugException {
        return null;
    }

    @Override
    protected Type getUnderlyingType() throws DebugException {
        return null;
    }
    
    @Override
    public void setValue(IValue value) throws DebugException {
        if (value instanceof JDIValue) {
            this.modelValue = value;
        }
    }
    
    @Override
    public IValue getValue() throws DebugException {
        return this.modelValue;
    }
}
