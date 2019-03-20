/*******************************************************************************
 * Copyright (c) 2010-2013, Peter Lunk, Abel Hegedus, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.transformation.evm.api.adapter;

import org.eclipse.viatra.query.runtime.api.ViatraQueryEngine;
import org.eclipse.viatra.transformation.evm.api.Activation;
import org.eclipse.viatra.transformation.evm.api.RuleSpecification;
import org.eclipse.viatra.transformation.evm.api.event.ActivationState;
import org.eclipse.viatra.transformation.evm.api.event.EventFilter;
import org.eclipse.viatra.transformation.evm.api.event.EventType;

/**
 * Abstract {@link IEVMListener} implementation.
 * 
 * @author Peter Lunk
 *
 */
public class AbstractEVMListener implements IEVMListener {

    @Override
    public void initializeListener(ViatraQueryEngine engine) {
    }

    @Override
    public void beforeFiring(Activation<?> activation) {
    }

    @Override
    public void afterFiring(Activation<?> activation) {
    }

    @Override
    public void startTransaction(String transactionID) {
    }

    @Override
    public void endTransaction(String transactionID) {
    }

    @Override
    public void activationChanged(Activation<?> activation, ActivationState oldState, EventType event) {
    }

    @Override
    public void activationCreated(Activation<?> activation, ActivationState inactiveState) {
    }

    @Override
    public void activationRemoved(Activation<?> activation, ActivationState oldState) {
    }

    @Override
    public void addedRule(RuleSpecification<?> specification, EventFilter<?> filter) {
    }

    @Override
    public void removedRule(RuleSpecification<?> specification, EventFilter<?> filter) {
    }

    @Override
    public void disposeListener() {
    }

}
