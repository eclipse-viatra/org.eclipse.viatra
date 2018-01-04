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
package org.eclipse.viatra.query.runtime.localsearch.plan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.viatra.query.runtime.localsearch.operations.ISearchOperation;

/**
 * A SearchPlan stores a collection of SearchPlanOperations for a fixed order of variables.
 * 
 * @author Zoltan Ujhelyi
 * 
 */
public class SearchPlan {

    private List<ISearchOperation> operations = new ArrayList<>();

    public void addOperation(ISearchOperation operation) {
        operations.add(operation);
    }

    public void addOperations(ISearchOperation[] newOperations) {
        operations.addAll(Arrays.asList(newOperations));
    }

    public void addOperations(List<ISearchOperation> newOperations) {
        operations.addAll(newOperations);
    }

    /**
     * Returns an immutable list of operations stored in the plan.
     * @return the operations
     */
    public List<ISearchOperation> getOperations() {
        return Collections.unmodifiableList(new ArrayList<>(operations));
    }

}
