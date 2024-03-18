/*******************************************************************************
 * Copyright (c) 2010-2017, Grill Balázs, IncQueryLabs
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.matchers.psystem.rewriters;

import org.eclipse.viatra.query.runtime.matchers.backend.CommonQueryHintOptions;
import org.eclipse.viatra.query.runtime.matchers.planning.SubPlan;
import org.eclipse.viatra.query.runtime.matchers.psystem.PBody;

/**
 * An implementation of this interface can provide an execution plan for a {@link PBody}.
 * The plan provider can be registered via a query hint (see {@link CommonQueryHintOptions#externalQueryPlanProvider}), 
 * and it will be used by the Rete recipe compiler. 
 * 
 * @since 2.8
 */
public interface ExternalQueryPlanProvider {

    public SubPlan getPlan(PBody body);

}
