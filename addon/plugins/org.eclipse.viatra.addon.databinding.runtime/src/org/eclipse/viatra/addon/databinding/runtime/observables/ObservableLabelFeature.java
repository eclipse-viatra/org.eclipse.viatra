/*******************************************************************************
 * Copyright (c) 2010-2013, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.addon.databinding.runtime.observables;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.viatra.addon.databinding.runtime.api.ViatraObservables;
import org.eclipse.viatra.query.runtime.api.IPatternMatch;
import org.eclipse.viatra.query.runtime.matchers.util.Preconditions;

/**
 * An observable label feature is a computed label, that can refer to the various parameters of the match, and reacts to
 * the corresponding model changes.
 * 
 * @author Zoltan Ujhelyi
 * 
 */
@SuppressWarnings("rawtypes")
public class ObservableLabelFeature extends ComputedValue {
    IPatternMatch match;
    String expression;
    Object container;
    final Map<String, IObservableValue> expressionMap;
    private List<String> expressionTokens;
    

    public ObservableLabelFeature(IPatternMatch match, String expression, Object container) {
        super(String.class);
        Preconditions.checkArgument(expression != null && !expression.isEmpty(), "Expression must be set for label feature");
        this.match = match;
        this.expression = expression;
        this.container = container;
        expressionMap = initializeObservableMap(expression, match);
    }

    private final Map<String, IObservableValue> initializeObservableMap(String expression, IPatternMatch match) {
        Map<String, IObservableValue> map = new HashMap<>();

        StringTokenizer tokenizer = new StringTokenizer(expression, "$", false);
//        Splitter tokenizer = Splitter.on("$");
        boolean inExpression = false;
        boolean foundToken = false;
        while (tokenizer.hasMoreElements()) {
            String token = tokenizer.nextToken();
            if (token == null || token.isEmpty()) {
                if (inExpression && !foundToken) {
                    throw new IllegalArgumentException("Empty reference ($$) in message is not allowed.");
                }
                inExpression = !inExpression;
            } else if (inExpression) {
                if (!map.containsKey(token)) {
                    IObservableValue value = ViatraObservables.getObservableValue(match, token);
                    map.put(token, value);
                }
                foundToken = true;
            }
        }
        if (inExpression) {
            throw new IllegalArgumentException("Inconsistent model references - a $ character is missing.");
        }

        return map;
    }
    
    public Object getContainer() {
        return container;
    }

    public IPatternMatch getMatch() {
        return match;
    }

    public String getExpression() {
        return expression;
    }

    @Override
    protected Object calculate() {

        StringBuilder sb = new StringBuilder();
        try {
            for (int i = 0; i < expressionTokens.size(); i++) {
                String token = expressionTokens.get(i);
                if ((i % 2) == 0) {
                    Preconditions.checkState(expressionMap.containsKey(token), "Error while parsing expression %s", token);
                    IObservableValue value = expressionMap.get(token);
                    sb.append(value.getValue());
                } else {
                    sb.append(token);
                }
            }

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
        return sb.toString();
    }

    @Override
    public synchronized void dispose() {
        for (Entry<String, IObservableValue> entry : expressionMap.entrySet()) {
            entry.getValue().dispose();
        }
        expressionMap.clear();
        super.dispose();
    }
    
    
}