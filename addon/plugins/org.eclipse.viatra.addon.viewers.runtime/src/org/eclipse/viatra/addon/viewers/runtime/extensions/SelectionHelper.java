/*******************************************************************************
 * Copyright (c) 2010-2013, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.addon.viewers.runtime.extensions;

import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.viatra.addon.viewers.runtime.model.ViewerState;
import org.eclipse.viatra.addon.viewers.runtime.notation.Edge;
import org.eclipse.viatra.addon.viewers.runtime.notation.Item;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Helper class for bidirectional selection synchronization support for
 * VIATRA Viewers components.
 * 
 * @author istvanrath
 *
 */
public class SelectionHelper {

    private final Set<ISelectionChangedListener> selectionChangedListeners = Sets.newHashSet();
    
    private final ISelectionChangedListener trickyListener = event -> {
        for (ISelectionChangedListener l : selectionChangedListeners) {
            l.selectionChanged(new SelectionChangedEvent(event.getSelectionProvider(), unwrapElements_ViewersElementsToEObjects(event.getSelection())));
        }
    };

    public Iterable<ISelectionChangedListener> getSelectionChangedListeners() {
        return selectionChangedListeners;
    }

    public boolean addSelectionChangedListener(ISelectionChangedListener listener) {
        return selectionChangedListeners.add(listener);
    }
    
    public boolean removeSelectionChangedListener(ISelectionChangedListener listener) {
        return selectionChangedListeners.remove(listener);
    }
    
    public ISelectionChangedListener getTrickyListener() {
        return trickyListener;
    }

    private Object getSourceObject(Item i) {
        if (i.getParamEObject() != null) {
            return i.getParamEObject();
        } else if (i.getParamObject() != null) {
            return i.getParamObject();
        } else {
            throw new IllegalStateException("Invalid Item selected - no source model element available.");
        }
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ISelection unwrapElements_ViewersElementsToEObjects(ISelection sel) {
        List proxy = Lists.newArrayList();
        if (sel instanceof IStructuredSelection) {
            for (Object e : ((IStructuredSelection)sel).toArray()) {
                if (e instanceof Item) {
                    proxy.add(getSourceObject((Item)e));
                }
                else if (e instanceof Edge) {
                    proxy.add(getSourceObject(((Edge) e).getSource()));
                    proxy.add(getSourceObject(((Edge) e).getTarget()));
                }
            }
        }
        return new StructuredSelection(proxy);
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ISelection unwrapElements_EObjectsToViewersElements(ISelection sel, ViewerState state) {
        List proxy = Lists.newArrayList();
            if (state!=null && sel instanceof IStructuredSelection) {
                for (Object e : ((IStructuredSelection)sel).toArray()) {
                    if (e instanceof EObject) {
                        proxy.addAll(state.getItemsFor(e));
                    }
                }
            }
        return new StructuredSelection(proxy);
    }
}
