package org.lucee.extension.com.functions;

import org.lucee.extension.com.COMObject;

import lucee.runtime.PageContext;
import lucee.runtime.exp.PageException;

/**
 * createComObject(className): creates a COM/ActiveX object, replaces createObject("com", className) and
 * &lt;cfobject type="com"&gt; of Lucee 8 and older.
 */
public class CreateComObject extends FunctionSupport {

	private static final long serialVersionUID = 1L;

	@Override
	public Object invoke(PageContext pc, Object[] args) throws PageException {
		if (args.length != 1) {
			throw exp.createFunctionException(pc, "CreateComObject", 1, 1, args.length);
		}
		return call(pc, cast.toString(args[0]));
	}

	public static Object call(PageContext pc, String className) throws PageException {
		if (className == null || className.trim().isEmpty()) throw exp.createFunctionException(pc, "CreateComObject", 1, "className", "must not be empty", "");
		return new COMObject(pc, className.trim());
	}
}
