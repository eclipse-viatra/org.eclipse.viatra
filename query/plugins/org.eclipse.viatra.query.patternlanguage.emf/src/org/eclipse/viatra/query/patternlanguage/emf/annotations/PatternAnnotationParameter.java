/*******************************************************************************
 * Copyright (c) 2010-2012, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.patternlanguage.emf.annotations;

/**
 * @since 2.0
 */
public class PatternAnnotationParameter {
    public static final String STRING = "string";
    public static final String VARIABLEREFERENCE = "variablereference";
    public static final String LIST = "list";
    public static final String BOOLEAN = "boolean";
    public static final String DOUBLE = "double";
    public static final String INT = "int";
    private String name;
    private String type;
    private boolean multiple;
    private boolean mandatory;
    private String description;
    private boolean deprecated;

    public PatternAnnotationParameter(String name, String type, String description, boolean multiple,
            boolean mandatory) {
        this(name, type, description, multiple, mandatory, false);
    }
    public PatternAnnotationParameter(String name, String type, String description, boolean multiple,
            boolean mandatory, boolean deprecated) {
        super();
        this.name = name;
        this.type = type;
        this.description = description;
        this.multiple = multiple;
        this.mandatory = mandatory;
        this.deprecated = deprecated;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public boolean isDeprecated() {
        return deprecated;
    }
}