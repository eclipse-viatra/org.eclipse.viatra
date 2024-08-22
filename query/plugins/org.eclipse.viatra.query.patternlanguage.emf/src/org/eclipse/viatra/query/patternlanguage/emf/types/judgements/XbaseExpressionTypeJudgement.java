/*******************************************************************************
 * Copyright (c) 2010-2016, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.patternlanguage.emf.types.judgements;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.viatra.query.patternlanguage.emf.helper.PatternLanguageHelper;
import org.eclipse.viatra.query.patternlanguage.emf.types.ITypeSystem;
import org.eclipse.viatra.query.patternlanguage.emf.vql.Expression;
import org.eclipse.viatra.query.patternlanguage.emf.vql.PatternBody;
import org.eclipse.viatra.query.patternlanguage.emf.vql.Variable;
import org.eclipse.viatra.query.runtime.matchers.context.IInputKey;
import org.eclipse.viatra.query.runtime.matchers.context.common.JavaTransitiveInstancesKey;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;
import org.eclipse.xtext.xbase.typesystem.references.ParameterizedTypeReference;
import org.eclipse.xtext.xbase.typesystem.references.UnknownTypeReference;

/**
 * @author Zoltan Ujhelyi
 * @since 1.3
 */
public class XbaseExpressionTypeJudgement extends AbstractTypeJudgement {

    private XExpression xExpression;
    private IBatchTypeResolver xbaseResolver;
    private ITypeSystem typeSystem;
    private boolean unwind;
    
    public XbaseExpressionTypeJudgement(Expression expression, XExpression xExpression,
            IBatchTypeResolver xbaseResolver, ITypeSystem typeSystem, boolean unwind) {
        super(expression);
        this.xExpression = xExpression;
        this.xbaseResolver = xbaseResolver;
        this.typeSystem = typeSystem;
        this.unwind = unwind;
    }

    @Override
    public Set<Expression> getDependingExpressions() {
        final List<Variable> usedVariables = PatternLanguageHelper.getUsedVariables(xExpression, EcoreUtil2
                .getContainerOfType(expression, PatternBody.class).getVariables());
        return new HashSet<>(usedVariables);
    }

    public IInputKey getExpressionType() {
        LightweightTypeReference expressionType = xbaseResolver.resolveTypes(xExpression).getReturnType(xExpression);
        if (expressionType == null) {
             return new JavaTransitiveInstancesKey(Object.class);
        } else if (expressionType instanceof UnknownTypeReference) {
            return new JavaTransitiveInstancesKey(Object.class);
        } else if (this.unwind) {
            return getComponentTypeKey(expressionType);
        } else {
            return asInputKey(expressionType);
        }
    }
    private JavaTransitiveInstancesKey getComponentTypeKey(LightweightTypeReference typeRef) {
        for (LightweightTypeReference parent : typeRef.getAllSuperTypes()) {
            if (parent.getRawTypeReference().isType(Set.class) && parent instanceof ParameterizedTypeReference) {
                final ParameterizedTypeReference parentTypeRef = (ParameterizedTypeReference) parent;
                final List<LightweightTypeReference> typeArguments = parentTypeRef.getTypeArguments();
                if (typeArguments.size() != 1)
                    continue;
                final LightweightTypeReference componentTypeRef = typeArguments.get(0);
                return asInputKey(componentTypeRef);
            }
        }

        return new JavaTransitiveInstancesKey(Object.class);
    }

    private JavaTransitiveInstancesKey asInputKey(LightweightTypeReference typeRef) {
        return typeSystem.fromJvmType(typeRef.getType(), xExpression);
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((xExpression == null) ? 0 : xExpression.hashCode());
        return result+super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        XbaseExpressionTypeJudgement other = (XbaseExpressionTypeJudgement) obj;
        if (xExpression == null) {
            if (other.xExpression != null)
                return false;
        } else if (!xExpression.equals(other.xExpression))
            return false;
        return super.equals(obj);
    }
    
}