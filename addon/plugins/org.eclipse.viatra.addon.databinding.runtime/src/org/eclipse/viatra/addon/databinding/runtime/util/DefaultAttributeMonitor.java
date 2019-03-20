/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Abel Hegedus, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.viatra.addon.databinding.runtime.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.emf.databinding.EMFProperties;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.viatra.query.runtime.api.IPatternMatch;
import org.eclipse.viatra.transformation.evm.notification.AttributeMonitor;

/**
 * Default implementation of the {@link AttributeMonitor} that uses EMF Data binding to 
 * watch the values of each feature of each object in matches.
 * 
 * @author Abel Hegedus
 *
 * @param <MatchType>
 */
public class DefaultAttributeMonitor<MatchType extends IPatternMatch> extends AttributeMonitor<MatchType> {

    private ChangeListener changeListener;
    private Map<IObservableValue, MatchType> observableMap;
    private Map<MatchType, List<IObservableValue>> observableMapReversed;

    public DefaultAttributeMonitor() {
        super();
        this.changeListener = new ChangeListener();
        this.observableMap = new HashMap<IObservableValue, MatchType>();
        this.observableMapReversed = new HashMap<MatchType, List<IObservableValue>>();
    }

    /**
     * Simple change listener implementation that sends a notification on each change.
     * 
     * @author Abel Hegedus
     *
     */
    private class ChangeListener implements IValueChangeListener {
        @Override
        public void handleValueChange(final ValueChangeEvent event) {
            IObservableValue val = event.getObservableValue();
            if (val != null) {
                notifyListeners(observableMap.get(val));
            }
        }
    }

    @Override
    public void registerFor(final MatchType atom) {
        List<IObservableValue> values = new ArrayList<IObservableValue>();
            for (String param : atom.parameterNames()) {
                Object location = atom.get(param);
                List<IObservableValue> observableValues = observeAllAttributes(changeListener, location);
                values.addAll(observableValues);
            }
            
            // inserting {observable,match} pairs
            for (IObservableValue val : values) {
                observableMap.put(val, atom);
            }

        // inserting {match, list(observable)} pairs
        observableMapReversed.put(atom, values);
    }

    /**
     * Iterates on all features and returns a list of observable values.
     * 
     * @param changeListener
     * @param object
     * @return
     */
    private List<IObservableValue> observeAllAttributes(final IValueChangeListener changeListener, final Object object) {
        List<IObservableValue> affectedValues = new ArrayList<IObservableValue>();
        if (object instanceof EObject) {
            for (EStructuralFeature feature : ((EObject) object).eClass().getEAllStructuralFeatures()) {
                IObservableValue val = EMFProperties.value(feature).observe(object);
                affectedValues.add(val);
                val.addValueChangeListener(changeListener);
            }
        }
        return affectedValues;
    }

    @Override
    public void unregisterForAll() {
        for (MatchType atom : observableMapReversed.keySet()) {
            unregisterFor(atom);
        }
    }

    @Override
    public void unregisterFor(final MatchType atom) {
        List<IObservableValue> observables = observableMapReversed.get(atom);
        if (observables != null) {
            for (IObservableValue val : observables) {
                val.removeValueChangeListener(changeListener);
            }
        }
    }
}