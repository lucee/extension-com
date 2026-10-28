package org.lucee.extension.com.functions;

import org.lucee.extension.com.COMObject;

import lucee.runtime.PageContext;
import lucee.runtime.exp.PageException;

/**
 * releaseComObject(obj): releases a COM object created with createComObject.
 */
public class ReleaseComObject extends FunctionSupport {

	private static final long serialVersionUID = 1L;

	@Override
	public Object invoke(PageContext pc, Object[] args) throws PageException {
		if (args.length != 1) {
			throw exp.createFunctionException(pc, "ReleaseComObject", 1, 1, args.length);
		}
		return call(pc, args[0]);
	}

	public static Object call(PageContext pc, Object obj) {
		if (obj instanceof COMObject) ((COMObject) obj).release();
		return null;
	}
}
