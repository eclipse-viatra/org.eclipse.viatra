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

public class RuleParameterTrace implements Serializable{
    private static final long serialVersionUID = -4720249391693557182L;
    private final String parameterName;
    private final String objectId;
    
    public RuleParameterTrace(String parameterName, String objectId) {
        super();
        this.parameterName = parameterName;
        this.objectId = objectId;
    }

    public String getParameterName() {
        return parameterName;
    }

    public String getObjectId() {
        return objectId;
    }
    
    
}
