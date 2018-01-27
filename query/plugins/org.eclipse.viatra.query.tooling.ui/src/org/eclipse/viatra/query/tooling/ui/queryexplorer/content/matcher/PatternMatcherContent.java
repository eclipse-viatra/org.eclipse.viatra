/*******************************************************************************
 * Copyright (c) 2010-2012, Zoltan Ujhelyi, Tamas Szabo, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Zoltan Ujhelyi, Tamas Szabo - initial API and implementation
 *******************************************************************************/

package org.eclipse.viatra.query.tooling.ui.queryexplorer.content.matcher;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;

import org.eclipse.core.databinding.observable.list.IListChangeListener;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.ListChangeEvent;
import org.eclipse.core.databinding.observable.list.ListDiffEntry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.viatra.addon.databinding.runtime.collection.ObservablePatternMatchCollectionBuilder;
import org.eclipse.viatra.addon.databinding.runtime.collection.ObservablePatternMatchList;
import org.eclipse.viatra.query.runtime.api.AdvancedViatraQueryEngine;
import org.eclipse.viatra.query.runtime.api.IPatternMatch;
import org.eclipse.viatra.query.runtime.api.IQuerySpecification;
import org.eclipse.viatra.query.runtime.api.ViatraQueryMatcher;
import org.eclipse.viatra.query.runtime.exception.ViatraQueryException;
import org.eclipse.viatra.query.runtime.matchers.backend.IQueryBackendFactory;
import org.eclipse.viatra.query.runtime.matchers.backend.QueryEvaluationHint;
import org.eclipse.viatra.query.runtime.matchers.psystem.annotations.PAnnotation;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PQuery.PQueryStatus;
import org.eclipse.viatra.query.tooling.ui.ViatraQueryGUIPlugin;
import org.eclipse.viatra.query.tooling.ui.queryexplorer.QueryExplorer;
import org.eclipse.viatra.query.tooling.ui.queryexplorer.util.DisplayUtil;
import org.eclipse.viatra.query.tooling.ui.util.IFilteredMatcherContent;
import org.eclipse.viatra.transformation.evm.api.RuleEngine;

/**
 * The middle level element in the tree viewer of the {@link QueryExplorer}. Instances of this class represent the
 * various patterns (generated or runtime) that are loaded during runtime.
 * 
 * @author Tamas Szabo (itemis AG)
 * 
 */
@SuppressWarnings({ "unchecked" })
public class PatternMatcherContent extends CompositeContent<PatternMatcherRootContent, PatternMatchContent> implements IFilteredMatcherContent {

    private static final String KEY_ATTRIBUTE_OF_ORDER_BY_ANNOTATION = "The key attribute of OrderBy annotation must look like \"ClassName.AttributeName\"!";
    private final boolean generated;
    private IPatternMatch filter;
    private Object[] parameterFilter;
    private String orderParameter;
    private boolean ascendingOrder;
    private String exceptionMessage;
    private Exception exception;
    private IQuerySpecification<?> specification;
    private ViatraQueryMatcher<IPatternMatch> matcher;
    /**
     * XXX Note that the generic type is not the same as the type of the items in the list
     */
    private ObservablePatternMatchList<IPatternMatch> children;
    private TransformerFunction transformerFunction;
    private MatchComparator matchComparator;
    private IListChangeListener listChangeListener;

    private IQueryBackendFactory usedBackend = null;
    
    public PatternMatcherContent(PatternMatcherRootContent parent, AdvancedViatraQueryEngine engine, RuleEngine ruleEngine, 
            final IQuerySpecification<?> specification, boolean generated, QueryEvaluationHint hint) {
        super(parent);
        this.specification = specification;
        this.transformerFunction = new TransformerFunction(this);
        this.listChangeListener = new ListChangeListener();

        if (specification.getInternalQueryRepresentation().getStatus() != PQueryStatus.ERROR) {
            try {
                usedBackend = hint.getQueryBackendFactory();
                matcher = (ViatraQueryMatcher<IPatternMatch>) engine.getMatcher(specification, hint);
            } catch (ViatraQueryException e) {
                this.exceptionMessage = e.getShortMessage();
                this.exception = e;
                logException();
            } catch (Exception e) {
                this.exceptionMessage = e.getMessage();
                this.exception = e;
                logException();
            }
        }

        this.generated = generated;
        this.orderParameter = null;

        DisplayUtil.removeOrderByPatternWarning(specification.getFullyQualifiedName());
        
        if (this.matcher != null) {
            initOrdering();
            initFilter();
            
            ObservablePatternMatchCollectionBuilder<IPatternMatch> builder = ObservablePatternMatchCollectionBuilder.create((IQuerySpecification<? extends ViatraQueryMatcher<IPatternMatch>>) specification);
            builder.setComparator(matchComparator).setConverter(transformerFunction).setFilter(filter);
            children = builder.setEngine(ruleEngine).buildList();
            children.addListChangeListener(listChangeListener);
            // label needs to be set explicitly, in case of no matches setText will not be invoked at all
            setText(DisplayUtil.getMessage(matcher, children.size(), specification.getFullyQualifiedName(),
                    isGenerated(), isFiltered(), exceptionMessage, usedBackend));
        }
        else {
            setText(DisplayUtil.getMessage(null, 0, specification.getFullyQualifiedName(),
                    isGenerated(), isFiltered(), exceptionMessage, null));
        }
    }
    
