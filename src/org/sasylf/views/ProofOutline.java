package org.sasylf.views;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Stack;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.sasylf.Activator;
import org.sasylf.IDEProof;
import org.sasylf.ProofChecker;
import org.sasylf.util.DocumentUtil;
import org.sasylf.util.IProjectStorage;

import edu.cmu.cs.sasylf.ast.Case;
import edu.cmu.cs.sasylf.ast.Clause;
import edu.cmu.cs.sasylf.ast.Derivation;
import edu.cmu.cs.sasylf.ast.DerivationByAnalysis;
import edu.cmu.cs.sasylf.ast.Fact;
import edu.cmu.cs.sasylf.ast.Judgment;
import edu.cmu.cs.sasylf.ast.ModulePart;
import edu.cmu.cs.sasylf.ast.Node;
import edu.cmu.cs.sasylf.ast.RenameJudgment;
import edu.cmu.cs.sasylf.ast.Rule;
import edu.cmu.cs.sasylf.ast.RuleCase;
import edu.cmu.cs.sasylf.ast.Syntax;
import edu.cmu.cs.sasylf.ast.SyntaxDeclaration;
import edu.cmu.cs.sasylf.ast.Theorem;
import edu.cmu.cs.sasylf.module.Module;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.ParseUtil;

/**
 * A content outline page which summarizes the structure of a proof file.
 */
public class ProofOutline extends ContentOutlinePage implements ProofChecker.Listener {

	//	private Object rootElement;
	/**
	 * Divides the editor's document into ten segments and provides elements for them.
	 */
	protected class ContentProvider implements ITreeContentProvider {

		protected final static String SEGMENTS= "__slf_segments"; //$NON-NLS-1$
		protected IPositionUpdater fPositionUpdater= new DefaultPositionUpdater(SEGMENTS);
		protected NavigableSet<ProofElement> pList= new TreeSet<ProofElement>();

		protected final static String FORALL = "∀";
		protected final static String EXISTS = "∃";

		private boolean inDocument(Location loc, IDocument doc, String docName) {
			try {
				// XXX: It would better to find a way to do this without risking a exception
				DocumentUtil.getOffset(loc, doc);
			} catch (BadLocationException ex) {
				return false;
			}
			return true;
		}

		private Position convertLocToPos(IDocument document, Location loc) {
			Position pos = null;
			try {
				int lineOffset = document.getLineOffset(loc.getLine() - 1);
				int lineLength = document.getLineLength(loc.getLine() - 1);
				pos = new Position(lineOffset, lineLength);
				document.addPosition(SEGMENTS, pos);
			} catch (BadLocationException e) {
				e.printStackTrace();
			} catch (BadPositionCategoryException e) {
				e.printStackTrace();
			}
			return pos;
		}

		private Map<String,ProofElement> makeReuseIndex() {
			Map<String,ProofElement> index = new HashMap<String,ProofElement>();
			for (ProofElement pe : pList) {
				index.put(pe.toString(), pe);
			}
			return index;
		}
		
		private void useReuseIndex(Map<String, ProofElement> index) {
			List<ProofElement> newElements = new ArrayList<ProofElement>(pList);
			pList.clear();
			for (ProofElement newer : newElements) {
				ProofElement older = index.remove(newer.toString());
				if (older == null) pList.add(newer);
				else {
					// System.out.println("Found existing: " + older);
					older.updateTo(newer);
					pList.add(older);
				}
			}
		}
		
