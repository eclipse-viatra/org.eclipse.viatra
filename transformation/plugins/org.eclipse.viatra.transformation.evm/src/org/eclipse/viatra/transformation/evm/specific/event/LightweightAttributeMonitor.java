/*******************************************************************************
 * Copyright (c) 2010-2013, Abel Hegedus, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.transformation.evm.specific.event;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.viatra.query.runtime.api.IPatternMatch;
import org.eclipse.viatra.query.runtime.api.scope.IBaseIndex;
import org.eclipse.viatra.query.runtime.api.scope.IInstanceObserver;
import org.eclipse.viatra.transformation.evm.notification.AttributeMonitor;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * @author Abel Hegedus
 *
 */
public class LightweightAttributeMonitor<MatchType extends IPatternMatch> extends AttributeMonitor<MatchType> {

    private IInstanceObserver observer;
    private Multimap<Object, MatchType> observedMultimap;
    private IBaseIndex index;
    
    public LightweightAttributeMonitor(IBaseIndex index) {
        super();
        this.index = index;
        this.observer = new IInstanceObserver() {
            @Override
            public void notifyBinaryChanged(Object sourceElement,
                    Object edgeType) {
                Collection<MatchType> matches = observedMultimap.get(sourceElement);
                for (MatchType matchType : matches) {
                    notifyListeners(matchType);
                }
            }
            
            @Override
            public void notifyTernaryChanged(Object sourceElement,
                    Object edgeType) {
                Collection<MatchType> matches = observedMultimap.get(sourceElement);
                for (MatchType matchType : matches) {
                    notifyListeners(matchType);
                }
            }        
        };
        this.observedMultimap = HashMultimap.create();
    }
    
    @Override
    public void registerFor(MatchType atom) {
        Collection<Object> allObjects = findAllObjects(atom);
        for (Object object : allObjects) {
            index.addInstanceObserver(observer, object);
            observedMultimap.put(object, atom);
        }
    }

    @Override
    public void unregisterForAll() {
        for (Object eobj : observedMultimap.keySet()) {
            index.removeInstanceObserver(observer, eobj);
        }
    }

    @Override
    public void unregisterFor(MatchType atom) {
        Collection<Object> allObjects = findAllObjects(atom);
        for (Object object : allObjects) {
            index.removeInstanceObserver(observer, object);
            observedMultimap.remove(object, atom);
        }
    }

    private Collection<Object> findAllObjects(MatchType atom){
        Collection<Object> objs = new ArrayList<>();
        for (String param : atom.parameterNames()) {
            Object location = atom.get(param);
            objs.add(location);
        }
        return objs;
    }
    
}
