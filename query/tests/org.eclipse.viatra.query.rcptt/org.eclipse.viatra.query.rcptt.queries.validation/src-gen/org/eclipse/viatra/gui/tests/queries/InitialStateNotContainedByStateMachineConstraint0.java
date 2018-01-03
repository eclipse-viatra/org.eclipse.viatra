/**

  Copyright (c) 2014-2016, IncQuery Labs Ltd.
  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
  
  Contributors:
    Akos Horvath, Abel Hegedus, Tamas Borbas, Zoltan Ujhelyi - initial API and implementation
*/
package org.eclipse.viatra.gui.tests.queries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;

import org.eclipse.viatra.addon.validation.core.api.Severity;
import org.eclipse.viatra.addon.validation.core.api.IConstraintSpecification;
import org.eclipse.viatra.query.runtime.api.IPatternMatch;
import org.eclipse.viatra.query.runtime.api.IQuerySpecification;
import org.eclipse.viatra.query.runtime.api.ViatraQueryMatcher;

import org.eclipse.viatra.gui.tests.queries.util.InitialStateNotContainedByStateMachineQuerySpecification;

public class InitialStateNotContainedByStateMachineConstraint0 implements IConstraintSpecification {

    private InitialStateNotContainedByStateMachineQuerySpecification querySpecification;

    public InitialStateNotContainedByStateMachineConstraint0() {
        querySpecification = InitialStateNotContainedByStateMachineQuerySpecification.instance();
    }

    @Override
    public String getMessageFormat() {
        return "The initial state $state.identifier$ of $statemachine.identifier$ is not included in its states";
    }


    @Override
    public Map<String,Object> getKeyObjects(IPatternMatch signature) {
        Map<String,Object> map = new HashMap<>();
        map.put("statemachine",signature.get("statemachine"));
        return map;
    }

    @Override
    public List<String> getKeyNames() {
        List<String> keyNames = Arrays.asList(
            "statemachine"
        );
        return keyNames;
    }

    @Override
    public List<String> getPropertyNames() {
        List<String> propertyNames = Arrays.asList(
            "state"
        );
        return propertyNames;
    }

    @Override
    public Set<List<String>> getSymmetricPropertyNames() {
        Set<List<String>> symmetricPropertyNamesSet = new HashSet<>(
        );
        return symmetricPropertyNamesSet;
    }

    @Override
    public Set<List<String>> getSymmetricKeyNames() {
        Set<List<String>> symmetricKeyNamesSet = new HashSet<>(
        );
        return symmetricKeyNamesSet;
    }

    @Override
    public Severity getSeverity() {
        return Severity.ERROR;
    }

    @Override
    public IQuerySpecification<? extends ViatraQueryMatcher<? extends IPatternMatch>> getQuerySpecification() {
        return querySpecification;
    }

}
