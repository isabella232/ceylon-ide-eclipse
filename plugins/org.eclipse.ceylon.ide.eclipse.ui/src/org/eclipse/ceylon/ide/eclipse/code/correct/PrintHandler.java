/********************************************************************************
 * Copyright (c) 2011-2017 Red Hat Inc. and/or its affiliates and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 1.0 which is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 *
 * SPDX-License-Identifier: EPL-1.0
 ********************************************************************************/
package org.eclipse.ceylon.ide.eclipse.code.correct;

import static org.eclipse.ceylon.ide.eclipse.code.correct.PrintProposal.addPrintProposal;
import static org.eclipse.ceylon.ide.eclipse.util.EditorUtil.getCurrentEditor;
import static org.eclipse.ceylon.ide.eclipse.util.Nodes.findNode;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorPart;

import org.eclipse.ceylon.compiler.typechecker.tree.Node;
import org.eclipse.ceylon.compiler.typechecker.tree.Tree;
import org.eclipse.ceylon.ide.eclipse.code.editor.CeylonEditor;
import org.eclipse.ceylon.ide.eclipse.code.parse.CeylonParseController;

public class PrintHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) 
            throws ExecutionException {
        IEditorPart editor = getCurrentEditor();
        if (editor instanceof CeylonEditor) {
            CeylonEditor ce = (CeylonEditor) editor;
            CeylonParseController controller = 
                    ce.getParseController();
            Tree.CompilationUnit rootNode = 
                    controller.getTypecheckedRootNode();
            if (rootNode!=null) {
                IRegion selection = ce.getSelection();
                int start = selection.getOffset();
                int end = start + selection.getLength();
                Node node = 
                        findNode(rootNode, 
                                controller.getTokens(), 
                                start, end);
                List<ICompletionProposal> list = 
                        new ArrayList<ICompletionProposal>();
                addPrintProposal(rootNode, list, node, start);
                if (!list.isEmpty()) {
                    IDocument doc = 
                            ce.getCeylonSourceViewer()
                                .getDocument();
                    ICompletionProposal proposal = 
                            list.get(0);
                    proposal.apply(doc);
                    Point point = proposal.getSelection(doc);
                    ce.getSelectionProvider()
                        .setSelection(new TextSelection(
                                point.x, point.y));
                }
            }
        }
        return null;
    }

}
