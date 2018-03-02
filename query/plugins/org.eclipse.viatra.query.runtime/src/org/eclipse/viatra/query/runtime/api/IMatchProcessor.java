/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Gabor Bergmann - initial API and implementation
 *******************************************************************************/

package org.eclipse.viatra.query.runtime.api;

import java.util.function.Consumer;

/**
 * A "lambda" action that can be executed on each match of a pattern.
 * 
 * <p>Clients can manually implement process() in an (anonymous) class, or alternatively, extend either
 * {@link GenericMatchProcessor} or the user-friendly pattern-specific generated match processor classes.
 * 
 * @author Bergmann Gábor
 * @deprecated Starting with VIATRA 2.0, a {@link Consumer} instance provides an appropriate alternative for this class.
 */
@Deprecated
public interface IMatchProcessor<Match extends IPatternMatch> extends Consumer<Match> {
    /**
     * Defines the action that is to be executed on each match.
     * 
     * @param match
     *            a single match of the pattern that must be processed by the implementation of this method
     */
    public void process(Match match);

    /**
     * @since 2.0
     */
    @Override
    default void accept(Match t) {
        process(t);
    }
}
