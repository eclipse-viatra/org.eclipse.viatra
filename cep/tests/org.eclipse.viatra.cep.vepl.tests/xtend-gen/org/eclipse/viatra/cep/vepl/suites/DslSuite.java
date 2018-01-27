/**
 * Copyright (c) 2004-2014, Istvan David, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Istvan David - initial API and implementation
 */
package org.eclipse.viatra.cep.vepl.suites;

import org.eclipse.viatra.cep.vepl.tests.atomic.AtomicTests;
import org.eclipse.viatra.cep.vepl.tests.complex.ComplexTests;
import org.eclipse.viatra.cep.vepl.tests.complex.InfinityTests;
import org.eclipse.viatra.cep.vepl.tests.validation.ValidationTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ AtomicTests.class, ComplexTests.class, ValidationTests.class, InfinityTests.class })
@SuppressWarnings("all")
public class DslSuite {
}
