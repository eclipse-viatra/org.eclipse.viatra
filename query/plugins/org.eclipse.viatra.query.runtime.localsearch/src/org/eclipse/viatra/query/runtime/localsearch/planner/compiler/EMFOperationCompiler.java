/*******************************************************************************
 * Copyright (c) 2010-2017, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Zoltan Ujhelyi - initial API and implementation
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.localsearch.planner.compiler;

import java.util.Map;

import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.viatra.query.runtime.emf.EMFQueryRuntimeContext;
import org.eclipse.viatra.query.runtime.emf.types.EClassTransitiveInstancesKey;
import org.eclipse.viatra.query.runtime.emf.types.EClassUnscopedTransitiveInstancesKey;
import org.eclipse.viatra.query.runtime.emf.types.EDataTypeInSlotsKey;
import org.eclipse.viatra.query.runtime.emf.types.EStructuralFeatureInstancesKey;
import org.eclipse.viatra.query.runtime.localsearch.operations.check.InstanceOfClassCheck;
import org.eclipse.viatra.query.runtime.localsearch.operations.check.InstanceOfDataTypeCheck;
import org.eclipse.viatra.query.runtime.localsearch.operations.check.InstanceOfJavaClassCheck;
import org.eclipse.viatra.query.runtime.localsearch.operations.check.StructuralFeatureCheck;
import org.eclipse.viatra.query.runtime.localsearch.operations.check.nobase.ScopeCheck;
import org.eclipse.viatra.query.runtime.localsearch.operations.extend.ExtendToEStructuralFeatureSource;
import org.eclipse.viatra.query.runtime.localsearch.operations.extend.ExtendToEStructuralFeatureTarget;
import org.eclipse.viatra.query.runtime.localsearch.operations.extend.IterateOverContainers;
import org.eclipse.viatra.query.runtime.localsearch.operations.extend.IterateOverEClassInstances;
import org.eclipse.viatra.query.runtime.localsearch.operations.extend.IterateOverEDatatypeInstances;
import org.eclipse.viatra.query.runtime.matchers.context.IInputKey;
import org.eclipse.viatra.query.runtime.matchers.context.IQueryRuntimeContext;
import org.eclipse.viatra.query.runtime.matchers.context.common.JavaTransitiveInstancesKey;
import org.eclipse.viatra.query.runtime.matchers.planning.QueryProcessingException;
import org.eclipse.viatra.query.runtime.matchers.psystem.PVariable;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicdeferred.TypeFilterConstraint;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.TypeConstraint;
import org.eclipse.viatra.query.runtime.matchers.tuple.TupleMask;

/**
 * Operation compiler implementation that uses EMF-specific operations.
 * 
 * @author Zoltan Ujhelyi
 * @since 1.7
 *
 */
public class EMFOperationCompiler extends AbstractOperationCompiler {

    private boolean baseIndexAvailable;

    private final EMFQueryRuntimeContext runtimeContext;
    
    public EMFOperationCompiler(IQueryRuntimeContext runtimeContext) {
        this(runtimeContext, false);
    }
    
    public EMFOperationCompiler(IQueryRuntimeContext runtimeContext, boolean baseIndexAvailable) {
        super(runtimeContext);
        this.runtimeContext = (EMFQueryRuntimeContext) runtimeContext;
        this.baseIndexAvailable = baseIndexAvailable;
    }

    @Override
    protected void createCheck(TypeFilterConstraint typeConstraint, Map<PVariable, Integer> variableMapping) {
        final IInputKey inputKey = typeConstraint.getInputKey();
        if (inputKey instanceof JavaTransitiveInstancesKey) {
            doCreateInstanceofJavaTypeCheck((JavaTransitiveInstancesKey) inputKey, variableMapping.get(typeConstraint.getVariablesTuple().get(0)));
        } else if (inputKey instanceof EDataTypeInSlotsKey) { // TODO probably only occurs as TypeConstraint
            doCreateInstanceofDatatypeCheck((EDataTypeInSlotsKey) inputKey, variableMapping.get(typeConstraint.getVariablesTuple().get(0)));
        } else if (inputKey instanceof EClassUnscopedTransitiveInstancesKey) {
            doCreateInstanceofUnscopedClassCheck((EClassUnscopedTransitiveInstancesKey) inputKey, variableMapping.get(typeConstraint.getVariablesTuple().get(0)));
        } else {
            String msg = UNSUPPORTED_TYPE_MESSAGE + inputKey;
            throw new QueryProcessingException(msg, null, msg, null);
        }
    }
    
