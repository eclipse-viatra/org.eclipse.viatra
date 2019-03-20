/*******************************************************************************
 * Copyright (c) 2010-2012, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.viatra.query.patternlanguage.emf.util;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.diagnostics.Severity;

public interface IErrorFeedback {

    /**
     * An error type for use in the JvmModelInferrer. It is differentiated from {@link #FRAGMENT_ERROR_TYPE}, as the two
     * builds have different lifecycles, so cleaning has to be executed at different points.
     */
    final String JVMINFERENCE_ERROR_TYPE = "org.eclipse.viatra.query.tooling.core.generator.marker.inference";
    /**
     * An error type for use in the generator fragments. It is differentiated from {@link #JVMINFERENCE_ERROR_TYPE}, as
     * the two builds have different lifecycles, so cleaning has to be executed at different points.
     */
    final String FRAGMENT_ERROR_TYPE = "org.eclipse.viatra.query.tooling.core.generator.marker.fragment";

    /**
     * Clears all problem markers from the resource and all its descendants.
     *
     * @param resource
     *            a file, folder or project to clean all markers from
     * @param markerType
     *            {@link #JVMINFERENCE_ERROR_TYPE} and {@link #FRAGMENT_ERROR_TYPE} are supported
     */
    void clearMarkers(Resource resource, String markerType);

    /**
     * Reports an error in a context object. The error marker only appears if the context object is contained in a
     * workspace resource, and then it is associated with the location of the context object in the textual file. All
     * runtime errors related to the creation of the marker are logged.
     *
     * @param ctx
     * @param message
     * @param errorCode
     *            an arbitrary error code
     * @param severity
     * @param markerType
     *            {@link #JVMINFERENCE_ERROR_TYPE} and {@link #FRAGMENT_ERROR_TYPE} are supported
     */
    void reportError(EObject ctx, String message, String errorCode, Severity severity, String markerType);

    /**
     * Reports an error in a context object. The error marker only appears if the context object is contained in a
     * workspace resource, but it is <b>NOT</b> associated with the location of the context object in the textual file.
     * All runtime errors related to the creation of the marker are logged.
     *
     * @param ctx
     * @param message
     * @param errorCode
     *            an arbitrary error code
     * @param severity
     * @param markerType
     *            {@link #JVMINFERENCE_ERROR_TYPE} and {@link #FRAGMENT_ERROR_TYPE} are supported
     */
    void reportErrorNoLocation(EObject ctx, String message, String errorCode, Severity severity, String markerType);

    /**
     * Reports an error in a file, but is not associated to any specific line. All runtime errors related to the
     * creation of the marker are logged.
     *
     * @param resource
     * @param message
     * @param errorCode
     *            an arbitrary error code
     * @param severity
     * @param markerType
     *            {@link #JVMINFERENCE_ERROR_TYPE} and {@link #FRAGMENT_ERROR_TYPE} are supported
     */
    void reportError(Resource resource, String message, String errorCode, Severity severity, String markerType);

}