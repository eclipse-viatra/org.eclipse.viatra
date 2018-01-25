/*******************************************************************************
 * Copyright (c) 2010-2012, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Zoltan Ujhelyi - initial API and implementation
 *******************************************************************************/
package org.eclipse.viatra.query.patternlanguage.emf.formatting;

import org.eclipse.viatra.query.patternlanguage.emf.services.EMFPatternLanguageGrammarAccess;
import org.eclipse.viatra.query.patternlanguage.emf.services.EMFPatternLanguageGrammarAccess.PatternModelElements;
import org.eclipse.viatra.query.patternlanguage.emf.services.EMFPatternLanguageGrammarAccess.AnnotationElements;
import org.eclipse.viatra.query.patternlanguage.emf.services.EMFPatternLanguageGrammarAccess.PatternElements;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.formatting.impl.AbstractDeclarativeFormatter;
import org.eclipse.xtext.formatting.impl.FormattingConfig;

/**
 * Formatting rules for the EMF pattern language.
 */
public class EMFPatternLanguageFormatter extends AbstractDeclarativeFormatter {

    @Override
    protected void configureFormatting(FormattingConfig c) {
        EMFPatternLanguageGrammarAccess grammar = (EMFPatternLanguageGrammarAccess) getGrammarAccess();

        PatternModelElements patternModelAccess = grammar.getPatternModelAccess();
        c.setLinewrap(2).after(patternModelAccess.getPackageNameAssignment_0_1());
        c.setLinewrap(1).after(patternModelAccess.getImportPackagesAssignment_1());
        c.setLinewrap().before(patternModelAccess.getPatternsAssignment_2());
        c.setLinewrap(2).before(patternModelAccess.getPatternsAssignment_2());
        c.setLinewrap(2).between(patternModelAccess.getPatternsAssignment_2(),
                patternModelAccess.getPatternsAssignment_2());
        
        PatternElements patternAccess = grammar.getPatternAccess();
        c.setLinewrap(1).after(patternAccess.getAnnotationsAssignment_0());
        c.setSpace(" ").around(patternAccess.getOrKeyword_9_0());

        AnnotationElements annotationAccess = grammar.getAnnotationAccess();
        c.setLinewrap().after(annotationAccess.getRule());

        // Preserve newlines around comments
        c.setLinewrap(0, 1, 2).before(grammar.getSL_COMMENTRule());
        c.setLinewrap(0, 1, 2).before(grammar.getML_COMMENTRule());
        c.setLinewrap(0, 1, 1).after(grammar.getML_COMMENTRule());
        
        for (Keyword keyword : grammar.findKeywords("=")) {
            c.setSpace(" ").around(keyword);
        }
        for (Keyword keyword : grammar.findKeywords(".")) {
            c.setNoSpace().before(keyword);
            c.setNoSpace().after(keyword);
        }
        for (Keyword keyword : grammar.findKeywords(":")) {
            c.setSpace(" ").before(keyword);
            c.setSpace(" ").after(keyword);
        }
        for (Keyword keyword : grammar.findKeywords("::")) {
            c.setNoSpace().before(keyword);
            c.setNoSpace().after(keyword);
        }
        for (Keyword keyword : grammar.findKeywords(",")) {
            c.setNoSpace().before(keyword);
            c.setSpace(" ").after(keyword);
        }
        for (Keyword keyword : grammar.findKeywords("(")) {
            c.setNoSpace().before(keyword);
            c.setNoSpace().after(keyword);
        }
        for (Keyword keyword : grammar.findKeywords(";")) {
            c.setNoSpace().before(keyword);
            c.setLinewrap(1, 1, 2).after(keyword);
        }

        for (Keyword keyword : grammar.findKeywords(")")) {
            c.setNoSpace().before(keyword);
            c.setLinewrap(1).after(keyword);
        }
        for (Keyword keyword : grammar.findKeywords("@")) {
            c.setNoSpace().after(keyword);
        }

        for (Keyword keyword : grammar.findKeywords("{")) {
            c.setSpace(" ").before(keyword);
            c.setLinewrap(1, 1, 2).after(keyword);
            c.setIndentationIncrement().after(keyword);
        }
        for (Keyword keyword : grammar.findKeywords("}")) {
            c.setLinewrap(2).after(keyword);
            c.setIndentationDecrement().before(keyword);
        }
        for (Keyword keyword : grammar.findKeywords("import")) {
            c.setLinewrap(1).before(keyword);
        }
    }
}
