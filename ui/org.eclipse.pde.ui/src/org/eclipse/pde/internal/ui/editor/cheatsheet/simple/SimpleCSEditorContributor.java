/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.cheatsheet.simple;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.ui.editor.PDEFormEditorContributor;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.actions.SimpleCSPreviewAction;

/**
 * SimpleCSEditorContributor
 *
 */
public class SimpleCSEditorContributor extends PDEFormEditorContributor {

	private SimpleCSPreviewAction fPreviewAction;
	
	/**
	 * @param menuName
	 */
	public SimpleCSEditorContributor() {
		super("simpleCSEditor"); //$NON-NLS-1$
		fPreviewAction = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditorContributor#makeActions()
	 */
	protected void makeActions() {
		super.makeActions();
		// Make the preview action
		fPreviewAction = new SimpleCSPreviewAction();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditorContributor#contextMenuAboutToShow(org.eclipse.jface.action.IMenuManager, boolean)
	 */
	public void contextMenuAboutToShow(IMenuManager manager, boolean addClipboard) {
		// Get the model
		ISimpleCSModel model = (ISimpleCSModel)getEditor().getAggregateModel();
		// Set the cheat sheet object
		fPreviewAction.setDataModelObject(model.getSimpleCS());
		// Set the editor input
		fPreviewAction.setEditorInput(getEditor().getEditorInput());
		// Add the preview action to the context menu
		manager.add(fPreviewAction);
		manager.add(new Separator());
		super.contextMenuAboutToShow(manager, addClipboard);
	}	

	/**
	 * @return
	 */
	public SimpleCSPreviewAction getPreviewAction() {
		return fPreviewAction;
	}
	
}
