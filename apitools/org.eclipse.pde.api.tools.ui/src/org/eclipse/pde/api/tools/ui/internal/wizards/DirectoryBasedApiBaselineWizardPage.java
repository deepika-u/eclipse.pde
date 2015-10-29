/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Manumitting Technologies Inc - bug 324310
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.wizards;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.model.SystemLibraryApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.ui.internal.ApiToolsLabelProvider;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.PlatformUI;

/**
 * The wizard page allowing a new directory-based API profiles to be created or
 * an existing one to be edited.
 * 
 * @since 1.0.0
 */
public class DirectoryBasedApiBaselineWizardPage extends ApiBaselineWizardPage {
	public static boolean isApplicable(IApiBaseline profile) {
		String loc = profile.getLocation();
		return loc != null && new Path(loc).toFile().exists();
	}

	/**
	 * Resets the baseline contents based on current settings and a location
	 * from which to read plug-ins.
	 */
	class ReloadOperation implements IRunnableWithProgress {
		private String location, name;

		/**
		 * Constructor
		 * 
		 * @param platformPath
		 */
		public ReloadOperation(String name, String location) {
			this.location = location;
			this.name = name;
		}

		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			monitor.beginTask(WizardMessages.ApiProfileWizardPage_0, 10);
			try {
				fProfile = ApiModelFactory.newApiBaseline(name, location);
				ApiModelFactory.addComponents(fProfile, location, monitor);
				DirectoryBasedApiBaselineWizardPage.this.contentchange = true;
			} catch (CoreException e) {
				ApiPlugin.log(e);
			} finally {
				monitor.done();
			}
		}

	}


	/**
	 * widgets
	 */
	private Text nametext = null;
	private TreeViewer treeviewer = null;
	Combo locationcombo = null;
	private Button browsebutton = null, reloadbutton = null;

	/**
	 * We need to know if we are initializing the page to not respond to changed
	 * events causing validation when the wizard opens.
	 * 
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=266597
	 */
	private boolean initializing = false;

	/**
	 * Constructor
	 * 
	 * @param profile
	 */
	protected DirectoryBasedApiBaselineWizardPage(IApiBaseline profile) {
		super(profile);
	}

	@Override
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 4, 1, GridData.FILL_HORIZONTAL);
		SWTFactory.createWrapLabel(comp, WizardMessages.ApiProfileWizardPage_5, 1);
		nametext = SWTFactory.createText(comp, SWT.BORDER | SWT.SINGLE, 3, GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL | GridData.BEGINNING);
		nametext.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setPageComplete(pageValid());
			}
		});

		SWTFactory.createVerticalSpacer(comp, 1);

		SWTFactory.createWrapLabel(comp, WizardMessages.ApiProfileWizardPage_9, 1);
		locationcombo = SWTFactory.createCombo(comp, SWT.BORDER | SWT.SINGLE, 1, GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL | GridData.BEGINNING, null);
		locationcombo.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setPageComplete(pageValid());
				updateButtons();
			}
		});
		browsebutton = SWTFactory.createPushButton(comp, WizardMessages.ApiProfileWizardPage_10, null);
		browsebutton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setMessage(WizardMessages.ApiProfileWizardPage_11);
				String loctext = locationcombo.getText().trim();
				if (loctext.length() > 0) {
					dialog.setFilterPath(loctext);
				}
				String newPath = dialog.open();
				if (newPath != null && (!new Path(loctext).equals(new Path(newPath)) || getCurrentComponents().length == 0)) {
					/*
					 * If the path is identical, but there is no component
					 * loaded, we still want to reload. This might be the case
					 * if the combo is initialized by copy/paste with a path
					 * that points to a plugin directory
					 */
					locationcombo.setText(newPath);
					setErrorMessage(null);
					doReload();
				}
			}
		});

		reloadbutton = SWTFactory.createPushButton(comp, WizardMessages.ApiProfileWizardPage_12, null);
		reloadbutton.setEnabled(locationcombo.getText().trim().length() > 0);
		reloadbutton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doReload();
			}
		});

		SWTFactory.createWrapLabel(comp, WizardMessages.ApiProfileWizardPage_13, 4);
		Tree tree = new Tree(comp, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 250;
		gd.horizontalSpan = 4;
		tree.setLayoutData(gd);
		treeviewer = new TreeViewer(tree);
		treeviewer.setLabelProvider(new ApiToolsLabelProvider());
		treeviewer.setContentProvider(new ContentProvider());
		treeviewer.setComparator(new ViewerComparator());
		treeviewer.setInput(getCurrentComponents());
		treeviewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons();
			}
		});
		treeviewer.addFilter(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IApiComponent) {
					IApiComponent component = (IApiComponent) element;
					try {
						if (component.isSourceComponent() || component.isSystemComponent()) {
							return false;
						}
					} catch (CoreException e) {
						ApiPlugin.log(e);
					}
					return true;
				}
				return !(element instanceof SystemLibraryApiComponent);
			}
		});

		setControl(comp);
		setPageComplete(fProfile != null);
		initialize();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(comp, IApiToolsHelpContextIds.APIPROFILES_WIZARD_PAGE);
		Dialog.applyDialogFont(comp);
	}

	/**
	 * Initializes the controls of the page if the profile is not
	 * <code>null</code>
	 */
	protected void initialize() {
		initializing = true;
		try {
			super.initialize();
			if (fProfile != null) {
				nametext.setText(fProfile.getName());
				IApiComponent[] components = fProfile.getApiComponents();
				HashSet<String> locations = new HashSet<String>();
				String loc = fProfile.getLocation();
				IPath location = null;
				if (loc != null) {
					location = new Path(loc);
					// check if the location is a file
					if (location.toFile().isDirectory()) {
						locations.add(location.removeTrailingSeparator().toOSString());
					}
				} else {
					for (int i = 0; i < components.length; i++) {
						if (!components[i].isSystemComponent()) {
							location = new Path(components[i].getLocation()).removeLastSegments(1);
							if (location.toFile().isDirectory()) {
								locations.add(location.removeTrailingSeparator().toOSString());
							}
						}
					}
				}
				if (locations.size() > 0) {
					locationcombo.setItems(locations.toArray(new String[locations.size()]));
					locationcombo.select(0);
				}
			} else {
				// try to set the default location to be the current install
				// directory
				// https://bugs.eclipse.org/bugs/show_bug.cgi?id=258969
				Location location = Platform.getInstallLocation();
				if (location != null) {
					URL url = location.getURL();
					IPath path = new Path(url.getFile()).removeTrailingSeparator();
					if (path.toFile().exists()) {
						locationcombo.add(path.toOSString());
						locationcombo.select(0);
					}
				}
			}
		} finally {
			initializing = false;
		}
	}

	/**
	 * Reloads all of the plugins from the location specified in the location
	 * text field.
	 */
	protected void doReload() {
		IRunnableWithProgress op = new ReloadOperation(nametext.getText().trim(), locationcombo.getText().trim());
		try {
			getContainer().run(true, true, op);
			treeviewer.setInput(getCurrentComponents());
			treeviewer.refresh();
			setPageComplete(pageValid());
		} catch (InvocationTargetException ite) {
		} catch (InterruptedException ie) {
		}
	}

	@Override
	public IApiBaseline finish() throws IOException, CoreException {
		if (fProfile != null) {
			fProfile.setName(nametext.getText().trim());
		}
		return fProfile;
	}

	/**
	 * @return if the page is valid, such that it is considered complete and can
	 *         be 'finished'
	 */
	protected boolean pageValid() {
		if (initializing) {
			return false;
		}
		setErrorMessage(null);
		if (!isNameValid(nametext.getText().trim())) {
			return false;
		}
		String text = locationcombo.getText().trim();
		if (text.length() < 1) {
			setErrorMessage(WizardMessages.ApiProfileWizardPage_23);
			reloadbutton.setEnabled(false);
			return false;
		}
		if (!new Path(text).toFile().exists()) {
			setErrorMessage(WizardMessages.ApiProfileWizardPage_24);
			reloadbutton.setEnabled(false);
			return false;
		}
		if (fProfile != null) {
			if (fProfile.getApiComponents().length == 0) {
				setErrorMessage(WizardMessages.ApiProfileWizardPage_2);
				return false;
			}
			IStatus status = fProfile.getExecutionEnvironmentStatus();
			if (status.getSeverity() == IStatus.ERROR) {
				setErrorMessage(status.getMessage());
				return false;
			}
			if (fProfile.getLocation() != null && !fProfile.getLocation().equals(locationcombo.getText())) {
				setErrorMessage(WizardMessages.ApiProfileWizardPage_location_needs_reset);
				return false;
			}
		} else {
			setErrorMessage(WizardMessages.ApiProfileWizardPage_location_needs_reset);
			return false;
		}
		return true;
	}

	/**
	 * Updates the state of a variety of buttons on this page
	 */
	protected void updateButtons() {
		String loctext = locationcombo.getText().trim();
		reloadbutton.setEnabled(loctext.length() > 0);
	}

}
