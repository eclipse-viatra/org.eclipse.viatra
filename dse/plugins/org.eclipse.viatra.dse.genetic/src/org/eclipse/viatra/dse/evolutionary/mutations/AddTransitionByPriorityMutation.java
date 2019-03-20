/*******************************************************************************
 * Copyright (c) 2010-2016, Andras Szabolcs Nagy and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.dse.evolutionary.mutations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.eclipse.viatra.dse.base.DesignSpaceManager;
import org.eclipse.viatra.dse.base.ThreadContext;
import org.eclipse.viatra.dse.evolutionary.interfaces.IMutation;
import org.eclipse.viatra.dse.objectives.TrajectoryFitness;
import org.eclipse.viatra.transformation.runtime.emf.rules.batch.BatchTransformationRule;

public class AddTransitionByPriorityMutation implements IMutation {

    private Random rnd = new Random();
    public Map<BatchTransformationRule<?, ?>, Integer> priorities;

    public AddTransitionByPriorityMutation(Map<BatchTransformationRule<?, ?>, Integer> priorities) {
        this.priorities = priorities;
    }

    @Override
    public boolean mutate(TrajectoryFitness parent, ThreadContext context) {

        DesignSpaceManager dsm = context.getDesignSpaceManager();

        context.executeTrajectoryWithMinimalBacktrackWithoutStateCoding(parent.trajectory);

        Collection<Object> transitions = dsm.getTransitionsFromCurrentState();
        int size = transitions.size();
        if (size == 0) {
            return false;
        }

        int bestPriority = Integer.MIN_VALUE;
        for (Object iTransition : transitions) {
            int priority = priorities.get(dsm.getRuleByActivationId(iTransition)).intValue();
            if (priority > bestPriority) {
                bestPriority = priority;
            }
        }
        List<Object> bestTrasitions = new ArrayList<>();
        for (Object iTransition : transitions) {
            if (priorities.get(dsm.getRuleByActivationId(iTransition)).intValue() == bestPriority) {
                bestTrasitions.add(iTransition);
            }
        }
        int index = rnd.nextInt(bestTrasitions.size());
        Object transition = bestTrasitions.get(index);

        dsm.fireActivation(transition);

        return true;
    }

    @Override
    public IMutation createNew() {
        return new AddTransitionByPriorityMutation(priorities);
    }
}
