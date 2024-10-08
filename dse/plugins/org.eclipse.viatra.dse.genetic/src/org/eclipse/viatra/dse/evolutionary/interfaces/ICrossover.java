/*******************************************************************************
 * Copyright (c) 2010-2016, Andras Szabolcs Nagy and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.dse.evolutionary.interfaces;

import org.eclipse.viatra.dse.base.ThreadContext;
import org.eclipse.viatra.dse.objectives.TrajectoryFitness;

public interface ICrossover {

    boolean mutate(TrajectoryFitness parent1, TrajectoryFitness parent2, ThreadContext context);

    boolean mutateAlternate(TrajectoryFitness parent1, TrajectoryFitness parent2, ThreadContext context);

    ICrossover createNew();

}
