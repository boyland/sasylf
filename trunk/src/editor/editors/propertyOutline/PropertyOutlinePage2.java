package editor.editors.propertyOutline;


import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

import edu.cmu.cs.sasylf.ast.Case;
import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.ast.Derivation;
import edu.cmu.cs.sasylf.ast.DerivationByAnalysis;
import edu.cmu.cs.sasylf.ast.Element;
import edu.cmu.cs.sasylf.ast.Fact;
import edu.cmu.cs.sasylf.ast.Judgment;
import edu.cmu.cs.sasylf.ast.Location;
import edu.cmu.cs.sasylf.ast.RuleCase;
import edu.cmu.cs.sasylf.ast.Theorem;
import edu.cmu.cs.sasylf.parser.DSLToolkitParser;
import edu.cmu.cs.sasylf.parser.ParseException;

/**
 * A content outline page which always represents the content of the
 * connected editor in 10 segments.
 */
public class PropertyOutlinePage2 extends ContentOutlinePage {

//	private Object rootElement;
	/**
	 * Divides the editor's document into ten segments and provides elements for them.
	 */
	protected class ContentProvider implements ITreeContentProvider {

		protected final static String SEGMENTS= "__slf_segments"; //$NON-NLS-1$
		protected IPositionUpdater fPositionUpdater= new DefaultPositionUpdater(SEGMENTS);
		protected List<PropertyElement> pList= new ArrayList<PropertyElement>();
		
		private Position convertLocToPos(IDocument document, Location loc) {
			Position pos = null;
			try {
				int lineOffset = document.getLineOffset(loc.getLine() - 1);
				int lineLength = document.getLineLength(loc.getLine() - 1);
				pos = new Position(lineOffset, lineLength);
				document.addPosition(pos);
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
			return pos;
		}
		
		private void parse(IDocument document, IFile documentFile) {
			CompUnit cu = null;
			try {
				cu = DSLToolkitParser.read(documentFile.getName(),documentFile.getContents());
			} catch (CoreException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			}
			
			if(cu == null) {
				return;
			}
			
			PropertyElement pe = null;
			
			//terminals
//			for (Terminal t : cu.getTerminals()) {
//				if (Character.isJavaIdentifierStart(t.getSymbol().charAt(0))) {
//					pe = new PropertyElement("terminal", t.getGrmSymbol().toString());
//					pe.setPosition(convertLocToPos(document, t.getLocation()));
//					pList.add(pe);
//				}
//			}
//			
			//judgments
			for (Judgment judg : cu.getJudgments()) {
				pe = new PropertyElement("Judgment", (judg.getName() + ": " + judg.getForm()).replaceAll("\"", ""));
				pe.setPosition(convertLocToPos(document, judg.getLocation()));
				pList.add(pe);
			}
			
			//theorem
			for (Theorem theo : cu.getTheorems()) {
				StringBuilder sb = new StringBuilder();
				sb.append(theo.getName()).append(" : forall ");
				for(Fact fact : theo.getForalls()) {
					sb.append(fact).append(" ");
				}
				sb.append("exists ");
				for(Element element : theo.getExists().getElements()) {
					sb.append(element).append(" ");
				}
				pe = new PropertyElement("Theorem", sb.toString().replaceAll("\"", ""));
				pe.setPosition(convertLocToPos(document, theo.getLocation()));
				pList.add(pe);
				cStack.push(pe);
				for(Derivation deri: theo.getDerivations()) {
					if(deri instanceof DerivationByAnalysis) {
						findCaseRule(document, ((DerivationByAnalysis) deri).getCases());
					}
				}
				cStack.pop();
			}
		}
		
		Stack<PropertyElement> cStack = new Stack<PropertyElement>();

		private void findCaseRule(IDocument document, List<Case> rList) {
			for(Case _case : rList) {
				if(_case instanceof RuleCase) {
					RuleCase ruleCase = (RuleCase) _case;
					PropertyElement pe = new PropertyElement("Case rule", ruleCase.getRuleName());
					pe.setPosition(convertLocToPos(document, ruleCase.getLocation()));
					if(!cStack.empty()) {
						cStack.peek().addChild(pe);
						pe.setParentElement(cStack.peek());
					}
					cStack.push(pe);
					for(Derivation deri : ruleCase.getDerivations()) {
						if(deri instanceof DerivationByAnalysis) {
							findCaseRule(document, ((DerivationByAnalysis) deri).getCases());
						}
					}
					if(!cStack.empty()) {
						cStack.pop();
					}
				}
			}
		}
		
		/*
		 * @see IContentProvider#inputChanged(Viewer, Object, Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (oldInput != null) {
				IDocument document= fDocumentProvider.getDocument(oldInput);
				if (document != null) {
					try {
						document.removePositionCategory(SEGMENTS);
					} catch (BadPositionCategoryException x) {
					}
					document.removePositionUpdater(fPositionUpdater);
				}
			}

			pList.clear();

			if (newInput != null) {
				IDocument document= fDocumentProvider.getDocument(newInput);
				if (document != null) {
					document.addPositionCategory(SEGMENTS);
					document.addPositionUpdater(fPositionUpdater);
					
					if(newInput instanceof IFileEditorInput) {
						//String filePath = ((IFileEditorInput) newInput).getFile().getLocationURI().getPath().replaceFirst("/", "");
						IFile file = ((IFileEditorInput)newInput).getFile(); // new File(filePath);
						parse(document, file);
						
					}
				}
			}
		}

		/*
		 * @see IContentProvider#dispose
		 */
		public void dispose() {
			if (pList != null) {
				pList.clear();
				pList= null;
			}
		}

		/*
		 * @see IContentProvider#isDeleted(Object)
		 */
		public boolean isDeleted(Object element) {
			return false;
		}

		/*
		 * @see IStructuredContentProvider#getElements(Object)
		 */
		public Object[] getElements(Object element) {
			return pList.toArray();
		}

		/*
		 * @see ITreeContentProvider#hasChildren(Object)
		 */
		public boolean hasChildren(Object element) {
			return ((PropertyElement)element).hasChildren();
		}

		/*
		 * @see ITreeContentProvider#getParent(Object)
		 */
		public Object getParent(Object element) {
			if (element instanceof PropertyElement)
				return ((PropertyElement)element).getParentElement();
			return null;
		}

		/*
		 * @see ITreeContentProvider#getChildren(Object)
		 */
		public Object[] getChildren(Object element) {
			if (element instanceof PropertyElement) {
				return ((PropertyElement)element).getChildren().toArray();
			}
			return null;
		}
	}

