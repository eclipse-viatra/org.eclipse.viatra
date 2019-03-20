/*******************************************************************************
 * Copyright (c) 2010-2015, Marton Bur, Zoltan Ujhelyi, Akos Horvath, Istvan Rath and Danil Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.localsearch.planner.util;

import java.util.Comparator;

import org.eclipse.viatra.query.runtime.localsearch.planner.PConstraintInfo;

/**
 * @author Marton Bur
 *
 */
public class OperationCostComparator implements Comparator<PConstraintInfo>{

    @Override
    public int compare(PConstraintInfo o1, PConstraintInfo o2) {
        return Double.compare(o1.getCost(), o2.getCost());
    }

}
