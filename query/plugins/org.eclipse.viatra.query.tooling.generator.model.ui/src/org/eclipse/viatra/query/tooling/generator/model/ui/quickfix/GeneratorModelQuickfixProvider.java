/*******************************************************************************
 * Copyright (c) 2010-2012, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.tooling.generator.model.ui.quickfix;

import org.eclipse.xtext.ui.editor.quickfix.DefaultQuickfixProvider;

public class GeneratorModelQuickfixProvider extends DefaultQuickfixProvider {

    // @Fix(MyJavaValidator.INVALID_NAME)
    // public void capitalizeName(final Issue issue, IssueResolutionAcceptor acceptor) {
    // acceptor.accept(issue, "Capitalize name", "Capitalize the name.", "upcase.png", new IModification() {
    // public void apply(IModificationContext context) throws BadLocationException {
    // IXtextDocument xtextDocument = context.getXtextDocument();
    // String firstLetter = xtextDocument.get(issue.getOffset(), 1);
    // xtextDocument.replace(issue.getOffset(), 1, firstLetter.toUpperCase());
    // }
    // });
    // }

}