		public void newCompUnit(IDocument document, String documentName, Module cu) {
			if(cu == null) {
				return;
			}
			
			document.addPositionCategory(SEGMENTS);
			
			Map<String,ProofElement> reuseIndex = makeReuseIndex();
			pList.clear();

			List<Node> contents = new ArrayList<Node>();
			cu.collectTopLevel(contents);
			
			ProofElement pe;			
			for (Node decl : contents) {
				if (!inDocument(decl.getLocation(), document, documentName)) continue;
				if (decl instanceof Syntax) {
					Syntax syn = (Syntax)decl;
					pe = new ProofElement("Syntax", syn.toString());
					pe.setPosition(convertLocToPos(document,syn.getLocation()));
					pList.add(pe);
					if (syn instanceof SyntaxDeclaration) {
						for (Clause c : ((SyntaxDeclaration)syn).getClauses()) {
							ProofElement ce = new ProofElement("Clause",c.toString());
							Location loc = c.getLocation();
							ce.setPosition(convertLocToPos(document, loc));
							pe.addChild(ce);
						}
					}
				}
				else if (decl instanceof Judgment) {
					Judgment judg = (Judgment)decl;
					pe = new ProofElement("Judgment", (judg.getName() + ": " + judg.getForm()));
					pe.setPosition(convertLocToPos(document, judg.getLocation()));
					pList.add(pe);
					if (judg instanceof RenameJudgment) continue;
					for (Rule r : judg.getRules()) {
						StringBuilder sb = new StringBuilder();
						sb.append(r.getName()).append(": ");
						for (Clause cl : r.getPremises()) {
							sb.append(FORALL).append(cl).append(" ");
						}
						sb.append(EXISTS);
						sb.append(r.getConclusion());
						ProofElement re = new ProofElement("Rule", sb.toString());
						Location loc = r.getLocation();
						Position barPos = convertLocToPos(document, loc);
						try {
							String barPlusName = document.get(barPos.getOffset(), barPos.getLength()).trim();
							int n = 0;
							while (n < barPlusName.length() && ParseUtil.isBarChar(barPlusName.charAt(n))) {
								++n;
							}
							re.setLexicalInfo(barPlusName.substring(0,n));
						} catch (BadLocationException e) {
							// muffle;
						}
						if (r.getPremises().size() > 0) {
							loc = r.getPremises().get(0).getLocation();
						}
						re.setPosition(convertLocToPos(document, loc));
						pe.addChild(re);
					}
				}
				else if (decl instanceof Theorem) {
					Theorem theo = (Theorem)decl;
					StringBuilder sb = new StringBuilder();
					sb.append(theo.getName());
					sb.append(": ");
					for(Fact fact : theo.getForalls()) {
						sb.append(FORALL);
						sb.append(fact.getElement()).append(" ");
					}
					sb.append(EXISTS);
					sb.append(theo.getExists());
					pe = new ProofElement(theo.getKindTitle(), sb.toString());
					try {
						Position pos = DocumentUtil.getPosition(theo, document);
						pe.setPosition(pos);
						document.addPosition(pos);
					} catch (BadLocationException e) {
						// NB: This theorem is now gone.  Just ignore it:
						continue;
					}
					pList.add(pe);
				}
				else if (decl instanceof ModulePart) {
					ModulePart mpart = (ModulePart)decl;
					pe = new ProofElement("Module", mpart.getName() + ": " + mpart.getModule().toString());
					pe.setPosition(convertLocToPos(document,mpart.getLocation()));
					pList.add(pe);
				}
			}
			useReuseIndex(reuseIndex);
		}

		Stack<ProofElement> cStack = new Stack<ProofElement>();

		@SuppressWarnings("unused")  // considering removing this capability
		private void findCaseRule(IDocument document, List<Case> rList) {
			for(Case _case : rList) {
				if(_case instanceof RuleCase) {
					RuleCase ruleCase = (RuleCase) _case;
					ProofElement pe = new ProofElement("Case rule", ruleCase.getRuleName());
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
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// System.out.println("oldInput = " + oldInput + ", newInput = " + newInput);
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

					IProjectStorage st = IProjectStorage.Adapter.adapt(newInput);
					if(st != null) {
						newCompUnit(document, st.getName(), IDEProof.getCompUnit(st));
					}
				}
			}
		}

		/*
		 * @see IContentProvider#dispose
		 */
		@Override
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
		@Override
		public Object[] getElements(Object element) {
			return pList.toArray();
		}

		/*
		 * @see ITreeContentProvider#hasChildren(Object)
		 */
		@Override
		public boolean hasChildren(Object element) {
			return ((ProofElement)element).hasChildren();
		}

		/*
		 * @see ITreeContentProvider#getParent(Object)
		 */
		@Override
		public Object getParent(Object element) {
			if (element instanceof ProofElement)
				return ((ProofElement)element).getParentElement();
			return null;
		}