    @Override
    protected void createCheck(TypeConstraint typeConstraint, Map<PVariable, Integer> variableMapping) {
        final IInputKey inputKey = typeConstraint.getSupplierKey();
        if (inputKey instanceof EClassTransitiveInstancesKey) {
            doCreateInstanceofClassCheck((EClassTransitiveInstancesKey)inputKey, variableMapping.get(typeConstraint.getVariablesTuple().get(0)));
        } else if (inputKey instanceof EStructuralFeatureInstancesKey) {
            int sourcePosition = variableMapping.get(typeConstraint.getVariablesTuple().get(0));
            int targetPosition = variableMapping.get(typeConstraint.getVariablesTuple().get(1));
            operations.add(new StructuralFeatureCheck(sourcePosition, targetPosition,
                    ((EStructuralFeatureInstancesKey) inputKey).getEmfKey()));
        } else if (inputKey instanceof EDataTypeInSlotsKey) {
            doCreateInstanceofDatatypeCheck((EDataTypeInSlotsKey) inputKey, variableMapping.get(typeConstraint.getVariablesTuple().get(0)));
        } else {
            String msg = UNSUPPORTED_TYPE_MESSAGE + inputKey;
            throw new QueryProcessingException(msg, null, msg, null);
        }
    }

    @Override
    protected void createUnaryTypeCheck(IInputKey inputKey, int position) {
        if (inputKey instanceof EClassTransitiveInstancesKey) {
            doCreateInstanceofClassCheck((EClassTransitiveInstancesKey)inputKey, position);
        } else if (inputKey instanceof EClassUnscopedTransitiveInstancesKey) {
            doCreateInstanceofUnscopedClassCheck((EClassUnscopedTransitiveInstancesKey)inputKey, position);
        } else if (inputKey instanceof EDataTypeInSlotsKey) {
            doCreateInstanceofDatatypeCheck((EDataTypeInSlotsKey) inputKey, position);
        } else if (inputKey instanceof JavaTransitiveInstancesKey) {
            doCreateInstanceofJavaTypeCheck((JavaTransitiveInstancesKey) inputKey, position);
        } else {
            String msg = UNSUPPORTED_TYPE_MESSAGE + inputKey;
            throw new QueryProcessingException(msg, null, msg, null);
        }
    }

    private void doCreateInstanceofClassCheck(EClassTransitiveInstancesKey inputKey, int position) {
        operations.add(new InstanceOfClassCheck(position, inputKey.getEmfKey()));
        operations.add(new ScopeCheck(position, runtimeContext.getEmfScope()));
    }
    
    private void doCreateInstanceofUnscopedClassCheck(EClassUnscopedTransitiveInstancesKey inputKey, int position) {
        operations.add(new InstanceOfClassCheck(position, inputKey.getEmfKey()));
    }
    
    private void doCreateInstanceofDatatypeCheck(EDataTypeInSlotsKey inputKey, int position) {
        operations.add(new InstanceOfDataTypeCheck(position, inputKey.getEmfKey()));
    }
    private void doCreateInstanceofJavaTypeCheck(JavaTransitiveInstancesKey inputKey, int position) {
        operations.add(new InstanceOfJavaClassCheck(position, inputKey.getInstanceClass()));
    }

