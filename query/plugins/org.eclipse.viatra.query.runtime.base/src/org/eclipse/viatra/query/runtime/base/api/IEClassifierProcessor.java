/*******************************************************************************
 * Copyright (c) 2010-2013, Abel Hegedus, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.base.api;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;

/**
 * @author Abel Hegedus
 *
 */
public interface IEClassifierProcessor<ClassType, InstanceType> {
    
    void process(ClassType type, InstanceType instance);
    
    public interface IEClassProcessor extends IEClassifierProcessor<EClass, EObject>{}
    public interface IEDataTypeProcessor extends IEClassifierProcessor<EDataType, Object>{}
}
