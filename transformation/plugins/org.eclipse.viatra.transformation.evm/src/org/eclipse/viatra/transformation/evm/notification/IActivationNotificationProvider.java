/*******************************************************************************
 * Copyright (c) 2010-2012, Abel Hegedus, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.transformation.evm.notification;

/**
 * This interface is used for providing an internal activation notification mechanism.
 * Listeners added to the implemented provider should be notified when an activation change occurs.
 * 
 * @author Abel Hegedus
 * 
 */
public interface IActivationNotificationProvider {

    /**
     * Registers an {@link IActivationNotificationListener} to receive updates on activation appearance and
     * disappearance.
     * 
     * <p>
     * The listener can be unregistered via
     * {@link #removeActivationNotificationListener(IActivationNotificationListener)}.
     * 
     * @param fireNow
     *            if true, listener will be immediately invoked on all current activations as a one-time effect.
     * 
     * @param listener
     *            the listener that will be notified of each new activation that appears or disappears, starting from
     *            now.
     */
    boolean addActivationNotificationListener(final IActivationNotificationListener listener, final boolean fireNow);

    /**
     * Unregisters a listener registered by
     * {@link #addActivationNotificationListener(IActivationNotificationListener, boolean)}.
     * 
     * @param listener
     *            the listener that will no longer be notified.
     */
    boolean removeActivationNotificationListener(final IActivationNotificationListener listener);

}