    @Override
    public void createExtend(TypeConstraint typeConstraint, Map<PVariable, Integer> variableMapping) {
        final IInputKey inputKey = typeConstraint.getSupplierKey();
        if (inputKey instanceof EDataTypeInSlotsKey) {
            if(baseIndexAvailable){
                operations.add(new IterateOverEDatatypeInstances(variableMapping.get(typeConstraint.getVariableInTuple(0)), ((EDataTypeInSlotsKey) inputKey).getEmfKey()));             
            } else {
                int position = variableMapping.get(typeConstraint.getVariableInTuple(0));
                operations
                        .add(new org.eclipse.viatra.query.runtime.localsearch.operations.extend.nobase.IterateOverEDatatypeInstances(position,
                                ((EDataTypeInSlotsKey) inputKey).getEmfKey(), runtimeContext.getEmfScope()));
                operations.add(new ScopeCheck(position, runtimeContext.getEmfScope()));
            }
        } else if (inputKey instanceof EClassTransitiveInstancesKey) {
            if(baseIndexAvailable){
                operations.add(new IterateOverEClassInstances(variableMapping.get(typeConstraint.getVariableInTuple(0)),
                        ((EClassTransitiveInstancesKey) inputKey).getEmfKey()));
            } else {
                int position = variableMapping.get(typeConstraint.getVariableInTuple(0));
                operations
                        .add(new org.eclipse.viatra.query.runtime.localsearch.operations.extend.nobase.IterateOverEClassInstances(
                                position,
                                ((EClassTransitiveInstancesKey) inputKey).getEmfKey(), runtimeContext.getEmfScope()));
                operations.add(new ScopeCheck(position, runtimeContext.getEmfScope()));
            }
        } else if (inputKey instanceof EStructuralFeatureInstancesKey) {
            final EStructuralFeature feature = ((EStructuralFeatureInstancesKey) inputKey).getEmfKey();
            
            int sourcePosition = variableMapping.get(typeConstraint.getVariablesTuple().get(0));
            int targetPosition = variableMapping.get(typeConstraint.getVariablesTuple().get(1));

            boolean fromBound = variableBindings.get(typeConstraint).contains(sourcePosition);
            boolean toBound = variableBindings.get(typeConstraint).contains(targetPosition);

            if (fromBound && !toBound) {
                operations.add(new ExtendToEStructuralFeatureTarget(sourcePosition, targetPosition, feature));
                operations.add(new ScopeCheck(targetPosition, runtimeContext.getEmfScope()));
            } else if(!fromBound && toBound){
                if (feature instanceof EReference && ((EReference)feature).isContainment()) {
                    // The iterate is also used to traverse a single container (third parameter)
                    operations.add(new IterateOverContainers(sourcePosition, targetPosition, false));
                    operations.add(new ScopeCheck(sourcePosition, runtimeContext.getEmfScope()));
                } else if(baseIndexAvailable){
                    TupleMask mask = TupleMask.fromSelectedIndices(variableMapping.size(), new int[] {targetPosition});
                    operations.add(new ExtendToEStructuralFeatureSource(sourcePosition, targetPosition, feature, mask));                  
                } else {
                    operations.add(new org.eclipse.viatra.query.runtime.localsearch.operations.extend.nobase.ExtendToEStructuralFeatureSource(
                                    sourcePosition, targetPosition, feature));
                    operations.add(new ScopeCheck(sourcePosition, runtimeContext.getEmfScope()));
                }
            } else {
                // TODO Elaborate solution based on the navigability of edges
                // As of now a static solution is implemented
                if (baseIndexAvailable) {
                    operations.add(new IterateOverEClassInstances(sourcePosition, feature.getEContainingClass()));
                    operations.add(new ExtendToEStructuralFeatureTarget(sourcePosition, targetPosition, feature));
                } else {
                    operations
                            .add(new org.eclipse.viatra.query.runtime.localsearch.operations.extend.nobase.IterateOverEClassInstances(
                                    sourcePosition, feature.getEContainingClass(), runtimeContext.getEmfScope()));
                    operations.add(new ScopeCheck(sourcePosition, runtimeContext.getEmfScope()));
                    operations.add(new ExtendToEStructuralFeatureTarget(sourcePosition, targetPosition, feature));
                    operations.add(new ScopeCheck(targetPosition, runtimeContext.getEmfScope()));
                }
            }

        } else {
            throw new IllegalArgumentException(UNSUPPORTED_TYPE_MESSAGE + inputKey);
        }        
    }

}
