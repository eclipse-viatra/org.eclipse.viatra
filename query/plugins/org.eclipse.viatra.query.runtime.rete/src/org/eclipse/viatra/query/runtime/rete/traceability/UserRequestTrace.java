/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.rete.traceability;

import java.util.Collection;

import org.eclipse.viatra.query.runtime.rete.recipes.ReteNodeRecipe;

//	private class AggregatorReferenceIndexTraceInfo extends RecipeTraceInfo {
//		RecipeTraceInfo aggregatorNodeRecipeTrace;		
//		public AggregatorReferenceIndexTraceInfo(ProjectionIndexerRecipe recipe,
//				RecipeTraceInfo parentRecipeTrace,
//				RecipeTraceInfo aggregatorNodeRecipeTrace) {
//			super(recipe, parentRecipeTrace);
//			this.aggregatorNodeRecipeTrace = aggregatorNodeRecipeTrace;
//		}
//		public RecipeTraceInfo getAggregatorNodeRecipeTrace() {
//			return aggregatorNodeRecipeTrace;
//		}
//	}
public  class UserRequestTrace extends RecipeTraceInfo {
    public UserRequestTrace(ReteNodeRecipe recipe,
            Collection<RecipeTraceInfo> parentRecipeTraces) {
        super(recipe, parentRecipeTraces);
    }
    public UserRequestTrace(ReteNodeRecipe recipe,
            RecipeTraceInfo... parentRecipeTraces) {
        super(recipe, parentRecipeTraces);
    }
}