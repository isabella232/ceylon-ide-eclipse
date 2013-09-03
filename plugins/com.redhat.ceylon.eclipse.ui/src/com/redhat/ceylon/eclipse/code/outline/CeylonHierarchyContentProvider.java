package com.redhat.ceylon.eclipse.code.outline;

import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.redhat.ceylon.compiler.typechecker.model.Declaration;
import com.redhat.ceylon.compiler.typechecker.model.TypeDeclaration;
import com.redhat.ceylon.eclipse.code.editor.CeylonEditor;

public final class CeylonHierarchyContentProvider 
        implements ITreeContentProvider {
	
	final CeylonEditor editor;
	
	public static final class RootNode {
		Declaration declaration;
		public RootNode(Declaration declaration) {
			this.declaration = declaration;
		}
	}
	
	HierarachyMode mode = HierarachyMode.HIERARCHY;
	
	Declaration declaration;
	
	CeylonHierarchyBuilder builder;
	
	CeylonHierarchyContentProvider(CeylonEditor editor) {
		this.editor = editor;
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput!=null) {
			declaration = ((RootNode) newInput).declaration;
			try {
				builder = new CeylonHierarchyBuilder(this, declaration);
                editor.getSite().getWorkbenchWindow().run(true, true, builder);
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	boolean isShowingRefinements() {
		return !(declaration instanceof TypeDeclaration);
	}
    
	@Override
	public void dispose() {}

	@Override
	public boolean hasChildren(Object element) {
	    return getChildren(element).length>0;
	}

	@Override
	public Object getParent(Object element) {
	    return null;
	}

	@Override
	public Object[] getElements(Object inputElement) {
	    return getChildren(inputElement);
	}

	@Override
	public CeylonHierarchyNode[] getChildren(Object parentElement) {
	    if (parentElement instanceof RootNode) {
	    	switch (mode) {
	    	case HIERARCHY:
		        return new CeylonHierarchyNode[] { builder.getHierarchyRoot() };		    		
	    	case SUPERTYPES:
	    		return new CeylonHierarchyNode[] { builder.getSupertypesRoot() };
	    	case SUBTYPES:
	    		return new CeylonHierarchyNode[] { builder.getSubtypesRoot() };
	    	default:
	    		throw new RuntimeException();
	    	}
	    }
	    else if (parentElement instanceof CeylonHierarchyNode) {
	    	List<CeylonHierarchyNode> children = ((CeylonHierarchyNode) parentElement).getChildren();
			return children.toArray(new CeylonHierarchyNode[children.size()]);
	    }
	    else {
	    	return null;
	    }
	}
}