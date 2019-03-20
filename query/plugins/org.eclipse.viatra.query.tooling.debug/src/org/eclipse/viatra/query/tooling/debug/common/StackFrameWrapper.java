/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.tooling.debug.common;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.debug.core.DebugException;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jdt.internal.debug.core.model.JDIDebugModelMessages;
import org.eclipse.jdt.internal.debug.core.model.JDIStackFrame;
import org.eclipse.jdt.internal.debug.core.model.JDIThread;
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngine;
import org.eclipse.viatra.query.runtime.util.ViatraQueryLoggingUtil;

import com.sun.jdi.StackFrame;

/**
 * Instances of this class wrap a {@link JDIStackFrame} and replaces the original 
 * {@link IJavaVariable}s with the ones that are created by the attached 
 * {@link VariablesFactory}. The {@link JDIStackFrame} then will be used by the 
 * Eclipse Debug Tooling to populate the contents of the Debug View.
 * 
 * @author Tamas Szabo (itemis AG)
 *
 */
@SuppressWarnings("restriction")
public class StackFrameWrapper extends JDIStackFrame {

    protected List<IJavaVariable> fVariables;
    protected JDIThread wrappedThread;
    protected JDIStackFrame wrappedStackFrame;
    protected StackFrame fStackFrame;
    private static Map<JDIStackFrame, StackFrameWrapper> stackFrameMap = new WeakHashMap<>();
    protected VariablesFactory variablesFactory;
    
    /**
     * Transforms the {@link JDIStackFrame} to an "VIATRA Query-specific" one. 
     * This means that all variables are ignored from the original stack frame and 
     * {@link ViatraQueryEngine} instances are added instead. 
     * 
     * @param frame the original stack frame
     * @return the transformed VIATRA Query specific stack frame
     */
    public static StackFrameWrapper transform(JDIStackFrame frame) {
        if (stackFrameMap.get(frame) != null) {
            return stackFrameMap.get(frame);
        } else {
            try {
                StackFrameWrapper transformed = new StackFrameWrapper(frame, (JDIThread) frame.getThread(),
                        (StackFrame) ViatraQueryDebugUtil.getField(frame, "fStackFrame"),
                        (Integer) ViatraQueryDebugUtil.getField(frame, "fDepth"));
                stackFrameMap.put(frame, transformed);
                return transformed;
            } catch (Exception e) {
                ViatraQueryLoggingUtil.getLogger(StackFrameWrapper.class).error("Stack frame transformation has failed!", e);
                return null;
            }
        }
    }

    public StackFrameWrapper(JDIStackFrame jdiStackFrame, JDIThread thread, StackFrame frame, int depth) {
        super(thread, frame, depth);
        this.fStackFrame = frame;
        this.wrappedThread = thread;
        this.wrappedStackFrame = jdiStackFrame;
    }
    
    public void setVariablesFactory(VariablesFactory variablesFactory) {
        this.variablesFactory = variablesFactory;
    }
    
    public VariablesFactory getVariablesFactory() {
        return variablesFactory;
    }

    @Override
    protected List<IJavaVariable> getVariables0() throws DebugException {
        synchronized (this.wrappedThread) {
            if (this.fVariables == null) {
                if (wrappedStackFrame.isNative()) {
                    requestFailed(
                            JDIDebugModelMessages.JDIStackFrame_Variable_information_unavailable_for_native_methods,
                            null);
                }

                fVariables = variablesFactory.getVariables(wrappedStackFrame, fStackFrame.thread());
            } 
            updateVariables();
            return fVariables;
        }
    }
    
    @Override
    protected void setUnderlyingStackFrame(StackFrame frame) {
        synchronized (this.wrappedThread) {
            fStackFrame = frame;
        }
    }

    @Override
    protected void setVariables(List<IJavaVariable> variables) {
        this.fVariables = variables;
    }

    @Override
    protected void updateVariables() throws DebugException {
        if (fVariables != null) {
            fVariables.clear();
            for (IJavaVariable variable : variablesFactory.getVariables(wrappedStackFrame, fStackFrame.thread())) {
                fVariables.add(variable);
            }
        }
    }
}
