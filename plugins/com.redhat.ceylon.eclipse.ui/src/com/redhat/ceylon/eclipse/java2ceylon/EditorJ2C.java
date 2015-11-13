package com.redhat.ceylon.eclipse.java2ceylon;

import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;

import com.redhat.ceylon.ide.common.editor.AbstractFormatAction;

public interface EditorJ2C {
    public AbstractFormatAction<IDocument,InsertEdit,TextEdit,TextChange> eclipseFormatAction();
}
