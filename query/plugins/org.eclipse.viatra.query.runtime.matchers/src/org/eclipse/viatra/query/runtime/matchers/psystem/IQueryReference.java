/*******************************************************************************
 * Copyright (c) 2010-2014, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Zoltan Ujhelyi - initial API and implementation
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.matchers.psystem;

import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PQuery;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;


/**
 * @author Zoltan Ujhelyi
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IQueryReference {

    PQuery getReferredQuery();
    
    /**
     * @return the tuple of variables given as actual parameters
     * @since 2.1
     */
    Tuple getActualParametersTuple();
}
