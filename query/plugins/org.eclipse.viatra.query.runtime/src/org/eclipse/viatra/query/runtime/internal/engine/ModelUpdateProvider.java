/*******************************************************************************
 * Copyright (c) 2010-2013, Abel Hegedus, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Abel Hegedus - initial API and implementation
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.internal.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.eclipse.viatra.query.runtime.api.AdvancedViatraQueryEngine;
import org.eclipse.viatra.query.runtime.api.IMatchUpdateListener;
import org.eclipse.viatra.query.runtime.api.IPatternMatch;
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngineLifecycleListener;
import org.eclipse.viatra.query.runtime.api.ViatraQueryMatcher;
import org.eclipse.viatra.query.runtime.api.ViatraQueryModelUpdateListener;
import org.eclipse.viatra.query.runtime.api.ViatraQueryModelUpdateListener.ChangeLevel;
import org.eclipse.viatra.query.runtime.api.scope.ViatraBaseIndexChangeListener;
import org.eclipse.viatra.query.runtime.exception.ViatraQueryException;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public final class ModelUpdateProvider extends ListenerContainer<ViatraQueryModelUpdateListener> {

    private final AdvancedViatraQueryEngine queryEngine;
    private ChangeLevel currentChange = ChangeLevel.NO_CHANGE;
    private ChangeLevel maxLevel = ChangeLevel.NO_CHANGE;
    private final Multimap<ChangeLevel, ViatraQueryModelUpdateListener> listenerMap;
    private final Logger logger;
    
    public ModelUpdateProvider(AdvancedViatraQueryEngine queryEngine, Logger logger) {
        super();
        this.queryEngine = queryEngine;
        this.logger = logger;
        Map<ChangeLevel, Collection<ViatraQueryModelUpdateListener>> map = new EnumMap<>(ChangeLevel.class);
        listenerMap = Multimaps.newSetMultimap(map, HashSet::new);
    }
    
    @Override
    protected void listenerAdded(ViatraQueryModelUpdateListener listener) {
        // check ChangeLevel
        // create callback for given level if required
        if(listenerMap.isEmpty()) {
            try {
                this.queryEngine.getBaseIndex().addBaseIndexChangeListener(indexListener);
                // add listener to new matchers (use lifecycle listener)
                this.queryEngine.addLifecycleListener(selfListener);
            } catch (ViatraQueryException e) {
                throw new IllegalStateException("Model update listener used on engine without base index", e);
            }
        }
        
        ChangeLevel changeLevel = listener.getLevel();
        listenerMap.put(changeLevel, listener);
        // increase or keep max level of listeners
        ChangeLevel oldMaxLevel = maxLevel;
        maxLevel = maxLevel.changeOccured(changeLevel); 
        if(!maxLevel.equals(oldMaxLevel) && ChangeLevel.MATCHSET.compareTo(oldMaxLevel) > 0 && ChangeLevel.MATCHSET.compareTo(maxLevel) <= 0) {
            // add matchUpdateListener to all matchers
            for (ViatraQueryMatcher<?> matcher : this.queryEngine.getCurrentMatchers()) {
                this.queryEngine.addMatchUpdateListener(matcher, matchSetListener, false);
            }
        }
    }

    @Override
    protected void listenerRemoved(ViatraQueryModelUpdateListener listener) {
        ChangeLevel changeLevel = listener.getLevel();
        boolean removed = listenerMap.remove(changeLevel, listener);
        if(!removed) {
            handleUnsuccesfulRemove(listener);
        }
        
        updateMaxLevel();
        
        if(listenerMap.isEmpty()) {
            this.queryEngine.removeLifecycleListener(selfListener);
            removeBaseIndexChangeListener();
        }
    }

    private void removeBaseIndexChangeListener() {
        try {
            this.queryEngine.getBaseIndex().removeBaseIndexChangeListener(indexListener);
        } catch (ViatraQueryException e) {
            throw new IllegalStateException("Model update listener used on engine without base index", e);
        }
    }

    private void updateMaxLevel() {
        if(!listenerMap.containsKey(maxLevel)) {
            ChangeLevel newMaxLevel = ChangeLevel.NO_CHANGE;
            for (ChangeLevel level : new HashSet<>(listenerMap.keySet())) {
                newMaxLevel = newMaxLevel.changeOccured(level);
            }
            maxLevel = newMaxLevel;
        }
        if(maxLevel.compareTo(ChangeLevel.MATCHSET) < 0) {
            // remove listener from matchers
            for (ViatraQueryMatcher<?> matcher : this.queryEngine.getCurrentMatchers()) {
                this.queryEngine.removeMatchUpdateListener(matcher, matchSetListener);
            }
        }
    }

    private void handleUnsuccesfulRemove(ViatraQueryModelUpdateListener listener) {
        for (Entry<ChangeLevel, ViatraQueryModelUpdateListener> entry : listenerMap.entries()) {
            if(entry.getValue().equals(listener)) {
                logger.error("Listener "+listener+" change level changed since initialization!");
                listenerMap.remove(entry.getKey(), entry.getValue());
                return; // listener is contained only once
            }
        }
        logger.error("Listener "+listener+" already removed from map (e.g. engine was already disposed)!");
    }

    private void notifyListeners() {
        
        // any change that occurs after this point should be regarded as a new event
        // FIXME what should happen when a listener creates new notifications?
        // -> other listeners will get events in different order
        ChangeLevel tempLevel = currentChange;
        currentChange = ChangeLevel.NO_CHANGE;
        
        if(!listenerMap.isEmpty()) {
            for (ChangeLevel level : new HashSet<>(listenerMap.keySet())) {
                if(tempLevel.compareTo(level) >= 0) {
                    for (ViatraQueryModelUpdateListener listener : new ArrayList<>(listenerMap.get(level))) {
                        try {
                            listener.notifyChanged(tempLevel);
                        } catch (Exception ex) {
                            logger.error(
                                    "VIATRA Query encountered an error in delivering model update notification to listener "
                                            + listener + ".", ex);
                        }
                    }
                }
            }
        } else {
            throw new IllegalStateException("Notify listeners must not be called without listeners! Maybe an update callback was not removed correctly.");
        }
        
    }
    
    // model update "providers":
    // - model: IQBase callback even if not dirty
    // - index: IQBase dirty callback
    private final ViatraBaseIndexChangeListener indexListener = new ViatraBaseIndexChangeListener() {
        
        public boolean onlyOnIndexChange() {
            return false;
        }
        
        public void notifyChanged(boolean indexChanged) {
            if(indexChanged) {
                currentChange = currentChange.changeOccured(ChangeLevel.INDEX);
            } else {
                currentChange = currentChange.changeOccured(ChangeLevel.MODEL);
            }
            notifyListeners();
        }
        
    };
    // - matchset: add the same listener to each matcher and use a dirty flag. needs IQBase callback as well
    private final IMatchUpdateListener<IPatternMatch> matchSetListener = new IMatchUpdateListener<IPatternMatch>() {
        
        @Override
        public void notifyDisappearance(IPatternMatch match) {
            currentChange = currentChange.changeOccured(ChangeLevel.MATCHSET);
        }
        
        @Override
        public void notifyAppearance(IPatternMatch match) {
            currentChange = currentChange.changeOccured(ChangeLevel.MATCHSET);
        }
    };
    
    private final ViatraQueryEngineLifecycleListener selfListener = new ViatraQueryEngineLifecycleListener() {
        
        @Override
        public void matcherInstantiated(ViatraQueryMatcher<? extends IPatternMatch> matcher) {
            if (maxLevel.compareTo(ChangeLevel.MATCHSET) >= 0) {
                ModelUpdateProvider.this.queryEngine.addMatchUpdateListener(matcher, matchSetListener, false);
            }
        }
        
        @Override
        public void engineWiped() {}
        
        @Override
        public void engineDisposed() {
            removeBaseIndexChangeListener();
            listenerMap.clear();
            maxLevel = ChangeLevel.NO_CHANGE;
        }
        
        @Override
        public void engineBecameTainted(String description, Throwable t) {}
    };
}
