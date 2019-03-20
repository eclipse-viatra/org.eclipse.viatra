/*******************************************************************************
 * Copyright (c) 2010-2016, Andras Szabolcs Nagy and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.dse.evolutionary.mutationrate;

import java.util.Collection;

import org.eclipse.viatra.dse.evolutionary.interfaces.IMutationRate;
import org.eclipse.viatra.dse.objectives.TrajectoryFitness;

public class AdaptiveMutationRate implements IMutationRate {

    private double baseMutationChance;
    private double adaptiveMutation;

    public AdaptiveMutationRate() {
        this(0.5d, 0.33d);
    }

    public AdaptiveMutationRate(double baseMutationChance) {
        this(baseMutationChance, 0.33d);
    }

    public AdaptiveMutationRate(double baseMutationChance, double adaptiveMutation) {
        this.baseMutationChance = baseMutationChance;
        this.adaptiveMutation = adaptiveMutation;
    }

    @Override
    public double getMutationChance(Collection<TrajectoryFitness> currentPopulation,
            Collection<TrajectoryFitness> survivedPopulation, Collection<TrajectoryFitness> parentPopulation) {
        int paretoFrontSize = 0;
        for (TrajectoryFitness trajectoryFitness : parentPopulation) {
            if (trajectoryFitness.rank == 1) {
                paretoFrontSize++;
            }
        }
        return baseMutationChance + adaptiveMutation * paretoFrontSize / parentPopulation.size();
    }

}