    /**
     * @param e
     */
    private void logException() {
        String logMessage = 
                String.format(
                        "Query Explorer has encountered an error during evaluation of query %s%s",
                        specification.getFullyQualifiedName(),
                        exceptionMessage == null ? "" : (": " + exceptionMessage)
                );
        ViatraQueryGUIPlugin.getDefault().getLog().log(new Status(
                IStatus.ERROR, 
                ViatraQueryGUIPlugin.getDefault().getBundle().getSymbolicName(), 
                logMessage, 
                exception)
        );
    }

    @Override
    public void dispose() {
        super.dispose();
        this.matcher = null;
        this.specification = null;
        if(this.children != null) {
            this.children.removeListChangeListener(listChangeListener);
        }
        this.listChangeListener = null;
    }

    private class ListChangeListener implements IListChangeListener {

        @Override
        public void handleListChange(ListChangeEvent event) {
            for (ListDiffEntry entry : event.diff.getDifferences()) {
                if (!entry.isAddition()) {
                    ((PatternMatchContent) entry.getElement()).dispose();
                } else {
                    ((PatternMatchContent) entry.getElement()).initialize();
                }
            }

            setText(DisplayUtil.getMessage(matcher, children.size(), specification.getFullyQualifiedName(),
                    isGenerated(), isFiltered(), exceptionMessage, usedBackend));
            
            // 462706 - if no match is present initially, the user will not be able to expand it otherwise
            updateHasChildren();
        }

    }

    /**
     * Initializes the matcher for ordering if the annotation is present.
     */
    private void initOrdering() {
        PAnnotation annotation = specification.getFirstAnnotationByName(DisplayUtil.ORDERBY_ANNOTATION);
        if (annotation != null) {
            for (Entry<String, Object> ap : annotation.getAllValues()) {
                if (ap.getKey().matches("key")) {
                    orderParameter = (String) ap.getValue();
                }
                if (ap.getKey().matches("direction")) {
                    String direction = ((String) ap.getValue());
                    if (direction.matches("asc")) {
                        ascendingOrder = true;
                    } else {
                        ascendingOrder = false;
                    }
                }
            }

            if (orderParameter != null) {
                String[] tokens = orderParameter.split("\\.");
                if (tokens.length != 2) {
                    DisplayUtil.addOrderByPatternWarning(this.matcher.getPatternName(),
                            KEY_ATTRIBUTE_OF_ORDER_BY_ANNOTATION);
                } else {
                    matchComparator = new MatchComparator(matcher, tokens[0], tokens[1], ascendingOrder);
                }
            }
        }
    }

    public IQuerySpecification<?> getSpecification() {
        return specification;
    }

    public ViatraQueryMatcher<IPatternMatch> getMatcher() {
        return matcher;
    }

    public String getPatternName() {
        return specification.getFullyQualifiedName();
    }

    private void initFilter() {
        if (matcher != null) {
            final int arity = this.matcher.getParameterNames().size();
            parameterFilter = new Object[arity];
            this.filter = this.matcher.newMatch(parameterFilter);
        }
    }

    public void setFilter(Object[] parameterFilter) {
        this.parameterFilter = parameterFilter.clone();
        this.filter = this.matcher.newMatch(this.parameterFilter);
        this.children.setFilter(filter);
    }

    private boolean isFiltered() {
        if (matcher != null) {
            for (int i = 0; i < this.matcher.getParameterNames().size(); i++) {
                if (parameterFilter[i] != null) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns the current filter used on the corresponding matcher.
     * 
     * @return the filter as an array of objects
     */
    public Object[] getFilter() {
        return parameterFilter;
    }

    /**
     * @since 1.4
     */
    @Override
    public IPatternMatch getFilterMatch() {
        return matcher.newMatch(parameterFilter);
    }

    /**
     * Returns true if the matcher is generated, false if it is generic.
     * 
     * @return true for generated, false for generic matcher
     */
    public boolean isGenerated() {
        return generated;
    }

    /**
     * Returns true if the RETE matcher was created for this observable matcher, false otherwise.
     * 
     * @return true if matcher could be created
     */
    public boolean isCreated() {
        return specification.getInternalQueryRepresentation().getStatus() == PQueryStatus.OK && matcher != null;
    }

    @Override
    public IObservableList getChildren() {
        return children;
    }
    
    public Exception getException() {
        return exception;
    }

    @Override
    public Iterator<PatternMatchContent> getChildrenIterator() {
        /*
         *  XXX the iterator is Iterator<Object> but its contents are guaranteed to be PatternMatchContent,
         *  only compiles because of raw types and works due to type erasure on generic collections
         */
        return children.iterator();
    }

    @Override
    public Collection getFilteredMatches() {
        return getMatcher().getAllMatches(getFilterMatch());
    }

    @Override
    public int countFilteredMatches() {
        return getMatcher().countMatches(getFilterMatch());
    }

    @Override
    public boolean hasFilteredMatch() {
        return getMatcher().hasMatch(getFilterMatch());
    }
}
