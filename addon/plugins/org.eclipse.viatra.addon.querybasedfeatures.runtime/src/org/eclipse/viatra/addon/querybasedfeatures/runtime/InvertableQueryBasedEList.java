/*******************************************************************************
 * Copyright (c) 2010-2012, Abel Hegedus, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.addon.querybasedfeatures.runtime;

import java.util.Collection;
import java.util.List;

import org.eclipse.emf.common.util.AbstractEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

/**
 * @author Abel Hegedus
 * 
 */
public class InvertableQueryBasedEList<ComputedType, StorageType> extends AbstractEList<ComputedType> {

    private EList<StorageType> storageEList;
    private EObject sourceObject;
    private IQueryBasedFeatureHandler handler;
    private QueryBasedFeatureInverter<ComputedType, StorageType> inverter;

    /**
   * 
   */
    public InvertableQueryBasedEList(EObject sourceObject, EList<StorageType> storageEList,
            IQueryBasedFeatureHandler handler, QueryBasedFeatureInverter<ComputedType, StorageType> inverter) {
        super();
        this.storageEList = storageEList;
        this.sourceObject = sourceObject;
        this.handler = handler;
        this.inverter = inverter;
    }

    @Override
    protected ComputedType validate(int index, ComputedType object) {
        ComputedType s = super.validate(index, object);
        return inverter.validate(s);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected ComputedType primitiveGet(int index) {
        // TODO efficient reversal of index
        StorageType t = storageEList.get(index);
        List<?> values = handler.getManyReferenceValue(sourceObject);
        for (Object object : values) {
            if (inverter.invert((ComputedType) object).equals(t)) {
                return (ComputedType) object;
            }
        }
        return null;
        // NOTE indexing based on source list
        // return (Source) handler.getManyReferenceValue(sourceObject).get(index);
    }

    @Override
    public ComputedType setUnique(int index, ComputedType object) {
        ComputedType source = get(index);
        StorageType newTarget = inverter.invert(object);
        storageEList.set(index, newTarget);
        return source;
    }

    @Override
    public void addUnique(ComputedType object) {
        StorageType newTarget = inverter.invert(object);
        storageEList.add(newTarget);
    }

    @Override
    public void addUnique(int index, ComputedType object) {
        StorageType newTarget = inverter.invert(object);
        storageEList.add(index, newTarget);
    }

    @Override
    public boolean addAllUnique(Collection<? extends ComputedType> collection) {
        boolean hasChanged = false;
        for (ComputedType source : collection) {
            StorageType newTarget = inverter.invert(source);
            hasChanged |= storageEList.add(newTarget);
        }
        return hasChanged;
    }

    @Override
    public boolean addAllUnique(int index, Collection<? extends ComputedType> collection) {
        int oldSize = storageEList.size();
        int tempIndex = index;
        for (ComputedType source : collection) {
            StorageType newTarget = inverter.invert(source);
            storageEList.add(tempIndex, newTarget);
            tempIndex++;
        }
        return oldSize < storageEList.size();
    }

    @Override
    public boolean addAllUnique(Object[] objects, int start, int end) {
        boolean hasChanged = false;
        for (int i = start; i <= end; i++) {
            @SuppressWarnings("unchecked")
            StorageType newTarget = inverter.invert((ComputedType) objects[i]);
            hasChanged |= storageEList.add(newTarget);
        }
        return hasChanged;
    }

    @Override
    public boolean addAllUnique(int index, Object[] objects, int start, int end) {
        int oldSize = storageEList.size();
        int tempIndex = index;
        for (int i = start; i <= end; i++) {
            @SuppressWarnings("unchecked")
            StorageType newTarget = inverter.invert((ComputedType) objects[i]);
            storageEList.add(tempIndex, newTarget);
            tempIndex++;
        }
        return oldSize < storageEList.size();
    }

    @Override
    public ComputedType remove(int index) {
        ComputedType source = get(index);
        StorageType target = inverter.invert(source);
        storageEList.remove(target);
        return source;
    }

    @Override
    public ComputedType move(int targetIndex, int sourceIndex) {
        ComputedType tSource = get(sourceIndex);
        StorageType tTarget = inverter.invert(tSource);
        storageEList.move(targetIndex, tTarget);
        return tSource;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<ComputedType> basicList() {
        return (List<ComputedType>) handler.getManyReferenceValue(sourceObject);
    }

    @Override
    public ComputedType get(int index) {
        return basicGet(index);
    }

    @Override
    public int size() {
        return handler.getManyReferenceValue(sourceObject).size();
    }

}
