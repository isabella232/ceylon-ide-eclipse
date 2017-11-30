/********************************************************************************
 * Copyright (c) 2011-2017 Red Hat Inc. and/or its affiliates and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 1.0 which is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 *
 * SPDX-License-Identifier: EPL-1.0
 ********************************************************************************/
package org.eclipse.ceylon.ide.eclipse.code.hover;

import static org.eclipse.ceylon.ide.eclipse.code.editor.Navigation.gotoDeclaration;
import static org.eclipse.jdt.ui.PreferenceConstants.APPEARANCE_JAVADOC_FONT;
import static org.eclipse.ceylon.ide.eclipse.java2ceylon.Java2CeylonProxies.hoverJ2C;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.AbstractReusableInformationControlCreator;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInputChangedListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.ceylon.ide.eclipse.code.browser.BrowserInformationControl;
import org.eclipse.ceylon.ide.eclipse.code.editor.CeylonEditor;
import org.eclipse.ceylon.ide.eclipse.code.hover.DocumentationHover.BackAction;
import org.eclipse.ceylon.ide.eclipse.code.hover.DocumentationHover.ForwardAction;
import org.eclipse.ceylon.ide.eclipse.ui.CeylonPlugin;
import org.eclipse.ceylon.ide.eclipse.ui.CeylonResources;

final class CeylonEnrichedInformationControlCreator 
        extends AbstractReusableInformationControlCreator {

    private final CeylonEditor editor;

    public CeylonEnrichedInformationControlCreator(CeylonEditor editor) {
        this.editor = editor;
    }

    @Override
    public IInformationControl doCreateInformationControl(Shell parent) {
        ToolBarManager tbm = new ToolBarManager(SWT.FLAT);
        BrowserInformationControl control = 
                new BrowserInformationControl(parent, 
                        APPEARANCE_JAVADOC_FONT, tbm);

        final BackAction backAction = 
                new DocumentationHover.BackAction(control);
        backAction.setEnabled(false);
        tbm.add(backAction);
        final ForwardAction forwardAction = 
                new DocumentationHover.ForwardAction(control);
        tbm.add(forwardAction);
        forwardAction.setEnabled(false);

        final OpenDeclarationAction openDeclarationAction = 
                new OpenDeclarationAction(control);
        tbm.add(openDeclarationAction);

        IInputChangedListener inputChangeListener = 
                new IInputChangedListener() {
            public void inputChanged(Object newInput) {
                backAction.update();
                forwardAction.update();
                boolean isDeclaration = false;
                if (newInput instanceof CeylonBrowserInput) {
                    CeylonBrowserInput input = 
                            (CeylonBrowserInput) newInput;
                    isDeclaration = input.getAddress()!=null;
                }
                openDeclarationAction.setEnabled(isDeclaration);
            }
        };
        control.addInputChangeListener(inputChangeListener);

        tbm.update(true);

        control.addLocationListener(new CeylonLocationListener(editor, control));
        return control;
    }

    /**
     * Action that opens the current hover input element.
     */
    final class OpenDeclarationAction extends Action {
        
        private final BrowserInformationControl fInfoControl;
        
        public OpenDeclarationAction(
                BrowserInformationControl infoControl) {
            fInfoControl = infoControl;
            setText("Open Declaration");
            setToolTipText("Open Declaration");
            ImageDescriptor descriptor = 
                    CeylonPlugin.imageRegistry()
                        .getDescriptor(CeylonResources.GOTO);
            setImageDescriptor(descriptor);
        }
        @Override
        public void run() {
            DocumentationHover.close(fInfoControl); //FIXME: should have protocol to hide, rather than dispose
            CeylonBrowserInput input = (CeylonBrowserInput) 
                    fInfoControl.getInput();
            gotoDeclaration(hoverJ2C().getDocGenerator().getLinkedModel(
                    new ceylon.language.String(input.getAddress()), 
                    editor.getParseController()));
        }
    }

}