		/*
		 * @see ITreeContentProvider#getChildren(Object)
		 */
		@Override
		public Object[] getChildren(Object element) {
			if (element instanceof ProofElement) {
				List<ProofElement> children = ((ProofElement)element).getChildren();
				if (children == null) return null;
				return children.toArray();
			}
			return null;
		}

		public ProofElement findProofElementByName(String name) {
			String key = name + ": ";
			for (ProofElement pe : pList) {
				if (pe.getContent().startsWith(key)) return pe;
				if ("Judgment".equals(pe.getCategory())) {
					if (pe.getChildren() == null) continue;
					for (ProofElement ce : pe.getChildren()) {
						if (ce.getContent().startsWith(key)) return ce;
					}
				}
			}
			return null;
		}

		public List<ProofElement> findMatching(String category, String prefix) {
			if (category == null) category = "";
			// System.out.println("category = " + category + ", prefix = " + prefix);
			List<ProofElement> result = new ArrayList<ProofElement>();
			for (ProofElement pe : pList) {
				// System.out.println("  looking at " + pe);
				if (categoryMatch(pe.getCategory(),category) && pe.getContent().startsWith(prefix)) result.add(pe);
				if ("Judgment".equals(pe.getCategory())) {
					if (pe.getChildren() == null) continue;
					for (ProofElement ce : pe.getChildren()) {
						if (ce.getCategory().startsWith(category) &&
								ce.getContent().startsWith(prefix)) result.add(ce);
					}
				}
			}
			return result;
		}

		/**
		 * Find the smallest position (extent of text) that encloses the given offset.
		 * XXX: This code is not useful currently.
		 * @param offset offset within the document.
		 * @return position that encloses the offset, or null if none does.
		 */
		public Position findEnclosingPosition(int offset) {
			Position dummy = new Position(offset,Integer.MAX_VALUE);
			NavigableSet<ProofElement> partial = pList.headSet(new ProofElement(dummy),false);
			for (ProofElement pe : partial.descendingSet()) {
				Position pos = pe.getPosition();
				if (pos == null) continue;
				if (pos.includes(offset)) return pos;
			}
			return null;
		}

		protected boolean categoryMatch(String cat, String pattern) {
			if (pattern.length() == 0) return true;
			if (cat.equals(pattern)) return true;
			if (cat.equals("Lemma") && pattern.equals("Theorem") ||
					cat.equals("Theorem") && pattern.equals("Lemma")) return true;
			return false;
		}
	}

	private static class MyLabelProvider extends LabelProvider {

		private final Map<String,Image> kindImages = new HashMap<String,Image>();

		private void ensureImages() {
			if (kindImages.size() == 0) {
				Activator activator = Activator.getDefault();
				kindImages.put("Lemma",activator.getImage("icons/dull-green-ball.png"));
				kindImages.put("Theorem", activator.getImage("icons/mauve-ball.png"));
				kindImages.put("Rule", activator.getImage("icons/green-ball.png"));
				kindImages.put("Judgment", activator.getImage("icons/yellow-diamond.png"));
				kindImages.put("Syntax", activator.getImage("icons/small-yellow-diamond.png"));
				kindImages.put("Clause", activator.getImage("icons/small-green-ball.png"));
				kindImages.put("Package", activator.getImage("icons/packd_obj.png"));
				kindImages.put("Module", activator.getImage("icons/star.png"));
			}
		}


		@Override
		public Image getImage(Object element) {
			if (element instanceof ProofElement) {
				ensureImages();
				return kindImages.get(((ProofElement)element).getCategory());
			}
			return super.getImage(element);
		}

		@Override
		public String getText(Object element) {
			if (element instanceof ProofElement) {
				ProofElement pe = (ProofElement)element;
				return pe.getContent();
			}
			return super.getText(element);
		}


		@Override 
		public void dispose() {
			super.dispose();
			kindImages.clear();
		}
	}

	protected IEditorInput fInput;
	protected IDocumentProvider fDocumentProvider;
	protected ITextEditor fTextEditor;

