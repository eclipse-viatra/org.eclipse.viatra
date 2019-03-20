/*******************************************************************************
 * Copyright (c) 2010-2012, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.viatra.query.patternlanguage.emf.ui.highlight;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.viatra.query.patternlanguage.emf.ide.highlight.EMFPatternLanguageHighlightingStyles;
import org.eclipse.xtext.ui.editor.syntaxcoloring.IHighlightingConfigurationAcceptor;
import org.eclipse.xtext.ui.editor.utils.TextStyle;
import org.eclipse.xtext.xbase.ui.highlighting.XbaseHighlightingConfiguration;

@SuppressWarnings("restriction")
public class EMFPatternLanguageHighlightingConfiguration extends XbaseHighlightingConfiguration {

    public static final String METAMODEL_REFERENCE = EMFPatternLanguageHighlightingStyles.METAMODEL_REFERENCE;

    @Override
    public void configure(IHighlightingConfigurationAcceptor acceptor) {
        acceptor.acceptDefaultHighlighting(METAMODEL_REFERENCE, "EMF type reference", metamodelReference());

        super.configure(acceptor);
    }

    public TextStyle metamodelReference() {
        TextStyle textStyle = defaultTextStyle().copy();
        textStyle.setStyle(SWT.BOLD);
        textStyle.setColor(new RGB(0, 26, 171));
        return textStyle;
    }
}
