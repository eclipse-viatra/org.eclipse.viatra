/**
 * Copyright (c) 2010-2016, Peter Lunk, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.viatra.transformation.debug.transformationtrace.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ActivationTrace implements Serializable {
    private static final long serialVersionUID = 4482651679137706911L;
    private final String ruleName;
    private List<RuleParameterTrace> ruleParameterTraces = new ArrayList<>();

    public ActivationTrace(String ruleName) {
        super();
        this.ruleName = ruleName;
    }

    public String getRuleName() {
        return ruleName;
    }

    public List<RuleParameterTrace> getRuleParameterTraces() {
        return ruleParameterTraces;
    }
}
