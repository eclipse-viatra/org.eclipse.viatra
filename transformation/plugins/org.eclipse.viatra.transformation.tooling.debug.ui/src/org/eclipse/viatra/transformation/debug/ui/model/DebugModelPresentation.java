/**
 * Copyright (c) 2010-2016, Peter Lunk, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.viatra.transformation.debug.ui.model;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.viatra.transformation.debug.model.TransformationDebugTarget;
import org.eclipse.viatra.transformation.debug.model.TransformationStackFrame;
import org.eclipse.viatra.transformation.debug.model.TransformationThread;
import org.eclipse.viatra.transformation.debug.model.breakpoint.ActivationBreakpoint;
import org.eclipse.viatra.transformation.debug.model.breakpoint.ActivationBreakpointHandler;
import org.eclipse.viatra.transformation.debug.transformationtrace.model.RuleParameterTrace;
import org.eclipse.viatra.transformation.debug.ui.activator.TransformationDebugUIActivator;

public class DebugModelPresentation extends LabelProvider implements IDebugModelPresentation {

    @Override
    public void setAttribute(String attribute, Object value) {
        // Attributes are not supported
    }

    @Override
    public String getText(Object element) {
        try {
            if (element instanceof ActivationBreakpoint) {
                ActivationBreakpoint breakpoint = (ActivationBreakpoint) element;
                String parameters = "";
                for (RuleParameterTrace parameterTrace : ((ActivationBreakpointHandler) breakpoint.getHandler()).getTrace().getRuleParameterTraces()) {
                    parameters = parameters.concat(parameterTrace.getParameterName() + " : " + parameterTrace.getObjectId() + " ");
                }
                return "Transformation Activation Breakpoint - Rule: " + ((ActivationBreakpointHandler) breakpoint.getHandler()).getTrace().getRuleName() + "(" + parameters + ")";

            } else if (element instanceof TransformationStackFrame) {
                return ((TransformationStackFrame) element).getName();
            } else if (element instanceof TransformationThread) {
                return ((TransformationThread) element).getName();
            } else if (element instanceof TransformationDebugTarget) {
                return ((TransformationDebugTarget) element).getName();
            } 
        } catch (DebugException e) {
            TransformationDebugUIActivator.getDefault().logException(e.getMessage(), e);
            return super.getText(element);
        }

        return super.getText(element);
    }

    @Override
    public void computeDetail(IValue value, IValueDetailListener listener) {
    }

    @Override
    public IEditorInput getEditorInput(Object element) {
        // SRC lookup is not supported yet
        return null;
    }

    @Override
    public String getEditorId(IEditorInput input, Object element) {
        // SRC lookup is not supported yet
        return null;
    }

}
