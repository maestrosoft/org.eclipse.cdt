package org.eclipse.cdt.managedbuilder.internal.core;

/**********************************************************************
 * Copyright (c) 2002,2003 Rational Software Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors: 
 * IBM Rational Software - Initial API and implementation
 * **********************************************************************/

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.CommandLauncher;
import org.eclipse.cdt.core.ConsoleOutputStream;
import org.eclipse.cdt.core.ErrorParserManager;
import org.eclipse.cdt.core.model.ICModelMarker;
import org.eclipse.cdt.core.resources.ACBuilder;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.core.ManagedBuilderCorePlugin;
import org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderDependencyCalculator;
import org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;

public class GeneratedMakefileBuilder extends ACBuilder {
	// String constants
	private static final String MESSAGE = "ManagedMakeBuilder.message";	//$NON-NLS-1$
	private static final String BUILD_ERROR = MESSAGE + ".error";	//$NON-NLS-1$
	private static final String REFRESH_ERROR = BUILD_ERROR + ".refresh";	//$NON-NLS-1$
	private static final String BUILD_FINISHED = MESSAGE + ".finished";	//$NON-NLS-1$
	private static final String INCREMENTAL = MESSAGE + ".incremental";	//$NON-NLS-1$
	private static final String MAKE = MESSAGE + ".make";	//$NON-NLS-1$
	private static final String REBUILD = MESSAGE + ".rebuild";	//$NON-NLS-1$
	private static final String START = MESSAGE + ".starting";	//$NON-NLS-1$
	private static final String REFRESH = MESSAGE + ".updating";	//$NON-NLS-1$
	private static final String MARKERS = MESSAGE + ".creating.markers";	//$NON-NLS-1$
	private static final String CONSOLE_HEADER = MESSAGE + ".console.header";	//$NON-NLS-1$
	private static final String TYPE_FULL = "ManagedMakeBuilder.type.full";	//$NON-NLS-1$
	private static final String TYPE_INC = "ManagedMakeBuider.type.incremental";	//$NON-NLS-1$

	// Local variables
	protected List resourcesToBuild;
	protected List ruleList;
	protected IManagedBuilderMakefileGenerator generator;
	
	public class ResourceDeltaVisitor implements IResourceDeltaVisitor {
		private boolean buildNeeded = false;

		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			// If the project has changed, then a build is needed and we can stop
			if (resource != null && resource.getProject() == getProject()) {
				buildNeeded = true;
				return false;
			}

			return true;
		}

