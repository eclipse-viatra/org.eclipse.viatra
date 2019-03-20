/*******************************************************************************
 * Copyright (c) 2010-2014, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.matchers.psystem.rewriters;

import org.eclipse.viatra.query.runtime.matchers.psystem.queries.QueryInitializationException;

/**
 * An exception to wrap various issues during PDisjunction rewriting.
 * @author Zoltan Ujhelyi
 *
 */
public class RewriterException extends QueryInitializationException {

    private static final long serialVersionUID = -4703825954995497932L;

    public RewriterException(String message, String[] context, String shortMessage, Object patternDescription,
            Throwable cause) {
        super(message, context, shortMessage, patternDescription, cause);
    }

    public RewriterException(String message, String[] context, String shortMessage, Object patternDescription) {
        super(message, context, shortMessage, patternDescription);
    }

}
