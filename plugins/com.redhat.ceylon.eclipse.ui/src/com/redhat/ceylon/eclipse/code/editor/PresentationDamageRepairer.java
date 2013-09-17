package com.redhat.ceylon.eclipse.code.editor;

import static com.redhat.ceylon.eclipse.code.parse.CeylonSourcePositionLocator.getEndOffset;
import static com.redhat.ceylon.eclipse.code.parse.CeylonSourcePositionLocator.getStartOffset;
import static com.redhat.ceylon.eclipse.code.parse.CeylonSourcePositionLocator.getTokenIndexAtCharacter;
import static com.redhat.ceylon.eclipse.code.parse.CeylonTokenColorer.getColoring;
import static com.redhat.ceylon.eclipse.code.parse.CeylonTokenColorer.getInterpolationColoring;
import static com.redhat.ceylon.eclipse.code.parse.CeylonTokenColorer.getMemberColoring;

import java.util.Iterator;
import java.util.List;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.presentation.IPresentationDamager;
import org.eclipse.jface.text.presentation.IPresentationRepairer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;

import com.redhat.ceylon.compiler.typechecker.parser.CeylonLexer;
import com.redhat.ceylon.compiler.typechecker.parser.CeylonParser;

class PresentationDamageRepairer implements IPresentationDamager, 
        IPresentationRepairer {
	
    private final ISourceViewer sourceViewer;
    private volatile List<CommonToken> tokens;
    private final CeylonEditor editor;
    
	PresentationDamageRepairer(ISourceViewer sourceViewer, CeylonEditor editor) {
		this.sourceViewer = sourceViewer;
		this.editor = editor;
	}
	
	public IRegion getDamageRegion(ITypedRegion partition, 
			DocumentEvent event, boolean documentPartitioningChanged) {

		if (tokens==null) {
			//parse and color the whole document the first time!
			return partition;
		}
		
		if (noTextChange(event)) {
			//it was a change to annotations - don't reparse
			return new Region(event.getOffset(), 
					event.getLength());
		}
		
		int i = getTokenIndexAtCharacter(tokens, event.getOffset()-1);
		if (i<0) i=-i;
		CommonToken t = tokens.get(i);
		if (isWithinExistingToken(event, t)) {
			if (isWithinTokenChange(event, t)) {
				//the edit just changes the text inside
				//a token, leaving the rest of the
				//document structure unchanged
				return new Region(event.getOffset(), 
						event.getText().length());
			}
		}
		return partition;
	}

	public boolean isWithinExistingToken(DocumentEvent event, 
			CommonToken t) {
		return t.getStartIndex()<=event.getOffset() && 
				t.getStopIndex()>=event.getOffset()+event.getLength()-1;
	}

	public boolean isWithinTokenChange(DocumentEvent event,
			CommonToken t) {
		switch (t.getType()) {
		case CeylonLexer.WS:
			for (char c: event.getText().toCharArray()) {
				if (!Character.isWhitespace(c)) {
					return false;
				}
			}
			break;
		case CeylonLexer.UIDENTIFIER:
		case CeylonLexer.LIDENTIFIER:
			for (char c: event.getText().toCharArray()) {
				if (!Character.isJavaIdentifierPart(c)) {
					return false;
				}
			}
			break;
		case CeylonLexer.STRING_LITERAL:
        case CeylonLexer.CHAR_LITERAL:
			for (char c: event.getText().toCharArray()) {
				if (c=='"'||c=='\'') {
					return false;
				}
			}
			break;
		case CeylonLexer.MULTI_COMMENT:
			for (char c: event.getText().toCharArray()) {
				if (c=='/'||c=='*') {
					return false;
				}
			}
			break;
		case CeylonLexer.LINE_COMMENT:
			for (char c: event.getText().toCharArray()) {
				if (c=='\n'||c=='\f'||c=='\r') {
					return false;
				}
			}
			break;
		default:
			return false;
		}
		return true;
	}
	
	public void createPresentation(TextPresentation presentation, 
			ITypedRegion damage) {
		String text = sourceViewer.getDocument().get();
		ANTLRStringStream input = new ANTLRStringStream(text);
		CeylonLexer lexer = new CeylonLexer(input);
		CommonTokenStream tokenStream = new CommonTokenStream(lexer);
		
		CeylonParser parser = new CeylonParser(tokenStream);
		try {
		    parser.compilationUnit();
		}
		catch (RecognitionException e) {
		    throw new RuntimeException(e);
		}
		
		//it sounds strange, but it's better to parse
		//and cache here than in getDamageRegion(),
		//because these methods get called in strange
		//orders
		tokens = (List<CommonToken>) tokenStream.getTokens();
		
		highlightTokens(presentation, damage);
		// The document might have changed since the presentation was computed, so
		// trim the presentation's "result window" to the current document's extent.
		// This avoids upsetting SWT, but there's still a question as to whether
		// this is really the right thing to do. i.e., this assumes that the
		// presentation will get recomputed later on, when the new document change
		// gets noticed. But will it?
		/*IDocument doc = sourceViewer.getDocument();
		int newDocLength= doc!=null ? doc.getLength() : 0;
		IRegion presExtent= presentation.getExtent();
		if (presExtent.getOffset() + presExtent.getLength() > newDocLength) {
			presentation.setResultWindow(new Region(presExtent.getOffset(), 
					newDocLength - presExtent.getOffset()));
		}*/
		sourceViewer.changeTextPresentation(presentation, true);
		
//		ProjectionAnnotationModel annotationModel = sourceViewer.getProjectionAnnotationModel();
//		if (annotationModel!=null) {
//			new FoldingUpdater(sourceViewer).updateFoldingStructure(cu, tokens, annotationModel);
//		}
	}

	private void highlightTokens(TextPresentation presentation,
			ITypedRegion damage) {
		//int prevStartOffset= -1;
		//int prevEndOffset= -1;
		boolean inMetaLiteral=false;
		int inInterpolated=0;
		boolean afterMemberOp = false;
		//start iterating tokens
		Iterator<CommonToken> iter= tokens.iterator();
		if (iter!=null) {
			while (iter.hasNext()) {
				CommonToken token= iter.next();
				int tt = token.getType();
                if (tt==CeylonLexer.EOF) {
					break;
				}
                switch (tt) {
                case CeylonParser.BACKTICK:
				    inMetaLiteral = !inMetaLiteral;
				    break;
                case CeylonParser.STRING_START:
                    inInterpolated++;
                    break;
                case CeylonParser.STRING_END:
                    inInterpolated--;
                    break;
				}
				
				int startOffset= getStartOffset(token);
				int endOffset= getEndOffset(token);
				if (endOffset<damage.getOffset()) continue;
				if (startOffset>damage.getOffset()+damage.getLength()) break;
				
                switch (tt) {
                case CeylonParser.STRING_MID:
                    endOffset-=2; startOffset+=2; 
                    break;
                case CeylonParser.STRING_START:
                    endOffset-=2;
                    break;
                case CeylonParser.STRING_END:
                    startOffset+=2; 
                    break;
                }
				/*if (startOffset <= prevEndOffset && 
						endOffset >= prevStartOffset) {
					//this case occurs when applying a
					//quick fix, and causes an error
					//from SWT if we let it through
					continue;
				}*/
				if (tt==CeylonParser.STRING_MID ||
				    tt==CeylonParser.STRING_END) {
                    changeTokenPresentation(presentation, 
                            getInterpolationColoring(),
                            startOffset-2,startOffset-1,
                            inInterpolated>1 ? SWT.ITALIC : SWT.NORMAL);
				}
				changeTokenPresentation(presentation, 
						afterMemberOp && tt==CeylonLexer.LIDENTIFIER ?
						        getMemberColoring() : getColoring(token), 
						startOffset, endOffset,
						inMetaLiteral || inInterpolated>1 ||
						    inInterpolated>0
						        && tt!=CeylonParser.STRING_START
						        && tt!=CeylonParser.STRING_MID
						        && tt!=CeylonParser.STRING_END? 
						            SWT.ITALIC : SWT.NORMAL);
                if (tt==CeylonParser.STRING_MID ||
                    tt==CeylonParser.STRING_START) {
                    changeTokenPresentation(presentation, 
                            getInterpolationColoring(),
                            endOffset+1,endOffset+2,
                            inInterpolated>1 ? SWT.ITALIC : SWT.NORMAL);
                }
				//prevStartOffset= startOffset;
				//prevEndOffset= endOffset;
                afterMemberOp = tt==CeylonLexer.MEMBER_OP ||
                		        tt==CeylonLexer.SAFE_MEMBER_OP||
                		        tt==CeylonLexer.SPREAD_OP;
			}
		}
	}
	
    private void changeTokenPresentation(TextPresentation presentation, 
    		TextAttribute attribute, int startOffset, int endOffset,
    		int extraStyle) {
    	
		StyleRange styleRange= new StyleRange(startOffset, 
        		endOffset-startOffset,
                attribute==null ? null : attribute.getForeground(),
                attribute==null ? null : attribute.getBackground(),
                attribute==null ? extraStyle : attribute.getStyle()|extraStyle);
		
		if (editor!=null) {
		    LinkedModeModel linkedMode = editor.getLinkedMode();
		    if (linkedMode!=null &&
		            (linkedMode.anyPositionContains(startOffset) ||
		                    linkedMode.anyPositionContains(endOffset))) {
		        return;
		    }
		}
		
        // Negative (possibly 0) length style ranges will cause an 
        // IllegalArgumentException in changeTextPresentation(..)
        /*if (styleRange.length <= 0 || 
        		styleRange.start+styleRange.length > 
                        sourceViewer.getDocument().getLength()) {
        	//do nothing
        } 
        else {*/
            presentation.addStyleRange(styleRange);
        //}
    }

    private boolean noTextChange(DocumentEvent event) {
		try {
			return sourceViewer.getDocument()
					.get(event.getOffset(),event.getLength())
					.equals(event.getText());
		} 
		catch (BadLocationException e) {
			return false;
		}
	}
	
	public void setDocument(IDocument document) {}
}