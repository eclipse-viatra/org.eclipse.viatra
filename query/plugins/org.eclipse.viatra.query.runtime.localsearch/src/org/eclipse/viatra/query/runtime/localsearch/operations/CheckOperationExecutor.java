/*******************************************************************************
 * Copyright (c) 2010-2013, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.localsearch.operations;

import org.eclipse.viatra.query.runtime.localsearch.MatchingFrame;
import org.eclipse.viatra.query.runtime.localsearch.matcher.ISearchContext;
import org.eclipse.viatra.query.runtime.localsearch.operations.ISearchOperation.ISearchOperationExecutor;

/**
 * Abstract base class for search operations that check only the already set variables.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.0
 */
public abstract class CheckOperationExecutor implements ISearchOperationExecutor {

    /**
     * The executed field ensures that the second call of the check always returns false, resulting in a quick
     * backtracking.
     */
    private boolean executed;

    @Override
    public void onInitialize(MatchingFrame frame, ISearchContext context) {
        executed = false;
    }

    @Override
    public void onBacktrack(MatchingFrame frame, ISearchContext context) {
    }

    @Override
    public boolean execute(MatchingFrame frame, ISearchContext context) {
        executed = executed ? false : check(frame, context);
        return executed;
    }

    /**
     * Executes the checking operation
     * @since 1.7
     */
    protected abstract boolean check(MatchingFrame frame, ISearchContext context) ;

}
