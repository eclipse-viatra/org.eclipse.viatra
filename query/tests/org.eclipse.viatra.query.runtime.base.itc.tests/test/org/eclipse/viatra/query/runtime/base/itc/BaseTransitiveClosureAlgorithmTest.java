/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.base.itc;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.viatra.query.runtime.base.itc.graphs.Graph1;
import org.eclipse.viatra.query.runtime.base.itc.graphs.Graph2;
import org.eclipse.viatra.query.runtime.base.itc.graphs.Graph3;
import org.eclipse.viatra.query.runtime.base.itc.graphs.Graph4;
import org.eclipse.viatra.query.runtime.base.itc.graphs.Graph5;
import org.eclipse.viatra.query.runtime.base.itc.graphs.Graph6;
import org.eclipse.viatra.query.runtime.base.itc.graphs.Graph7;
import org.eclipse.viatra.query.runtime.base.itc.graphs.Graph8;
import org.eclipse.viatra.query.runtime.base.itc.graphs.SelfLoopGraph;
import org.junit.runners.Parameterized.Parameters;

public abstract class BaseTransitiveClosureAlgorithmTest {

    @Parameters
    public static Collection<Object[]> getGraphs() {
        return Arrays.asList(new Object[][] {
                         { new SelfLoopGraph()},
                         { new Graph1() },
                         { new Graph2() },
                         { new Graph3() },
                         { new Graph4() },
                         { new Graph5() },
                         { new Graph6() },
                         { new Graph7() },
                         { new Graph8() }
        });
    }
    
}