		public boolean shouldBuild() {
			return buildNeeded;
		}
	}

	/**
	 * 
	 */
	public GeneratedMakefileBuilder() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.internal.events.InternalBuilder#build(int, java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
		String statusMsg = ManagedMakeMessages.getFormattedString(START, getProject().getName());
		IProject[] deps = getProject().getReferencedProjects();
		if (statusMsg != null) {
			monitor.subTask(statusMsg);
		}
		
		// Get the build information
		IManagedBuildInfo info = ManagedBuildManager.getBuildInfo(getProject());
		if (info == null) {
			return deps;
		}

		if (kind == IncrementalProjectBuilder.FULL_BUILD || info.isDirty()) {
			fullBuild(monitor, info);
		}
		else if (kind == IncrementalProjectBuilder.AUTO_BUILD && info.isDirty()) {
			fullBuild(monitor, info);
		}
		else {
			// Create a delta visitor to make sure we should be rebuilding
			ResourceDeltaVisitor visitor = new ResourceDeltaVisitor();
			IResourceDelta delta = getDelta(getProject());
			if (delta == null) {
				fullBuild(monitor, info);
			}
			else {
				delta.accept(visitor);
				if (visitor.shouldBuild()) {
					incrementalBuild(delta, info, monitor);
				}
			}
		}
		
		// Scrub the build info of all the projects participating in the build
		info.setDirty(false);
		for (int i = 0; i < deps.length; i++) {
			IProject project = deps[i];
			IManagedBuildInfo depInfo = ManagedBuildManager.getBuildInfo(project);
			// May not be a managed project 
			if (depInfo != null) {
				depInfo.setDirty(false);
			}
		} 
		
		// Ask build mechanism to compute deltas for project dependencies next time
		return deps;
	}

	/**
	 * Check whether the build has been canceled. Cancellation requests 
	 * propagated to the caller by throwing <code>OperationCanceledException</code>.
	 * 
	 * @see org.eclipse.core.runtime.OperationCanceledException#OperationCanceledException()
	 */
	public void checkCancel(IProgressMonitor monitor) {
		if (monitor != null && monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
	}

	/**
	 * @param monitor
	 */
	protected void fullBuild(IProgressMonitor monitor, IManagedBuildInfo info) throws CoreException {
		// Always need one of these bad boys
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		
		// Regenerate the makefiles for any managed projects this project depends on
		IProject[] deps = getProject().getReferencedProjects();
		for (int i = 0; i < deps.length; i++) {
			IProject depProject = deps[i];
			if (ManagedBuildManager.manages(depProject)) {
				IManagedBuildInfo depInfo = ManagedBuildManager.getBuildInfo(depProject);
				String targetID = depInfo.getDefaultTarget().getParent().getId();
				IManagedBuilderMakefileGenerator generator = ManagedBuildManager.getMakefileGenerator(targetID);
				generator.initialize(depProject, depInfo, monitor);
				try {
					generator.regenerateMakefiles();		
				} catch (CoreException e) {
					// Throw the exception back to the builder
					throw e;
				}
			}
		}

		// Need to report status to the user
		String statusMsg = ManagedMakeMessages.getFormattedString(REBUILD, getProject().getName());
		monitor.subTask(statusMsg);

		// Regenerate the makefiles for this project
		String targetID = info.getDefaultTarget().getParent().getId();
		generator = ManagedBuildManager.getMakefileGenerator(targetID);
		generator.initialize(getProject(), info, monitor);
		try {
			generator.regenerateMakefiles();
		} catch (CoreException e) {
			// Throw the exception back to the builder
			throw e;
		}
		
		// Now call make
		IPath topBuildDir = generator.getBuildWorkingDir();
		if (topBuildDir != null) {
			invokeMake(true, topBuildDir, info, monitor);
		} else {
			monitor.done();
			return;
		}
		
		// Now regenerate the dependencies
		try {
			generator.regenerateDependencies(false);
		} catch (CoreException e) {
			// Throw the exception back to the builder
			throw e;
		}
		
	}

	/**
	 * @param toolId
	 * @return
	 */
	public IManagedBuilderDependencyCalculator getDependencyCalculator(String toolId) {
		try {
			IExtensionPoint extension = Platform.getExtensionRegistry().getExtensionPoint(ManagedBuilderCorePlugin.getUniqueIdentifier(), ManagedBuilderCorePlugin.DEP_CALC_ID);
			if (extension != null) {
				// There could be many of these
				IExtension[] extensions = extension.getExtensions();
				for (int i = 0; i < extensions.length; i++) {
					IConfigurationElement[] configElements = extensions[i].getConfigurationElements();
					for (int j = 0; j < configElements.length; j++) {
						IConfigurationElement element = configElements[j];
						if (element.getName().equals(ITool.TOOL_ELEMENT_NAME)) { 
							if (element.getAttribute(ITool.ID).equals(toolId)) {
								if (element.getAttribute(ManagedBuilderCorePlugin.DEP_CALC_ID) != null) {
									return (IManagedBuilderDependencyCalculator) element.createExecutableExtension(ManagedBuilderCorePlugin.DEP_CALC_ID);
								}
							}
						}
					}
				}
			}
		} 
		catch (CoreException e) {
			// Probably not defined
		}
		return null;
	}
	/* (non-javadoc)
	 * Answers an array of strings with the proper make targets
	 * 
	 * @param fullBuild
	 * @return
	 */
	protected String[] getMakeTargets(boolean fullBuild) {
		List args = new ArrayList();
		if (fullBuild) {
			args.add("clean"); //$NON-NLS-1$
		}
		args.add("all"); //$NON-NLS-1$
		return (String[])args.toArray(new String[args.size()]);
	}
	
	/**
	 * @return
	 */
	protected List getResourcesToBuild() {
		if (resourcesToBuild == null) {
			resourcesToBuild = new ArrayList();
		}
		return resourcesToBuild;
	}

	/* (non-javadoc)
	 * Answers the list of build rules that have been assembled. If there are none, 
	 * answers an empty list, never <code>null</code>
	 * 
	 * @return
	 */
	protected List getRuleList() {
		if (ruleList == null) {
			ruleList = new ArrayList();
		}
		return ruleList;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.resources.ACBuilder#getWorkingDirectory()
	 */
	public IPath getWorkingDirectory() {
		return getProject().getLocation();
	}

	/* (non-Javadoc)
	 * @param delta
	 * @param info
	 * @param monitor
	 * @throws CoreException
	 */
	protected void incrementalBuild(IResourceDelta delta, IManagedBuildInfo info, IProgressMonitor monitor) throws CoreException {
		// Rebuild the resource tree in the delta
		String statusMsg = null;
		
		// Need to report status to the user
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		statusMsg = ManagedMakeMessages.getFormattedString(INCREMENTAL, getProject().getName());
		monitor.subTask(statusMsg);
		
		// Regenerate the makefiles for any managed projects this project depends on
		IProject[] deps = getProject().getReferencedProjects();
		for (int i = 0; i < deps.length; i++) {
			IProject depProject = deps[i];
			if (ManagedBuildManager.manages(depProject)) {
				IManagedBuildInfo depInfo = ManagedBuildManager.getBuildInfo(depProject);
				String targetID = depInfo.getDefaultTarget().getParent().getId();
				IManagedBuilderMakefileGenerator generator = ManagedBuildManager.getMakefileGenerator(targetID);
				generator.initialize(depProject, depInfo, monitor);	
				try {
					generator.regenerateMakefiles();		
				} catch (CoreException e) {
					// Throw the exception back to the builder
					ManagedBuilderCorePlugin.log(e);
					throw e;
				}
			}
		}

		// Ask the makefile generator to generate any makefiles needed to build delta
		String targetID = info.getDefaultTarget().getParent().getId();
		generator = ManagedBuildManager.getMakefileGenerator(targetID);
		generator.initialize(getProject(), info, monitor);
		try {
			generator.generateMakefiles(delta);
		} catch (CoreException e) {
			// Throw the exception back to the builder
			ManagedBuilderCorePlugin.log(e);
			throw e;
		}	

		// Run the build
		IPath buildDir = new Path(info.getConfigurationName());
		if (buildDir != null) {
			invokeMake(false, buildDir, info, monitor);
		} else {
			monitor.done();
			return;
		}
		
		// Generate the dependencies for all changes
		try {
			generator.generateDependencies();
		} catch (CoreException e) {
			ManagedBuilderCorePlugin.log(e);
			throw e;
		}
	}

	/* (non-Javadoc)
	 * @param fullBuild
	 * @param buildDir
	 * @param info
	 * @param monitor
	 */
	protected void invokeMake(boolean fullBuild, IPath buildDir, IManagedBuildInfo info, IProgressMonitor monitor) {
		// Get the project and make sure there's a monitor to cancel the build
		IProject currentProject = getProject();
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}

		try {
			// Figure out the working directory for the build and make sure there is a makefile there 
			IPath workingDirectory = getWorkingDirectory().append(buildDir);
			IWorkspace workspace = currentProject.getWorkspace();
			if (workspace == null) {
				return;
			}
			IWorkspaceRoot root = workspace.getRoot();
			if (root == null) {
				return;
			}
			IPath makefile = workingDirectory.addTrailingSeparator().append(generator.getMakefileName());
			if (root.getFileForLocation(makefile) == null) {
				return; 
			}

			// Flag to the user that make is about to be called
			IPath makeCommand = new Path(info.getMakeCommand()); 
			if (makeCommand != null) {
				String[] msgs = new String[2];
				msgs[0] = makeCommand.toString();
				msgs[1] = currentProject.getName();
				monitor.beginTask(ManagedMakeMessages.getFormattedString(MAKE, msgs), IProgressMonitor.UNKNOWN);

				// Get a build console for the project
				StringBuffer buf = new StringBuffer();
				IConsole console = CCorePlugin.getDefault().getConsole();
				console.start(currentProject);
				ConsoleOutputStream consoleOutStream = console.getOutputStream();
				String[] consoleHeader = new String[3];
				consoleHeader[0] = fullBuild ? 
						ManagedMakeMessages.getResourceString(TYPE_FULL) : 
						ManagedMakeMessages.getResourceString(TYPE_INC);
				consoleHeader[1] = info.getConfigurationName();
				consoleHeader[2] = currentProject.getName();
				buf.append(System.getProperty("line.separator", "\n"));	//$NON-NLS-1$	//$NON-NLS-2$
				buf.append(ManagedMakeMessages.getFormattedString(CONSOLE_HEADER, consoleHeader));
				buf.append(System.getProperty("line.separator", "\n"));	//$NON-NLS-1$	//$NON-NLS-2$
				buf.append(System.getProperty("line.separator", "\n"));	//$NON-NLS-1$	//$NON-NLS-2$
				consoleOutStream.write(buf.toString().getBytes());
				consoleOutStream.flush();
				
				// Remove all markers for this project
				removeAllMarkers(currentProject);
				IProject[] deps = currentProject.getReferencedProjects();
				for (int i = 0; i < deps.length; i++) {
					IProject project = deps[i];
					removeAllMarkers(project);
				} 

				// Get the arguments to be passed to make from build model
				ArrayList makeArgs = new ArrayList();
				String arg = info.getMakeArguments();
				if (arg.length() > 0) {
					String[] args = arg.split("\\s"); //$NON-NLS-1$
					for (int i = 0; i < args.length; ++i) {
						makeArgs.add(args[i]);
					}
				}
				makeArgs.addAll(Arrays.asList(getMakeTargets(fullBuild)));
				String[] makeTargets = (String[]) makeArgs.toArray(new String[makeArgs.size()]);
			
				// Get a launcher for the make command
				String errMsg = null;
				CommandLauncher launcher = new CommandLauncher();
				launcher.showCommand(true);
	
				// Set the environmennt, some scripts may need the CWD var to be set.
				Properties props = launcher.getEnvironment();
				props.put("CWD", workingDirectory.toOSString());	//$NON-NLS-1$
				props.put("PWD", workingDirectory.toOSString());	//$NON-NLS-1$
				String[] env = null;
				ArrayList envList = new ArrayList();
				Enumeration names = props.propertyNames();
				if (names != null) {
					while (names.hasMoreElements()) {
						String key = (String) names.nextElement();
						envList.add(key + "=" + props.getProperty(key)); //$NON-NLS-1$
					}
					env = (String[]) envList.toArray(new String[envList.size()]);
				}
			
				// Hook up an error parser manager
				String[] errorParsers = info.getDefaultTarget().getErrorParserList(); 
				ErrorParserManager epm = new ErrorParserManager(getProject(), workingDirectory, this, errorParsers);
				epm.setOutputStream(consoleOutStream);
				OutputStream stdout = epm.getOutputStream();
				OutputStream stderr = epm.getOutputStream();
			
				// Launch make
				Process proc = launcher.execute(makeCommand, makeTargets, env, workingDirectory);
				if (proc != null) {
					try {
						// Close the input of the process since we will never write to it
						proc.getOutputStream().close();
					} catch (IOException e) {
					}
				
					if (launcher.waitAndRead(stdout, stderr, new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN)) != CommandLauncher.OK) { 
						errMsg = launcher.getErrorMessage();
					}
				
					// Force a resync of the projects without allowing the user to cancel.
					// This is probably unkind, but short of this there is no way to insure
					// the UI is up-to-date with the build results
					monitor.subTask(ManagedMakeMessages.getResourceString(REFRESH));
					try {
						currentProject.refreshLocal(IResource.DEPTH_INFINITE, null);
						for (int j = 0; j < deps.length; ++j) {
							IProject project = deps[j];
							project.refreshLocal(IResource.DEPTH_INFINITE, null);
						}
					} catch (CoreException e) {
						monitor.subTask(ManagedMakeMessages.getResourceString(REFRESH_ERROR));
					}
				} else {
					errMsg = launcher.getErrorMessage();
				}
				
				// Report either the success or failure of our mission
				buf = new StringBuffer();
				if (errMsg != null && errMsg.length() > 0) {
					String errorDesc = ManagedMakeMessages.getResourceString(BUILD_ERROR);
					buf.append(errorDesc);
					buf.append(System.getProperty("line.separator", "\n"));  //$NON-NLS-1$//$NON-NLS-2$
					buf.append("(").append(errMsg).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					// Report a successful build
					String successMsg = ManagedMakeMessages.getFormattedString(BUILD_FINISHED, currentProject.getName());
					buf.append(successMsg);
					buf.append(System.getProperty("line.separator", "\n"));  //$NON-NLS-1$//$NON-NLS-2$
				}

				// Write message on the console
				consoleOutStream.write(buf.toString().getBytes());
				consoleOutStream.flush();
				stdout.close();
				stderr.close();				

				monitor.subTask(ManagedMakeMessages.getResourceString(MARKERS));
				epm.reportProblems();
			}
		} catch (Exception e) {
			CCorePlugin.log(e);
			forgetLastBuiltState();
		} finally {
			monitor.done();
		}
	}

	private void removeAllMarkers(IProject project) throws CoreException {
		IWorkspace workspace = project.getWorkspace();

		// remove all markers
		IMarker[] markers = project.findMarkers(ICModelMarker.C_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
		if (markers != null) {
			workspace.deleteMarkers(markers);
		}
	}

}
