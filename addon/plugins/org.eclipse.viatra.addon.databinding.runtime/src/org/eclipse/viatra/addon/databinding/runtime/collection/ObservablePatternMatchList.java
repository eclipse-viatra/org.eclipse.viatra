/*******************************************************************************
 * Copyright (c) 2010-2012, Abel Hegedus, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Abel Hegedus - initial API and implementation
 *******************************************************************************/
package org.eclipse.viatra.addon.databinding.runtime.collection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.ObservableTracker;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.list.AbstractObservableList;
import org.eclipse.core.databinding.observable.list.ListDiff;
import org.eclipse.core.databinding.observable.list.ListDiffEntry;
import org.eclipse.core.runtime.Assert;
import org.eclipse.viatra.query.runtime.api.IPatternMatch;
import org.eclipse.viatra.query.runtime.api.IQuerySpecification;
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngine;
import org.eclipse.viatra.query.runtime.api.ViatraQueryMatcher;
import org.eclipse.viatra.transformation.evm.api.ExecutionSchema;
import org.eclipse.viatra.transformation.evm.api.RuleEngine;
import org.eclipse.viatra.transformation.evm.api.RuleSpecification;
import org.eclipse.viatra.transformation.evm.api.event.EventFilter;
import org.eclipse.viatra.transformation.evm.specific.Rules;

/**
 * Observable view of a match set for a given {@link ViatraQueryMatcher} on a model (match sets of an
 * {@link ViatraQueryMatcher} are ordered by the order of their appearance).
 * 
 * <p>
 * For creating complex observable lists, use {@link ObservablePatternMatchCollectionBuilder}.
 * 
 * <p>
 * This implementation uses the {@link ExecutionSchema} to get notifications for match set changes, and can be
 * instantiated using either an existing {@link ViatraQueryMatcher}, or an {@link IQuerySpecification} and either a
 * {@link ViatraQueryEngine} or {@link ExecutionSchema}.
 * 
 * <p>
 * Note that a converter can be used for the list, in which case the type of list items may be different from
 * the generic type parameter Match.
 * 
 * @author Abel Hegedus
 * 
 */
public class ObservablePatternMatchList<Match extends IPatternMatch> extends AbstractObservableList {

    private final List<Match> cache = Collections.synchronizedList(new LinkedList<Match>());
    private ListCollectionUpdate updater;
    private RuleSpecification<Match> specification;
    private EventFilter<Match> matchFilter;
    private RuleEngine ruleEngine;
    private boolean privateRuleEngine;
    
    private ObservablePatternMatchCollection<Match> internalCollection;
    
    /**
     * Creates an observable list, that will be built be the {@link ObservablePatternMatchCollectionBuilder}
     * using the {@link ObservablePatternMatchCollection} interface.
     */
    protected ObservablePatternMatchList() {
        this.internalCollection = new ObservablePatternMatchCollection<Match>() {

            @Override
            public void createUpdater(Function<Match, ?> converter, Comparator<Match> comparator) {
                updater = new ListCollectionUpdate(converter, comparator);
            }

            @Override
            public void createRuleSpecification(IQuerySpecification<? extends ViatraQueryMatcher<Match>> querySpecification) {
                specification = ObservableCollectionHelper.createRuleSpecification(updater, querySpecification);
            }

            @Override
            public void createRuleSpecification(ViatraQueryMatcher<Match> matcher) {
                specification = ObservableCollectionHelper.createRuleSpecification(updater, matcher);
            }

            @Override
            public void setFilter(EventFilter<Match> filter) {
                if(filter == null) {
                    matchFilter = specification.createEmptyFilter();
                } else {
                    matchFilter = filter;
                }
            }

            @Override
            public void initialize(ViatraQueryEngine engine) {
                ruleEngine = ObservableCollectionHelper.prepareRuleEngine(engine, specification, matchFilter);
                privateRuleEngine = true;
            }

            @Override
            public void initialize(RuleEngine engine) {
                ruleEngine = engine;
                privateRuleEngine = false;
                engine.addRule(specification, matchFilter);
                ObservableCollectionHelper.fireActivations(engine, specification, matchFilter);
            }
            
        };
        
    }

    protected ObservablePatternMatchCollection<Match> getInternalCollection() {
        return internalCollection;
    }
    
    @Override
    public void clear() {
        this.cache.clear();
        this.updater.clear();
    }

    @Override
    public synchronized void dispose() {
        if (ruleEngine != null) {
            ruleEngine.removeRule(specification, matchFilter);
            if (privateRuleEngine && ruleEngine.getRuleSpecificationMultimap().isEmpty()) {
                ObservableCollectionHelper.disposeRuleEngine(ruleEngine);
            }
            ruleEngine = null;
        }
        clear();
        super.dispose();
    }
    
    @Override
    public Object getElementType() {
        if (updater.converter != null) {
            // XXX we cannot get the class of the generic parameter or the return type of the converter
            return null;
        }
        return IPatternMatch.class;
    }

    @Override
    protected int doGetSize() {
        return cache.size();
    }

    @Override
    public Object get(int index) {
        if (updater.converter != null) {
            return updater.matchToItem.get(cache.get(index));
        }
        return cache.get(index);
    }

    private void getterCalled() {
        ObservableTracker.getterCalled(this);
    }
    
    /**
     * @TrackedGetter This method will notify ObservableTracker that the receiver has been read from
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Iterator iterator() {
        getterCalled();
        if (updater.converter != null) {
            final Iterator<Match> iterator = cache.iterator();
            // XXX the Iterator type is object as we have no idea what item type is used by the converter
            return new Iterator<Object>() {

                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public Object next() {
                    Match next = iterator.next();
                    return updater.matchToItem.get(next);
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("This iterator does not support element removal!");
                }

            };
        }
        return cache.iterator();
    }

    /**
     * @return the specification
     */
    public RuleSpecification<Match> getSpecification() {
        return specification;
    }
    
