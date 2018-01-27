/**
 * Copyright (c) 2004-2015, Peter Lunk, Zoltan Ujhelyi and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Peter Lunk - initial API and implementation
 */
package org.eclipse.viatra.integration.mwe2.test.resources;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import org.eclipse.viatra.integration.mwe2.providers.IIterableProvider;
import org.eclipse.viatra.integration.mwe2.providers.impl.BaseProvider;

@SuppressWarnings("all")
public class TestLoopIterable extends BaseProvider implements IIterableProvider {
  @Override
  public Iterable<?> getIterable() {
    ArrayList<String> list = Lists.<String>newArrayList();
    list.add("1");
    list.add("2");
    return list;
  }
}
