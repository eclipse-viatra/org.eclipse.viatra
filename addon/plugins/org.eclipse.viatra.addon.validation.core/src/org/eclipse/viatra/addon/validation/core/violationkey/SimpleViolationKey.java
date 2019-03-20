/*******************************************************************************
 * Copyright (c) 2010-2014, Balint Lorand, Zoltan Ujhelyi, Abel Hegedus, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.viatra.addon.validation.core.violationkey;

/**
 * A simple violation key serves as an object to uniquely identify an Object.
 * 
 * @author Balint Lorand
 *
 */
public class SimpleViolationKey implements ViolationKey {

    private Object keyObject;

    public Object getKeyObject() {
        return keyObject;
    }

    public void setKeyObject(Object keyObject) {
        this.keyObject = keyObject;
    }

    public SimpleViolationKey(Object keyObject) {
        super();
        this.keyObject = keyObject;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((keyObject == null) ? 0 : keyObject.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SimpleViolationKey other = (SimpleViolationKey) obj;
        if (keyObject == null) {
            if (other.keyObject != null)
                return false;
        } else if (!keyObject.equals(other.keyObject))
            return false;
        return true;
    }
}
