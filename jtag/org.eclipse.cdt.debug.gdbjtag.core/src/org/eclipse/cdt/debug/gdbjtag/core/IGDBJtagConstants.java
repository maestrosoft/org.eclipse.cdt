/*******************************************************************************
 * Copyright (c) 2007 - 2008 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *     Andy Jin - Hardware debugging UI improvements, bug 229946
 *******************************************************************************/

package org.eclipse.cdt.debug.gdbjtag.core;

/**
 * @author Doug Schaefer
 *
 */
public interface IGDBJtagConstants {
	
	public static final String DEBUGGER_ID = "org.eclipse.cdt.debug.mi.core.CDebuggerNew"; //$NON-NLS-1$
	
	// Debugger
	public static final String ATTR_USE_REMOTE_TARGET = Activator.PLUGIN_ID + ".useRemoteTarget"; //$NON-NLS-1$
	public static final String ATTR_IP_ADDRESS = Activator.PLUGIN_ID + ".ipAddress"; //$NON-NLS-1$
	public static final String ATTR_PORT_NUMBER = Activator.PLUGIN_ID + ".portNumber"; //$NON-NLS-1$
	public static final String ATTR_JTAG_DEVICE = Activator.PLUGIN_ID + ".jtagDevice"; //$NON-NLS-1$
	
	public static final boolean DEFAULT_USE_REMOTE_TARGET = true;
	public static final String DEFAULT_IP_ADDRESS = "localhost"; //$NON-NLS-1$
	public static final int DEFAULT_PORT_NUMBER = 10000;
	
	// Startup
	public static final String ATTR_INIT_COMMANDS = Activator.PLUGIN_ID + ".initCommands"; //$NON-NLS-1$
	public static final String ATTR_DELAY = Activator.PLUGIN_ID + ".delay"; //$NON-NLS-1$
	public static final String ATTR_DO_RESET = Activator.PLUGIN_ID + ".doReset"; //$NON-NLS-1$
	public static final String ATTR_LOAD_IMAGE = Activator.PLUGIN_ID + ".loadImage"; //$NON-NLS-1$
	public static final String ATTR_LOAD_SYMBOLS = Activator.PLUGIN_ID + ".loadSymbols"; //$NON-NLS-1$
	public static final String ATTR_IMAGE_FILE_NAME = Activator.PLUGIN_ID + ".imageFileName"; //$NON-NLS-1$
	public static final String ATTR_SYMBOLS_FILE_NAME = Activator.PLUGIN_ID + ".symbolsFileName"; //$NON-NLS-1$
	public static final String ATTR_IMAGE_OFFSET = Activator.PLUGIN_ID + ".imageOffset"; //$NON-NLS-1$
	public static final String ATTR_SYMBOLS_OFFSET = Activator.PLUGIN_ID + ".symbolsOffset"; //$NON-NLS-1$
	public static final String ATTR_SET_PC_REGISTER = Activator.PLUGIN_ID + ".setPcRegister"; //$NON-NLS-1$
	public static final String ATTR_PC_REGISTER = Activator.PLUGIN_ID + ".pcRegister"; //$NON-NLS-1$
	public static final String ATTR_SET_STOP_AT = Activator.PLUGIN_ID + ".setStopAt"; //$NON-NLS-1$
	public static final String ATTR_STOP_AT = Activator.PLUGIN_ID + ".stopAt"; //$NON-NLS-1$
	public static final String ATTR_SET_RESUME = Activator.PLUGIN_ID + ".setResume"; //$NON-NLS-1$
	public static final String ATTR_RUN_COMMANDS = Activator.PLUGIN_ID + ".runCommands"; //$NON-NLS-1$
	
	public static final boolean DEFAULT_DO_RESET = true;
	public static final int DEFAULT_DELAY = 3;
	public static final boolean DEFAULT_LOAD_IMAGE = false;
	public static final boolean DEFAULT_LOAD_SYMBOLS = false;
	public static final boolean DEFAULT_SET_PC_REGISTER = false;
	public static final boolean DEFAULT_SET_STOP_AT = false;
	public static final boolean DEFAULT_SET_RESUME = false;
	public static final boolean DEFAULT_USE_DEFAULT_RUN = true;
		
}
