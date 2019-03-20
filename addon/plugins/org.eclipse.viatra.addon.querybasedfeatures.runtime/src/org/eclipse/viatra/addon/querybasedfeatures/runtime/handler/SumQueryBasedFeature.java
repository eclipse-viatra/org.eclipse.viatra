/*******************************************************************************
 * Copyright (c) 2010-2013, Abel Hegedus, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.addon.querybasedfeatures.runtime.handler;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.viatra.addon.querybasedfeatures.runtime.QueryBasedFeatureKind;
import org.eclipse.viatra.query.runtime.api.IPatternMatch;
import org.eclipse.viatra.query.runtime.util.ViatraQueryLoggingUtil;

/**
 * FIXME write AggregateHandler if any EDataType should be allowed TODO notifications could be static final? to ensure
 * message ordering
 * 
 * @author Abel Hegedus
 * 
 * 
 */
public class SumQueryBasedFeature extends IterationQueryBasedFeature {

    private final Map<InternalEObject, Integer> counterMemory = new HashMap<InternalEObject, Integer>();

    /**
     * @param feature
     * @param kind
     * @param keepCache
     */
    protected SumQueryBasedFeature(EStructuralFeature feature, QueryBasedFeatureKind kind) {
        super(feature, false);
        if (!(feature instanceof EAttribute)) {
            ViatraQueryLoggingUtil.getLogger(getClass()).error(
                    "[ViatraqueryFeatureHandler] Invalid configuration (Aggregate can be used only with EAttribute)!");
        }
    }

    @Override
    protected ENotificationImpl newMatchIteration(IPatternMatch signature) {
        InternalEObject source = getSourceValue(signature);
        Integer oldValue = getIntValue(source);
        Integer delta = (Integer) getTargetValue(signature);
        if (delta != null && oldValue <= Integer.MAX_VALUE - delta) {
            int tempMemory = oldValue + delta;
            counterMemory.put(source, tempMemory);
            return new ENotificationImpl(source, Notification.SET, getFeature(), getIntValue(source), tempMemory);
        } else {
            ViatraQueryLoggingUtil
                    .getLogger(getClass())
                    .error(String
                            .format("[ViatraqueryFeatureHandler] Exception during update: The counter of %s for feature %s reached the maximum value of int!",
                                    source, getFeature()));
        }
        return null;
    }

    @Override
    protected ENotificationImpl lostMatchIteration(IPatternMatch signature) {
        InternalEObject source = getSourceValue(signature);
        Integer delta = (Integer) getTargetValue(signature);
        Integer value = counterMemory.get(source);
        if (value == null) {
            ViatraQueryLoggingUtil
                    .getLogger(getClass())
                    .error("[ViatraqueryFeatureHandler] Space-time continuum breached (should never happen): decreasing a counter with no previous value");
        } else if (value >= delta) {
            int tempMemory = value - delta;
            int oldValue = value;
            counterMemory.put(source, tempMemory);
            return new ENotificationImpl(source, Notification.SET, getFeature(), oldValue, tempMemory);
        } else {
            ViatraQueryLoggingUtil
                    .getLogger(getClass())
                    .error(String
                            .format("[ViatraqueryFeatureHandler] Exception during update: The counter of %s for feature %s cannot go below zero!",
                                    source, getFeature()));
        }
        return null;
    }

    @Override
    public Object getValueIteration(Object source) {
        return getIntValue(source);
    }

    public int getIntValue(Object source) {
        Integer result = counterMemory.get(source);
        if (result == null) {
            result = 0;
        }
        return result;
    }

    @Override
    public QueryBasedFeatureKind getKind() {
        return QueryBasedFeatureKind.SUM;
    }

    @Override
    public Object getValue(Object source) {
        return getIntValue(source);
    }

}
