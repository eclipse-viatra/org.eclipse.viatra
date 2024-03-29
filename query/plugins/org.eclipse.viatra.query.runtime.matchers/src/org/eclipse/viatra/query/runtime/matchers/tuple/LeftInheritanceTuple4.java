/*******************************************************************************
 * Copyright (c) 2010-2017, Gabor Bergmann, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.matchers.tuple;

import java.util.Objects;

/**
 * @author Gabor Bergmann
 * @since 1.7
 */
public final class LeftInheritanceTuple4 extends BaseLeftInheritanceTuple {
    private final Object localElement0;
    private final Object localElement1;
    private final Object localElement2;
    private final Object localElement3;

    protected LeftInheritanceTuple4(Tuple ancestor, Object localElement0, Object localElement1, Object localElement2,
            Object localElement3) {
        super(ancestor);
        this.localElement0 = localElement0;
        this.localElement1 = localElement1;
        this.localElement2 = localElement2;
        this.localElement3 = localElement3;
        calcHash();
    }

    @Override
    public int getLocalSize() {
        return 4;
    }
    
    @Override
    public int getSize() {
        return inheritedIndex + 4;
    }

    @Override
    public Object get(int index) {
        int local = index - inheritedIndex;
        if (local < 0) 
            return ancestor.get(index);
        else if (local == 0) return localElement0;
        else if (local == 1) return localElement1;
        else if (local == 2) return localElement2;
        else if (local == 3) return localElement3;
        else throw raiseIndexingError(index);
    }

    /**
     * Optimized hash calculation
     */
    @Override
    void calcHash() {
        final int PRIME = 31;
        cachedHash = ancestor.hashCode();
        cachedHash = PRIME * cachedHash;
        if (localElement0 != null) cachedHash += localElement0.hashCode();
        cachedHash = PRIME * cachedHash;
        if (localElement1 != null) cachedHash += localElement1.hashCode();
        cachedHash = PRIME * cachedHash;
        if (localElement2 != null) cachedHash += localElement2.hashCode();
        cachedHash = PRIME * cachedHash;
        if (localElement3 != null) cachedHash += localElement3.hashCode();
    }

    @Override
    protected boolean localEquals(BaseLeftInheritanceTuple other) {
        if (other instanceof LeftInheritanceTuple4) {
            LeftInheritanceTuple4 lit = (LeftInheritanceTuple4)other;
            return Objects.equals(this.localElement0, lit.localElement0) &&
                    Objects.equals(this.localElement1, lit.localElement1) &&
                    Objects.equals(this.localElement2, lit.localElement2) &&
                    Objects.equals(this.localElement3, lit.localElement3);
        } else {
            return (4 == other.getLocalSize()) && 
                    Objects.equals(localElement0, other.get(inheritedIndex)) && 
                    Objects.equals(localElement1, other.get(inheritedIndex + 1)) && 
                    Objects.equals(localElement2, other.get(inheritedIndex + 2)) && 
                    Objects.equals(localElement3, other.get(inheritedIndex + 3));
        }
    }
}
