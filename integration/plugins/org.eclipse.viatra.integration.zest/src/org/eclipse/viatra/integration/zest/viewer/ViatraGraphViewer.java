/*******************************************************************************
 * Copyright (c) 2010-2023, stampie, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.integration.zest.viewer;

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.GestureEvent;
import org.eclipse.swt.events.GestureListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.viatra.integration.zest.viewer.internal.GraphModelEdgeFactory;
import org.eclipse.zest.core.viewers.GraphViewer;
import org.eclipse.zest.core.viewers.IGraphContentProvider;
import org.eclipse.zest.core.viewers.IGraphEntityContentProvider;
import org.eclipse.zest.core.viewers.IGraphEntityRelationshipContentProvider;
import org.eclipse.zest.core.viewers.internal.IStylingGraphModelFactory;
import org.eclipse.zest.core.widgets.Graph;

/**
 * 
 */
@SuppressWarnings("restriction")
public class ViatraGraphViewer extends GraphViewer {

    private IStylingGraphModelFactory overriddenModelFactory = null;
    
    private static final double MINIMUM_ZOOM = 0.1;
    private static final double MAXIMUM_ZOOM = 10.0;
    
    public ViatraGraphViewer(Composite composite, int style) {
        super(composite, style);
        
        Graph graph = getGraphControl();
        graph.addGestureListener(new GestureListener() {

            double zoom = 1.0;

            public void gesture(GestureEvent e) {
                switch (e.detail) {
                case SWT.GESTURE_BEGIN:
                    zoom = graph.getRootLayer().getScale();
                    break;
                case SWT.GESTURE_END:
                    break;
                case SWT.GESTURE_MAGNIFY:
                    double newValue = zoom * e.magnification;
                    if (newValue >= MINIMUM_ZOOM && newValue <= MAXIMUM_ZOOM) {
                        graph.getRootLayer().setScale(newValue);
                        graph.getRootLayer().repaint();
                    }
                    break;
                default:
                    // Do nothing
                }
            }
        });
    }
    
    @Override
    public void setContentProvider(IContentProvider contentProvider) {
        if (contentProvider instanceof IGraphContentProvider) {
            overriddenModelFactory = null;
            super.setContentProvider(contentProvider);
        } else if (contentProvider instanceof IGraphEntityContentProvider) {
            overriddenModelFactory = null;
            super.setContentProvider(contentProvider);
        } else if (contentProvider instanceof IGraphEntityRelationshipContentProvider) {
            overriddenModelFactory = null;
            super.setContentProvider(contentProvider);
        } else if (contentProvider instanceof IGraphEdgeContentProvider) {
            overriddenModelFactory = null;
            super.setContentProvider(contentProvider);
        } else {
            throw new IllegalArgumentException(
                    "Invalid content provider, only IGraphContentProvider, IGraphEntityContentProvider, or IGraphEntityRelationshipContentProvider are supported.");
        }
    }

    @Override
    protected IStylingGraphModelFactory getFactory() {
        if (overriddenModelFactory == null) {
            if (getContentProvider() instanceof IGraphEdgeContentProvider) {
                overriddenModelFactory = new GraphModelEdgeFactory(this);
            } else {
                return super.getFactory();
            }
        }
        return overriddenModelFactory;
    }

}
