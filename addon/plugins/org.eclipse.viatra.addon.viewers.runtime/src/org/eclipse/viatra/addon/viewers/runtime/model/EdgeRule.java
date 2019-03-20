/*******************************************************************************
 * Copyright (c) 2010-2014, Csaba Debreceni, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.addon.viewers.runtime.model;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.viatra.addon.viewers.runtime.notation.Edge;
import org.eclipse.viatra.addon.viewers.runtime.notation.FormatSpecification;
import org.eclipse.viatra.addon.viewers.runtime.notation.Item;
import org.eclipse.viatra.addon.viewers.runtime.notation.NotationPackage;
import org.eclipse.viatra.addon.viewers.runtime.specifications.EdgeQuerySpecificationDescriptor;
import org.eclipse.viatra.addon.viewers.runtime.util.FormatParser;
import org.eclipse.viatra.addon.viewers.runtime.util.LabelParser;
import org.eclipse.viatra.query.runtime.api.GenericPatternMatch;
import org.eclipse.viatra.query.runtime.api.IPatternMatch;
import org.eclipse.viatra.query.runtime.api.IQuerySpecification;
import org.eclipse.viatra.query.runtime.matchers.ViatraQueryRuntimeException;
import org.eclipse.viatra.query.runtime.matchers.psystem.annotations.PAnnotation;
import org.eclipse.viatra.transformation.evm.api.Job;
import org.eclipse.viatra.transformation.evm.api.event.EventFilter;
import org.eclipse.viatra.transformation.evm.specific.Jobs;
import org.eclipse.viatra.transformation.evm.specific.crud.CRUDActivationStateEnum;
import org.eclipse.viatra.transformation.views.core.ViewModelRule;
import org.eclipse.viatra.transformation.views.core.util.ViewModelUtil;

public class EdgeRule extends ViewModelRule {

    private final EdgeQuerySpecificationDescriptor descriptor;
    private final ViewerState state;
    private ViewerDataFilter filter;

    public EdgeRule(EdgeQuerySpecificationDescriptor descriptor, ViewerState state, ViewerDataFilter filter) {
        super(descriptor);
        this.descriptor = descriptor;
        this.state = state;
        this.filter = filter;
    }

    /**
     * @throws ViatraQueryRuntimeException
     */
    public static EdgeRule initiate(IQuerySpecification<?> specification, PAnnotation annotation, ViewerState state,
            ViewerDataFilter filter) {
        EdgeQuerySpecificationDescriptor descriptor = new EdgeQuerySpecificationDescriptor(specification, annotation);
        return new EdgeRule(descriptor, state, filter);
    }

    private EventFilter<IPatternMatch> createFilter(ViewerDataFilter baseFilter) {
        if (!baseFilter.isFiltered(getBaseSpecification()))
            return null;

        ViewerFilterDefinition filterDefinition = baseFilter.getFilter(getBaseSpecification());
        return EventFilterBuilder.createEventFilter(filterDefinition, getReferencedSpecification());
    }

    @Override
    public Job<GenericPatternMatch> getAppearedJob() {
        return Jobs.newErrorLoggingJob(Jobs.newStatelessJob(CRUDActivationStateEnum.CREATED,
                match -> {
                    String sourceParam = "trace<" + descriptor.getSource() + ">";
                    String targetParam = "trace<" + descriptor.getTarget() + ">";

                    Item source = (Item) match.get(sourceParam);
                    Item target = (Item) match.get(targetParam);

                    EObject eObject = ViewModelUtil.create(NotationPackage.eINSTANCE.getEdge(),
                            state.getNotationModel(), NotationPackage.eINSTANCE.getNotationModel_Edges());
                    ViewModelUtil.trace(state.getManager(), getReferencedSpecification().getFullyQualifiedName(),
                            Collections.singleton(eObject), match.get(descriptor.getSource()),
                            match.get(descriptor.getTarget()));

                    Edge edge = (Edge) eObject;
                    edge.setSource(source);
                    edge.setTarget(target);
                    edge.setLabel(LabelParser.calculateLabel(match, descriptor.getLabel()));

                    if (descriptor.isFormatted()) {
                        FormatSpecification formatSpecification = FormatParser.parseFormatAnnotation(descriptor
                                .getFormatAnnotation());
                        edge.setFormat(formatSpecification);
                    }

                    state.edgeAppeared(edge);
                    logger.debug("Edge appeared: " + "<"+getTracedSpecification().getFullyQualifiedName()+">" + edge.toString());

                }));
    }

    @Override
    public Job<GenericPatternMatch> getDisappearedJob() {
        return Jobs.newErrorLoggingJob(Jobs.newStatelessJob(CRUDActivationStateEnum.DELETED,
                match -> {
                    if (ViewModelUtil.target(match) instanceof Edge) {
                        Collection<EObject> edges = ViewModelUtil.delete(match);
                        for (EObject edge : edges) {
                            state.edgeDisappeared((Edge) edge);
                            logger.debug("Edge disappeared: " + "<"+getTracedSpecification().getFullyQualifiedName()+">" + edge.toString());
                        }
                    }
                }));
    }

    @Override
    public Job<GenericPatternMatch> getUpdatedJob() {
        return Jobs.newErrorLoggingJob(Jobs.newStatelessJob(CRUDActivationStateEnum.UPDATED,
                match -> {
                    if (ViewModelUtil.target(match) instanceof Edge) {
                        Edge edge = (Edge) ViewModelUtil.target(match);
                        String oldLabel = edge.getLabel();
                        String newLabel = LabelParser.calculateLabel(match, descriptor.getLabel());
                        if (!oldLabel.equals(newLabel)) {
                            edge.setLabel(newLabel);
                            state.labelUpdated(edge, newLabel);
                            logger.debug("Edge updated: " + "<"+getTracedSpecification().getFullyQualifiedName()+">" + edge.toString());
                        }
                    }
                }));
    }

    @Override
    protected EventFilter<IPatternMatch> prepareFilter() {
        return createFilter(filter);
    }

}
