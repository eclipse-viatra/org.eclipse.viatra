/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Tamas Szabo - initial API and implementation
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.rete;

import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;
import org.eclipse.viatra.query.runtime.rete.network.communication.ddf.DifferentialTimestamp;

public interface IntervalTree {
        
    public boolean insert(final Tuple tuple, final DifferentialTimestamp timestamp);
    
    public boolean remove(final Tuple tuple, final DifferentialTimestamp timestamp);
    
    public int getCount(final Tuple tuple, final DifferentialTimestamp timestamp);

}
