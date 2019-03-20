/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.internal.apiimpl;

import org.eclipse.viatra.query.runtime.api.ViatraQueryEngine;
import org.eclipse.viatra.query.runtime.matchers.backend.IMatcherCapability;
import org.eclipse.viatra.query.runtime.matchers.backend.IQueryResultProvider;

/**
 * Internal class for wrapping a query result providing backend. It's only supported usage is by the
 * {@link ViatraQueryEngineImpl} class.
 * </p>
 * 
 * <strong>Important note</strong>: this class must not introduce any public method, as it will be visible through
 * BaseMatcher as an API, although this class is not an API itself.
 * 
 * @author Bergmann Gabor
 *
 */
public abstract class QueryResultWrapper {

    protected IQueryResultProvider backend;

    protected abstract void setBackend(ViatraQueryEngine engine, IQueryResultProvider resultProvider, IMatcherCapability capabilities);
    
}