	/**
	 * Creates a content outline page using the given provider and the given editor.
	 * @param provider the document provider
	 * @param editor the editor
	 */
	public ProofOutline(IDocumentProvider provider, ITextEditor editor) {
		super();
		this.fDocumentProvider = provider;
		this.fTextEditor = editor;
		ProofChecker.getInstance().addListener(this);
	}

	/* (non-Javadoc)
	 * Method declared on ContentOutlinePage
	 */
	@Override
	public void createControl(Composite parent) {

		super.createControl(parent);

		TreeViewer viewer= getTreeViewer();
		viewer.setContentProvider(new ContentProvider());
		viewer.setLabelProvider(new MyLabelProvider());
		viewer.addSelectionChangedListener(this);

		if (fInput != null)
			viewer.setInput(fInput);
	}

	/* (non-Javadoc)
	 * Method declared on ContentOutlinePage
	 */
	@Override
	public void selectionChanged(SelectionChangedEvent event) {

		super.selectionChanged(event);

		ISelection selection= event.getSelection();
		if (selection.isEmpty())
			fTextEditor.resetHighlightRange();
		else {
			ProofElement element = (ProofElement) ((IStructuredSelection) selection).getFirstElement();
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
	public void setInput(IEditorInput input) {
		fInput= input;
		IProjectStorage f = IProjectStorage.Adapter.adapt(input);
		if (f != null) {
			updateViewer(IDEProof.getProof(f), f.getName());
		}
	}

	@Override
	public void proofChecked(final IFile file, final IDEProof pf, int errors) {
		if (file == null || fInput == null) return;
		if (!file.equals(fInput.getAdapter(IFile.class))) return;
		final String proofName = file.getName();
		updateViewer(pf, proofName);
	}

	/**
	 * Update the outline with the proof given.
	 * @param pf proof to outline
	 * @param proofName name of file in which the proof is located.
	 */
	protected void updateViewer(final IDEProof pf, final String proofName) {
		if (pf == null || pf.getCompilation() == null) return;
		final TreeViewer viewer= getTreeViewer();
		if (viewer != null) {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					Control control= viewer.getControl();
					if (control != null && !control.isDisposed()) {
						control.setRedraw(false);
						ContentProvider provider = (ContentProvider)viewer.getContentProvider();
						IDocument doc = fDocumentProvider.getDocument(fInput);
						provider.newCompUnit(doc,proofName,pf.getCompilation());
						// viewer.expandAll();
						control.setRedraw(true);
						viewer.refresh(); // doesn't work if inside the controlled area.
					}
				}
			});
		}
	}


	@Override
	public void dispose() {
		super.dispose();
		fInput = null;
		ProofChecker.getInstance().removeListener(this);
	}

	/**
	 * Find a theorem or judgment by name.
	 * @param name name of theorem of judgment to locate
	 * @return position of declaration, or null if not found.
	 */
	public ProofElement findProofElementByName(String name) {
		if (getTreeViewer() == null) {
			System.out.println("No tree viewer!");
			return null;
		}
		ContentProvider provider = (ContentProvider)getTreeViewer().getContentProvider();
		return provider.findProofElementByName(name);
	}

	/**
	 * Return a list of content strings that start with the given key (for content assist)
	 */
	public List<String> findContentAssist(String category, String prefix) {
		List<String> result = new ArrayList<String>();
		if (getTreeViewer() == null) {
			System.out.println("No tree viewer!");
			return result;
		}
		ContentProvider provider = (ContentProvider)getTreeViewer().getContentProvider();
		if (category.equals("lemma")) category = "Lemma";
		else if (category.equals("theorem")) category = "Theorem";
		else if (category.equals("rule")) category = "Rule";
		else category = "";
		for (ProofElement pe : provider.findMatching(category, prefix)) {
			result.add(pe.getContent());
		}
		return result;
	}

	/**
	 * Find the smallest position (extent of text) that encloses the given offset.
	 * XXX Probably not useful.
	 * @param offset offset within the document.
	 * @return position that encloses the offset, or null if none does.
	 */
	public Position findEnclosingPosition(int offset) {
		if (getTreeViewer() == null) {
			System.out.println("No tree viewer!");
			return null;
		}
		ContentProvider provider = (ContentProvider)getTreeViewer().getContentProvider();
		return provider.findEnclosingPosition(offset);
	}
}
