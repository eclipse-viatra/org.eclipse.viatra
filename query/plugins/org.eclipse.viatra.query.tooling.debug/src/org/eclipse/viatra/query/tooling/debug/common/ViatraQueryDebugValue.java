/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.tooling.debug.common;

import java.util.Collections;
import java.util.List;

import org.eclipse.debug.core.DebugException;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jdt.internal.debug.core.model.JDIDebugTarget;
import org.eclipse.jdt.internal.debug.core.model.JDIValue;
import org.eclipse.viatra.query.tooling.debug.variables.ValueWrapper;

/**
 * An instances of this class represents the value of a VIATRA Query Debug variable. It is also responsible for the
 * creation of the children variables.
 * <br><br>
 * WARNING: Java Reflection and the Java Debug API are used heavily for the creation of the label and the children
 * variables. Upon API changes (field name change, method name change) these calls can break easily and thus, need to be
 * adjusted accordingly.
 * 
 * @author Tamas Szabo (itemis AG)
 * 
 */
@SuppressWarnings("restriction")
public abstract class ViatraQueryDebugValue extends JDIValue {

    protected JDIDebugTarget debugTarget;
    protected List<IJavaVariable> fVariables;
    protected ValueWrapper fValue;
    protected String[] additionalData;

    public ViatraQueryDebugValue(JDIDebugTarget debugTarget, ValueWrapper value,
            String... additionalData) {
        super(debugTarget, value.getValue());
        this.debugTarget = debugTarget;
        this.fValue = value;
        this.additionalData = additionalData;
    }

    @Override
    protected synchronized List<IJavaVariable> getVariablesList() throws DebugException {
        return Collections.emptyList();
    }

    /**
     * Returns the String representation of the value. This value will be used in the Eclipse Debug View.
     * 
     * @return the String representation of the value
     */
    public abstract String getLabel();
  
}
