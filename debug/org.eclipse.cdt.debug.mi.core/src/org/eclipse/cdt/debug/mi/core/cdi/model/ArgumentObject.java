/*
 *(c) Copyright QNX Software Systems Ltd. 2002.
 * All Rights Reserved.
 * 
 */

package org.eclipse.cdt.debug.mi.core.cdi.model;

import org.eclipse.cdt.debug.core.cdi.model.ICDIArgumentObject;
import org.eclipse.cdt.debug.core.cdi.model.ICDIStackFrame;
import org.eclipse.cdt.debug.core.cdi.model.ICDITarget;

/**
 */
public class ArgumentObject extends VariableObject implements ICDIArgumentObject {

	public ArgumentObject(ICDITarget target, String name, ICDIStackFrame frame, int pos, int depth) {
		super(target, name, frame, pos, depth);
	}

}
