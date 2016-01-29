/*******************************************************************************
 * Copyright (c) 2006 Intel Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Intel Corporation - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.managedbuilder.internal.buildmodel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.cdt.managedbuilder.buildmodel.IBuildIOType;
import org.eclipse.cdt.managedbuilder.buildmodel.IBuildResource;
import org.eclipse.cdt.managedbuilder.buildmodel.IBuildStep;
import org.eclipse.cdt.managedbuilder.core.IBuildObject;
import org.eclipse.cdt.managedbuilder.core.IInputOrder;
import org.eclipse.cdt.managedbuilder.core.IInputType;
import org.eclipse.cdt.managedbuilder.core.IOutputType;
import org.eclipse.core.runtime.IPath;

public class BuildIOType implements IBuildIOType {
	private BuildStep fStep;
	private BuildResourceComparator resComparator = new BuildResourceComparator();
	private List<BuildResource> fResources = new ArrayList<BuildResource>();
	private SortedSet<BuildResource> fSortedResources = new TreeSet<BuildResource>(resComparator);
	private boolean fIsInput;
	private boolean fIsPrimary;
	private String fLinkId;
	private IBuildObject fIoType;
	
	private class BuildResourceComparator implements Comparator<BuildResource> {
		
		public int getOrder(BuildResource res) {
			
			if(!(fIoType instanceof IInputType)) {
				throw new IllegalArgumentException("BuildIOType not instanceof IInputType");
			}
			int orderVal = 0;
			IInputType inType = ((IInputType)fIoType);
			
			do {
				IInputOrder orderRes = inType.getInputOrder(getPath(res).toOSString());
				if(orderRes != null) {
					orderVal = (new Integer(orderRes.getOrder())).intValue();
					if(orderVal < 1) {
						throw new IllegalArgumentException("InputOrder value less than 1");
					}
					break;
				}
				// May be the superclass does have an input order for this resource
				inType = inType.getSuperClass();
			} 
			while (inType != null);

			return orderVal;
		}

		@Override
      public int compare(BuildResource res1, BuildResource res2) {
	      return getOrder(res1) - getOrder(res2);
      }

	}
	

	public BuildIOType(BuildStep action, boolean input, boolean primary,/* BuildPattern pattern,*/ IBuildObject ioType ) {
		fStep = action;
		fIsInput = input;
		fIsPrimary = primary;
		if(ioType != null){
			if(input){
				if(ioType instanceof IInputType)
					fLinkId = ((IInputType)ioType).getBuildVariable();
				else
					throw new IllegalArgumentException("Unsupported IBuildObject");	//$NON-NLS-1$
			} else {
				if(ioType instanceof IOutputType) {
					fLinkId = ((IOutputType)ioType).getBuildVariable();
				} else
					throw new IllegalArgumentException("Unsupported IBuildObject");	//$NON-NLS-1$
			}
			fIoType = ioType;
		} else {
			//TODO
		}
		if(fStep != null) {
			((BuildDescription)fStep.getBuildDescription()).typeCreated(this);

			if(((DbgUtil.DEBUG & DbgUtil.BUILD_IO_TYPE) != 0))
				DbgUtil.trace("BuildIOType() " + (fIsInput ? "- InputType is " : "- OutputType is ") + ((ioType != null) ? ioType.getName() : "unknow")+ " of action " + DbgUtil.stepName(fStep) + " created");	//$NON-NLS-1$
		}
	}

	@Override
	public IBuildResource[] getResources() {
		
		BuildResource sorted[] = fSortedResources.toArray(new BuildResource[fSortedResources.size()]);
		BuildResource unsorted[] = fResources.toArray(new BuildResource[fResources.size()]);
		
	   //System.arraycopy(sorted, 0, allResource, 0, sorted.length);
	   //System.arraycopy(unsorted, 0, allResource, sorted.length, unsorted.length);
		//return allResource;
		
		int allSize = unsorted.length + sorted.length;
		BuildResource[] allResource = new BuildResource[allSize];
		
		if(allSize == 0) {
			return allResource;
		}
		// The order attribute must be an integer such that 0 < order <= Integer.MAX_VALUE
		int curIndex = 1;
		int sortIndex = 0;
		int sortedOrder = 0;
		int unsortIndex = 0;
		
		boolean unsortEmpty = (unsorted.length == 0);

		// Any hole between order value of sorted is filled with unsorted resources. This would
		// allow an arbitrary very high order value of a sorted resource to have it tool processing last.
		if(sorted.length != 0) {
			sortedOrder = resComparator.getOrder(sorted[sortIndex]);
		}
		do {
			if(curIndex == sortedOrder || unsortEmpty) {
				allResource[curIndex-1] = sorted[sortIndex++];
				
				if(((DbgUtil.DEBUG & DbgUtil.BUILD_IO_TYPE) != 0) && fStep != null)
					DbgUtil.trace("BuildIOType.getResources() - BuildResource (sorted) index = " + (curIndex-1) + " "  + DbgUtil.resourceName(allResource[curIndex-1]) + " returned as "  	//$NON-NLS-1$	//$NON-NLS-2$
							+ (fIsInput ? "input" : "output")	//$NON-NLS-1$	//$NON-NLS-2$
							+ " to the action " + DbgUtil.stepName(fStep));	//$NON-NLS-1$
				
				if(sortIndex < sorted.length) {
					sortedOrder = resComparator.getOrder(sorted[sortIndex]);
				}
				continue;
			}
			allResource[curIndex-1] = unsorted[unsortIndex++];
			
			if(((DbgUtil.DEBUG & DbgUtil.BUILD_IO_TYPE) != 0) && fStep != null)
				DbgUtil.trace("BuildIOType.getResources() - BuildResource (unsorted) index = " + (curIndex-1) + " " + DbgUtil.resourceName(allResource[curIndex-1]) + " returned as "  	//$NON-NLS-1$	//$NON-NLS-2$
						+ (fIsInput ? "input" : "output")	//$NON-NLS-1$	//$NON-NLS-2$
						+ " to the action " + DbgUtil.stepName(fStep));	//$NON-NLS-1$
			
			if(unsortIndex == unsorted.length) {
				unsortEmpty = true;
			}
		} 
		while ( ++curIndex <= allSize);
		
	   return allResource;
	}


	@Override
	public IBuildStep getStep() {
		return fStep;
	}

	public void addResource(BuildResource rc){

		if(fResources.size() != 0 ) {
			int test = fResources.size();
		}
		boolean sorted = false;
		IPath path = getPath(rc);
		
		if(fIoType != null){
			if(fIsInput){
				IInputType inType = ((IInputType)fIoType);
				do {
					IInputOrder inOrder = inType.getInputOrder(path.toOSString());
					if(inOrder != null) {
						fSortedResources.add(rc);
						sorted = true;							
						break;
					}
					// May be the superclass does have an input order for this resource
					inType = inType.getSuperClass();
				} 
				while (inType != null);
			}
		}
		if(!sorted) {
			fResources.add(rc);
		}
		rc.addToArg(this);
		if(((DbgUtil.DEBUG & DbgUtil.BUILD_IO_TYPE) != 0) && fStep != null)
			DbgUtil.trace("BuildIOType.addResource() - BuildResource " + DbgUtil.resourceName(rc) + " added as "  	//$NON-NLS-1$	//$NON-NLS-2$
					+ (fIsInput ? "input" : "output")	//$NON-NLS-1$	//$NON-NLS-2$
					+ " to the action " + DbgUtil.stepName(fStep) + ", fResources.size() = " + fResources.size() + ", fSortedResources.size() = " + fSortedResources.size() );	//$NON-NLS-1$

		if(fStep!= null) {
			((BuildDescription)fStep.getBuildDescription()).resourceAddedToType(this, rc);
		}
	}

	public void removeResource(BuildResource rc){
		
		if(!fResources.remove(rc)) {
			if(!fSortedResources.remove(rc)) {
				throw new IllegalArgumentException();
			}
		}
		rc.removeFromArg(this);

		if(((DbgUtil.DEBUG & DbgUtil.BUILD_IO_TYPE) != 0))
			DbgUtil.trace("BuildIOType.removeResource() - resource " + DbgUtil.resourceName(rc) + " removed as "  	//$NON-NLS-1$	//$NON-NLS-2$
					+ (fIsInput ? "input" : "output")	//$NON-NLS-1$	//$NON-NLS-2$
					+ " from the action " + DbgUtil.stepName(fStep));	//$NON-NLS-1$

		((BuildDescription)fStep.getBuildDescription()).resourceRemovedFromType(this, rc);
	}


	@Override
	public boolean isInput() {
		return fIsInput;
	}

	public boolean isPrimary(){
		return fIsPrimary;
	}

	public String getLinkId(){
		if(!fIsInput && fStep.getTool() != null && /*(fLinkId == null || fLinkId.length() == 0) && */
				fStep.getTool().getCustomBuildStep()){
			IBuildResource rcs[] = getResources();
			if(rcs.length != 0){
				BuildDescription.ToolAndType tt = ((BuildDescription)fStep.getBuildDescription()).getToolAndType((BuildResource)rcs[0], false);
				if(tt != null){
					IInputType type = tt.fTool.getPrimaryInputType();
					if(type != null)
						fLinkId = type.getBuildVariable();
				} else {
				}
			}

		}
		return fLinkId;
	}

	public IBuildObject getIoType(){
		return fIoType;
	}

	BuildResource[] remove(){
		BuildResource rcs[] = (BuildResource[])getResources();

		for(int i = 0; i < rcs.length; i++){
			removeResource(rcs[i]);
		}

		fStep = null;
		return rcs;
	}
	
	private IPath getPath(BuildResource rc) {
		
		IPath path = rc.getFullPath();
		// If this is a WS path then return relative path to WS
		if(path != null) {
			if(rc.isProjectResource()) {
				path = path.removeFirstSegments(1).makeRelative();
			}
		}
		else {
			// Otherwise return absolute path location
			path = rc.getLocation();
		}
		return path;
	}

}