	protected Object fInput;
	protected IDocumentProvider fDocumentProvider;
	protected ITextEditor fTextEditor;

	/**
	 * Creates a content outline page using the given provider and the given editor.
	 * 
	 * @param provider the document provider
	 * @param editor the editor
	 */
	public PropertyOutlinePage2(IDocumentProvider provider, ITextEditor editor) {
		super();
		this.fDocumentProvider = provider;
		this.fTextEditor = editor;
	}

	/* (non-Javadoc)
	 * Method declared on ContentOutlinePage
	 */
	public void createControl(Composite parent) {

		super.createControl(parent);

		TreeViewer viewer= getTreeViewer();
		viewer.setContentProvider(new ContentProvider());
		viewer.setLabelProvider(new LabelProvider());
		viewer.addSelectionChangedListener(this);

		if (fInput != null)
			viewer.setInput(fInput);
	}
	
	/* (non-Javadoc)
	 * Method declared on ContentOutlinePage
	 */
	public void selectionChanged(SelectionChangedEvent event) {

		super.selectionChanged(event);

		ISelection selection= event.getSelection();
		if (selection.isEmpty())
			fTextEditor.resetHighlightRange();
		else {
			PropertyElement element = (PropertyElement) ((IStructuredSelection) selection).getFirstElement();
			int start= element.getPosition().getOffset();
			int length= element.getPosition().getLength();
			try {
				fTextEditor.setHighlightRange(start, length, true);
			} catch (IllegalArgumentException x) {
				fTextEditor.resetHighlightRange();
			}
		}
	}
	
	/**
	 * Sets the input of the outline page
	 * 
	 * @param input the input of this outline page
	 */
	public void setInput(Object input) {
		fInput= input;
		update();
	}
	
	/**
	 * Updates the outline page.
	 */
	public void update() {
		TreeViewer viewer= getTreeViewer();

		if (viewer != null) {
			Control control= viewer.getControl();
			if (control != null && !control.isDisposed()) {
				control.setRedraw(false);
				viewer.setInput(fInput);
				viewer.expandAll();
				control.setRedraw(true);
			}
		}
	}
}
