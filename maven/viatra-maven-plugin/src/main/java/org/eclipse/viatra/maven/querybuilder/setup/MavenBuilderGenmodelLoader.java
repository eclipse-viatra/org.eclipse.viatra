/*******************************************************************************
 * Copyright (c) 2010-2014, Jozsef Makai, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.viatra.maven.querybuilder.setup;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.viatra.query.patternlanguage.emf.IGenmodelMappingLoader;

public class MavenBuilderGenmodelLoader implements IGenmodelMappingLoader {

    protected static final MavenBuilderGenmodelLoader genmodelLoader = new MavenBuilderGenmodelLoader();

    private Map<String, String> genModels;

    protected MavenBuilderGenmodelLoader() {
        genModels = new HashMap<>();
    }

    public static void addGenmodel(String modelNsUri, String genmodelUri) {
        genmodelLoader.putGenmodel(modelNsUri, genmodelUri);
    }

    public Map<String, String> loadGenmodels() {
        return genModels;
    }

    protected void putGenmodel(String modelNsUri, String genmodelUri) {
        genModels.put(modelNsUri, genmodelUri);
    }

    public static MavenBuilderGenmodelLoader getInstance() {
        return genmodelLoader;
    }

}
