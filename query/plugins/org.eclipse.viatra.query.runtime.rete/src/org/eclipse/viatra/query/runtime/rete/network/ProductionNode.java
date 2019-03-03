/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Gabor Bergmann - initial API and implementation
 *******************************************************************************/

package org.eclipse.viatra.query.runtime.rete.network;

import java.util.Map;

import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;

/**
 * Interface intended for nodes containing complete matches.
 *
 * @author Gabor Bergmann
 */
public interface ProductionNode extends Tunnel, Iterable<Tuple> {

    /**
     * @return the position mapping of this particular pattern that maps members of the tuple type to their positions
     */
    Map<String, Integer> getPosMapping();

}