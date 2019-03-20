/*******************************************************************************
 * Copyright (c) 2010-2014, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.patternlanguage.emf.ui.contentassist;

import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.swt.graphics.Image;
import org.eclipse.viatra.query.patternlanguage.emf.services.EMFPatternLanguageGrammarAccess;
import org.eclipse.xtext.ui.editor.contentassist.ContentAssistContext;
import org.eclipse.xtext.ui.editor.templates.ContextTypeIdHelper;
import org.eclipse.xtext.ui.editor.templates.DefaultTemplateProposalProvider;

import com.google.inject.Inject;

/**
 * Override
 * @author Zoltan Ujhelyi
 * 
 */
public class EMFPatternLanguageTemplateProposalProvider extends DefaultTemplateProposalProvider {

    @Inject
    private EMFPatternLanguageGrammarAccess ga;

    @Inject
    public EMFPatternLanguageTemplateProposalProvider(TemplateStore templateStore, ContextTypeRegistry registry,
            ContextTypeIdHelper helper) {
        super(templateStore, registry, helper);
    }

    @Override
    protected TemplateProposal createProposal(Template template, TemplateContext templateContext,
            ContentAssistContext context, Image image, int relevance) {

        // suppress content assist in comments
        if (context.getCurrentNode().getGrammarElement() == ga.getML_COMMENTRule()
                || context.getCurrentNode().getGrammarElement() == ga.getSL_COMMENTRule()) {
            return null;
        }
        return super.createProposal(template, templateContext, context, image, relevance);
    }

}
