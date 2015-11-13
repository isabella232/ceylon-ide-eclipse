package com.redhat.ceylon.eclipse.code.refactor;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;

import com.redhat.ceylon.eclipse.java2ceylon.RefactorJ2C;
import com.redhat.ceylon.ide.common.refactoring.ExtractLinkedModeEnabled;
import com.redhat.ceylon.ide.common.refactoring.ExtractValueRefactoring;

public class refactorJ2C implements RefactorJ2C {
    @Override
    public ExtractValueRefactoring<IFile, ICompletionProposal, IDocument, InsertEdit, TextEdit, TextChange, IRegion> newExtractValueRefactoring(IEditorPart editorPart) {
        return new EclipseExtractValueRefactoring(editorPart);
    }
    @Override
    public ExtractLinkedModeEnabled<IRegion> toExtractLinkedModeEnabled(ExtractValueRefactoring<IFile, ICompletionProposal, IDocument, InsertEdit, TextEdit, TextChange, IRegion> refactoring) {
        return (ExtractLinkedModeEnabled<IRegion>) refactoring;
    }
}
