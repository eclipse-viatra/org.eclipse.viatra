/*******************************************************************************
 * Copyright (c) 2010-2013, Csaba Debreceni, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.addon.viewers.runtime.util;

import java.util.StringTokenizer;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.viatra.query.runtime.api.IPatternMatch;

public final class LabelParser {

    private LabelParser() {}
    
    public static <Match extends IPatternMatch> String calculateLabel(Match match, String labelExpression) {

        if (labelExpression == null || labelExpression.isEmpty())
            return "";

        StringBuilder sb = new StringBuilder();

        StringTokenizer tokenizer = new StringTokenizer(labelExpression, "$", true);
        if (tokenizer.countTokens() == 0) {
            throw new IllegalArgumentException("Expression must not be empty.");
        }
        boolean inExpression = false;
        boolean foundToken = false;
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.equals("$")) {
                if (inExpression && !foundToken) {
                    throw new IllegalAccessError("Empty reference ($$) in message is not allowed.");
                }
                inExpression = !inExpression;
            } else if (inExpression) {
                sb.append(calculateValue(match, token));
                foundToken = true;
            } else {
                sb.append(token);
            }
        }
        if (inExpression) {
            throw new IllegalArgumentException("Inconsistent model references - a $ character is missing.");
        }

        return sb.toString();
    }

    private static <Match extends IPatternMatch> String calculateValue(Match match, String expression) {
        String[] objectTokens = expression.split("\\.");

        if (objectTokens.length == 1) {
            Object o = match.get(objectTokens[0]);
            return getStringRepresentation(o);
        } else if (objectTokens.length > 1) {
            Object o = match.get(objectTokens[0]);
            // First element has to be EObject
            EObject e = (EObject) o;
            for (int i = 1; i < objectTokens.length; i++) {
                EStructuralFeature feature = e.eClass().getEStructuralFeature(objectTokens[i]);
                o = e.eGet(feature);
                if (o instanceof EObject) {
                    e = (EObject) o;
                } else if (i != objectTokens.length - 1) {
                    return null;
                } else if (o == null)
                    return null;
            }
            return getStringRepresentation(o);
        }
        return expression;
    }

    private static String getStringRepresentation(Object o) {
        if (o instanceof EObject) {
            final EStructuralFeature nameFeature = ((EObject) o).eClass().getEStructuralFeature("name");
            if (nameFeature != null) {
                final Object name = ((EObject) o).eGet(nameFeature);
                return (name == null) ? "" : name.toString();
            }
        }
        return o.toString();
    }

    
}