    /**
     * Update the filter used by the observable during runtime.
     * The contents of the observable are updated and the diff is sent to observers.
     * 
     * @param filter
     */
    public void setFilter(Match filter) {
        
        EventFilter<Match> oldFilter = matchFilter;
        matchFilter = Rules.newSingleMatchFilter(filter);
        if(Objects.equals(matchFilter, oldFilter)) {
            // same filter, do nothing
            return;
        }
        
        if(oldFilter == null) {
            ruleEngine.removeRule(specification);
        } else {
            ruleEngine.removeRule(specification, oldFilter);
        }
        
        List<Match> oldCache = updater.pauseUpdates();
        // delay removal to maintain list order
        updater.removed.addAll(oldCache);
        if(filter == null) {
            ruleEngine.addRule(specification);
        } else {
            ruleEngine.addRule(specification, matchFilter);
        }
        // new items are added back through updater
        ObservableCollectionHelper.fireActivations(ruleEngine, specification, matchFilter);
        // send combined notification
        updater.resumeUpdates();
        
    }

    public class ListCollectionUpdate implements IObservablePatternMatchCollectionUpdate<Match> {

        protected static final String DATA_BINDING_REALM_MUST_NOT_BE_NULL = "Data binding Realm must not be null";
        protected final Function<Match, ?> converter;
        protected final Comparator<Match> comparator;
        protected final Map<Match, Object> matchToItem = new HashMap<Match, Object>();
        protected ListDiff nextDiff = null;
        private List<Match> oldCache = null;
        private Set<Match> removed;

        /**
         * @since 2.0
         */
        public ListCollectionUpdate(Function<Match, ?> converter, Comparator<Match> comparator) {
            if (converter != null) {
                this.converter = converter;
            } else {
                this.converter = null;
            }
            this.comparator = comparator;
        }

        private int placeOf(Match match) {
            if (cache.isEmpty()) {
                return 0;
            }
            // tradeoff between the ArrayList get(int) and add(int, E) vs LinkedList get(int) and add(int, E)
            // stick to LinkedList and binary search so get(int) will be invoked not that many times (log2) and add(int,
            // E) is more efficient
            int left = 0;
            int right = cache.size() - 1;
            while (left <= right) {
                int mid = (left + right) >> 1;
                int cv = comparator.compare(match, cache.get(mid));
                if (cv == 0) {
                    return mid;
                }
                if (cv < 0) {
                    right = mid - 1;
                } else {
                    left = mid + 1;
                }
            }
            return left;
        }

        @Override
        public void addMatch(Match match) {
            ListDiffEntry diffentry = addItem(match);
            sendListUpdates(diffentry);
        }

        @Override
        public void removeMatch(Match match) {
            ListDiffEntry diffentry = removeItem(match);
            sendListUpdates(diffentry);
        }

        private void sendListUpdates(ListDiffEntry diffentry) {
            if(nextDiff == null) {
                final ListDiff diff = Diffs.createListDiff(diffentry);
                sendListUpdate(diff);
            }
        }

        private void sendListUpdate(final ListDiff diff) {
            Realm realm = getRealm();
            Assert.isNotNull(realm, DATA_BINDING_REALM_MUST_NOT_BE_NULL);
            realm.exec(() -> {
                if (!isDisposed()) {
                    fireListChange(diff);
                }
            });
        }

        private ListDiffEntry addItem(Match match) {
            if(removed != null && removed.remove(match)) {
                // item remains in list
                return null;
            } else {
                Object item = match;
                if (converter != null) {
                    item = converter.apply(match);
                    matchToItem.put(match, item);
                }
                final int index = (comparator == null ? cache.size() : placeOf(match));
                ListDiffEntry diffentry = Diffs.createListDiffEntry(index, true, item);
                cache.add(index, match);
                return diffentry;
            }
        }

        private ListDiffEntry removeItem(Match match) {
            Object item = match;
            if (converter != null) {
                item = matchToItem.remove(match);
            }
            final int index = cache.indexOf(match);
            ListDiffEntry diffentry = Diffs.createListDiffEntry(index, false, item);
            cache.remove(match);
            return diffentry;
        }
        
        private List<Match> pauseUpdates() {
            if(nextDiff == null) {
                oldCache = new ArrayList<>(cache);
                nextDiff = Diffs.computeLazyListDiff(oldCache,cache);
                removed = new HashSet<>();
                return oldCache;
            }
            return Collections.emptyList();
        }
        
        private void resumeUpdates() {
            if(nextDiff != null) {
                List<ListDiffEntry> entries = new ArrayList<>(removed.size());
                for (Match match : removed) {
                    // delayed removal of items
                    ListDiffEntry diffEntry = removeItem(match);
                    entries.add(diffEntry);
                }
                // convert lazy diff to real diff
                nextDiff = Diffs.createListDiff(nextDiff.getDifferences());
                if(!nextDiff.isEmpty()) {
                    for (ListDiffEntry entry : nextDiff.getDifferences()) {
                        // removals already processed
                        if(entry.isAddition()) {
                            // items already in cache at the proper place
                            int index = entry.getPosition();
                            Object item = get(index);
                            ListDiffEntry diffentry = Diffs.createListDiffEntry(index, true, item);
                            entries.add(diffentry);
                        }
                    }
                    ListDiff diff = Diffs.createListDiff(entries.toArray(new ListDiffEntry[entries.size()]));
                    sendListUpdate(diff);
                }
            }
            nextDiff = null; 
            oldCache = null;
            removed = null;
        }

        @Override
        public void clear() {
            this.matchToItem.clear();
        }
    }
}
