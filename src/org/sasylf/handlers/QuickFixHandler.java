package org.sasylf.handlers;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IWorkbenchPartSite;
import org.sasylf.editors.MarkerResolutionGenerator;
import org.sasylf.util.CompletionProposalMarkerResolution;
import org.sasylf.util.QuickFixPage;


/**
 * Handle a click on annotations in the vertical rule.
 * Unfortunately, we have to cheat and use Eclipse internal code,
 * since I can't find any extension points to handle clicks on annotations
 * in the vertical ruler.
 */
public class QuickFixHandler extends AbstractHandler {

	public QuickFixHandler() { }

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		return null;
	}

	private static class QuickFixResolutionMap extends HashMap<IMarkerResolution,Collection<IMarker>> {
		/**
		 * Keep Eclipse Happy
		 */
		private static final long serialVersionUID = 1L;
	}

	private static class QuickFixWizardDialog extends WizardDialog {
		public QuickFixWizardDialog(Shell parentShell, IWizard newWizard) {
			super(parentShell, newWizard);
			setShellStyle(SWT.CLOSE | SWT.MAX | SWT.TITLE | SWT.BORDER
					| SWT.MODELESS | SWT.RESIZE | getDefaultOrientation());
		}
	}



	private static class QuickFixWizard extends Wizard {

		private final String description;
		private final IMarker[] markers;
		private final QuickFixResolutionMap resolutionMap;
		private final IWorkbenchPartSite partSite;

		public QuickFixWizard(String desc, IMarker[] ms, QuickFixResolutionMap map, IWorkbenchPartSite site) {
			description = desc;
			markers = ms;
			resolutionMap = map;
			partSite = site;
		}

		@Override
		public void addPages() {
			super.addPages();
			addPage(new QuickFixPage(description, markers, resolutionMap, partSite));
		}

		@Override
		public boolean performFinish() {
			IWizardPage[] pages = getPages();
			for (int i = 0; i < pages.length; i++) {
				QuickFixPage wizardPage = (QuickFixPage) pages[i];
				wizardPage.performFinish();
			}
			return true;
		}
	}

	public static void showQuickFixes(IDocument doc, IWorkbenchPartSite site, IMarker[] markers) {
		QuickFixResolutionMap resolutionMap = new QuickFixResolutionMap();
		for (int i=0; i < markers.length; ++i) {
			ICompletionProposal[] proposals = MarkerResolutionGenerator.getProposals(doc, markers[i]);
			if (proposals != null) {
				for (ICompletionProposal prop : proposals) {
					resolutionMap.put(new CompletionProposalMarkerResolution(doc, prop),Collections.singletonList(markers[i]));
				}
			}
		}
		if (resolutionMap.size() > 0) {
			IMarker oneMarker = resolutionMap.values().iterator().next().iterator().next();
			Wizard wizard = new QuickFixWizard("Select the fix for '" +oneMarker.getAttribute(IMarker.MESSAGE, "problem found")+"'", markers, resolutionMap, site);
			wizard.setWindowTitle("Quick Fix");

			WizardDialog dialog = new QuickFixWizardDialog(site.getShell(), wizard);
			dialog.open();
		} else {
			MessageDialog.openError(site.getShell(), "Quick Fix Error", "No quick fixes available");
		}
	}
}
