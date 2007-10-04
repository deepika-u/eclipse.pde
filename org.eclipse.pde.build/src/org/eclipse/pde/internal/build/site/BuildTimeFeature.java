/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.site;

import java.net.URL;
import java.util.ArrayList;
import org.eclipse.pde.build.Constants;
import org.eclipse.pde.internal.build.site.compatibility.*;

public class BuildTimeFeature extends Feature {
	/**
	 * Simple file name of the default feature manifest file
	 * @since 3.4.0
	 */
	public static final String FEATURE_FILE = "feature"; //$NON-NLS-1$

	/**
	 * File extension of the default feature manifest file
	 * @since 3.4.0
	 */
	public static final String FEATURE_XML = FEATURE_FILE + ".xml"; //$NON-NLS-1$

	
	public BuildTimeFeature(String id, String version) {
		super(id, version);
	}

	public BuildTimeFeature() {
		super("", ""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private boolean binary = false;
	private int contextQualifierLength = -1;
	private BuildTimeSiteContentProvider 	contentProvider = null;
	private BuildTimeSite 				site = null;
	private URL								url = null;
	private String							rootLocation = null;

	public FeatureEntry[] getRawIncludedFeatureReferences() {
		ArrayList included = new ArrayList();
		FeatureEntry [] entries = getEntries();
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].isRequires() || entries[i].isPlugin())
				continue;
			included.add(entries[i]);
		}
		
		return (FeatureEntry[]) included.toArray(new FeatureEntry[included.size()]);
	}

	public FeatureEntry[] getIncludedFeatureReferences() {
		ArrayList included = new ArrayList();
		FeatureEntry [] entries = getEntries();
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].isRequires() || entries[i].isPlugin())
				continue;
			
			if(SiteManager.isValidEnvironment(entries[i])) {
				included.add(entries[i]);
			}
		}
		
		return (FeatureEntry[]) included.toArray(new FeatureEntry[included.size()]);
	}
	
	public FeatureEntry[] getPluginEntries() {
		ArrayList plugins = new ArrayList();
		FeatureEntry [] entries = getEntries();
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].isRequires() || !entries[i].isPlugin())
				continue;
			if(SiteManager.isValidEnvironment(entries[i])) {
				plugins.add(entries[i]);
			}
		}
		return (FeatureEntry[]) plugins.toArray(new FeatureEntry[plugins.size()]);
	}
	
	public FeatureEntry[] getRawPluginEntries() {
		ArrayList plugins = new ArrayList();
		FeatureEntry [] entries = getEntries();
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].isRequires() || !entries[i].isPlugin())
				continue;
			plugins.add(entries[i]);
		}
		return (FeatureEntry[]) plugins.toArray(new FeatureEntry[plugins.size()]);
	}
	
	public FeatureEntry[] getImports() {
		ArrayList imports = new ArrayList();
		FeatureEntry [] entries = getEntries();
		for (int i = 0; i < entries.length; i++) {
			if (!entries[i].isRequires())
				continue;
			imports.add(entries[i]);
		}
		return (FeatureEntry[]) imports.toArray(new FeatureEntry[imports.size()]);
	}
	
	public boolean isBinary() {
		return binary;
	}

	public void setBinary(boolean isCompiled) {
		this.binary = isCompiled;
	}

//	private VersionedIdentifier versionId;
//	
//	public VersionedIdentifier getVersionedIdentifier() {
//		if (versionId != null)
//			return versionId;
//
//		String id = getFeatureIdentifier();
//		String ver = getFeatureVersion();
//		if (id != null && ver != null) {
//			try {
//				versionId = new VersionedIdentifier(id, ver);
//				return versionId;
//			} catch (Exception e) {
//				//UpdateCore.warn("Unable to create versioned identifier:" + id + ":" + ver); //$NON-NLS-1$ //$NON-NLS-2$
//			}
//		}
//
//		versionId = new VersionedIdentifier(getURL().toExternalForm(), null);
//		return versionId;
//	}
	
//	public void setFeatureVersion(String featureVersion) {
//		super.setFeatureVersion(featureVersion);
//		versionId = null;
//	}

	public void setContextQualifierLength(int l) {
		contextQualifierLength = l;
	}
	
	public int getContextQualifierLength(){
		return contextQualifierLength;
	}

	public void setSite(BuildTimeSite site) {
		this.site = site;
	}

	public BuildTimeSite getSite() {
		return site;
	}
	
	public void setFeatureContentProvider(BuildTimeSiteContentProvider contentProvider) {
		this.contentProvider = contentProvider;
	}

	public BuildTimeSiteContentProvider getFeatureContentProvider() {
		return contentProvider;
	}

	public URL getURL() {
		return url;
	}
	
	public void setURL(URL url) {
		this.url = url;
	}
	
	public String getRootLocation() {
		if (rootLocation == null) {
			URL location = getURL();
			if (location == null)
				return null;
			rootLocation = location.getPath();
			int i = rootLocation.lastIndexOf(Constants.FEATURE_FILENAME_DESCRIPTOR);
			if (i != -1)
				rootLocation = rootLocation.substring(0, i);
		}
		return rootLocation;
	}
}
