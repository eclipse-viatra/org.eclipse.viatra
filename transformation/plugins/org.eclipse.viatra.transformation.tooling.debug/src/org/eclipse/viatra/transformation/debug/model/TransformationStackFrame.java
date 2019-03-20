/**
 * Copyright (c) 2010-2016, Peter Lunk, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.viatra.transformation.debug.model;

import java.util.Arrays;
import java.util.List;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.viatra.transformation.debug.model.transformationstate.ActivationParameter;
import org.eclipse.viatra.transformation.debug.model.transformationstate.RuleActivation;
import org.eclipse.viatra.transformation.debug.model.transformationstate.TransformationModelProvider;

import com.google.common.collect.Lists;

public class TransformationStackFrame extends TransformationDebugElement implements IStackFrame{
    private TransformationThread thread;
    private String name;
    private IVariable[] variables;
    private final TransformationModelProvider modelProvider;
    
    
    public TransformationStackFrame(TransformationThread thread, RuleActivation activation, TransformationModelProvider modelProvider) {
        super((TransformationDebugTarget) thread.getDebugTarget());
        this.thread = thread;
        this.name = activation.getRuleName()+" : "+activation.getState();
        this.modelProvider = modelProvider;
                
        List<TransformationVariable> transformationVariables = Lists.newArrayList();
        transformationVariables.addAll(createVariables(activation));
        
        this.variables = transformationVariables.toArray(new TransformationVariable[0]); 
    }
    
    private List<TransformationVariable> createVariables(RuleActivation activation){
        List<TransformationVariable> createdVariables = Lists.newArrayList();

        for (ActivationParameter parameter : activation.getParameters()) {
            TransformationValue value = new TransformationValue((TransformationDebugTarget) getDebugTarget(), parameter.getValue(), modelProvider);
            TransformationVariable variable = new TransformationVariable((TransformationDebugTarget) getDebugTarget(), parameter.getName(), value);
            createdVariables.add(variable);
        }
        return createdVariables;
    }
    
    @Override
    public boolean canStepInto() {
        return thread.canStepInto();
    }

    @Override
    public boolean canStepOver() {
        return thread.canStepOver();
    }

    @Override
    public boolean canStepReturn() {
        return thread.canStepReturn();
    }

    @Override
    public boolean isStepping() {
        return thread.isStepping();
    }

    @Override
    public void stepInto() throws DebugException {
        thread.stepInto();
    }

    @Override
    public void stepOver() throws DebugException {
        thread.stepOver();
    }

    @Override
    public void stepReturn() throws DebugException {
        thread.stepReturn();
    }

    @Override
    public boolean canResume() {
        return thread.canResume();
    }

    @Override
    public boolean canSuspend() {
        return thread.canSuspend();
    }

    @Override
    public boolean isSuspended() {
        return thread.isSuspended();
    }

    @Override
    public void resume() throws DebugException {
        thread.resume();
    }

    @Override
    public void suspend() throws DebugException {
        thread.suspend();
    }

    @Override
    public boolean canTerminate() {
        return thread.canTerminate();
    }

    @Override
    public boolean isTerminated() {
        return thread.isTerminated();
    }

    @Override
    public void terminate() throws DebugException {
        thread.terminate();
    }

    @Override
    public IThread getThread() {
        return thread;
    }

    @Override
    public IVariable[] getVariables() throws DebugException {
        return Arrays.copyOf(variables, variables.length);
    }

    @Override
    public boolean hasVariables() throws DebugException {
        return variables.length > 0;
    }

    @Override
    public int getLineNumber() throws DebugException {
        return -1;
    }

    @Override
    public int getCharStart() throws DebugException {
        return -1;
    }

    @Override
    public int getCharEnd() throws DebugException {
        return -1;
    }

    @Override
    public String getName() throws DebugException {
        return name;
    }

    @Override
    public IRegisterGroup[] getRegisterGroups() throws DebugException {
        return new IRegisterGroup[0];
    }

    @Override
    public boolean hasRegisterGroups() throws DebugException {
        return false;
    }
}
