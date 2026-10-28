package org.lucee.extension.com;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.jacob.com.Dispatch;
import com.jacob.com.Variant;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.PageContext;
import lucee.runtime.dump.DumpData;
import lucee.runtime.dump.DumpProperties;
import lucee.runtime.dump.DumpTable;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Collection;
import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.Iteratorable;
import lucee.runtime.type.Objects;
import lucee.runtime.type.Struct;
import lucee.runtime.type.dt.DateTime;

/**
 * A COM/ActiveX object, created with createComObject(className). Properties and methods are called like on any other object.
 */
public final class COMObject implements Objects, Iteratorable {

	private static final long serialVersionUID = 1L;

	private static final CFMLEngine eng = CFMLEngineFactory.getInstance();

	private final String name;
	private transient Dispatch dispatch;
	private transient Variant parent;

	/**
	 * creates a new COM object, loads the native library of Jacob when necessary
	 * 
	 * @param pc page context
	 * @param name class name (ProgID) of the COM object, for example "Scripting.FileSystemObject"
	 */
	public COMObject(PageContext pc, String name) throws PageException {
		DllSetup.setup(pc);
		this.name = name;
		this.dispatch = new Dispatch(name);
	}

	/**
	 * sub objects, returned by a property or a method
	 */
	COMObject(Variant parent, Dispatch dispatch, String name) {
		this.parent = parent;
		this.name = name;
		this.dispatch = dispatch;
	}

	@Override
	public Object get(PageContext pc, Collection.Key key) throws PageException {
		return COMUtil.toObject(this, Dispatch.call(dispatch, key.getString()), key.getString());
	}

	@Override
	public Object get(PageContext pc, Collection.Key key, Object defaultValue) {
		return COMUtil.toObject(this, Dispatch.call(dispatch, key.getString()), key.getString(), defaultValue);
	}

	@Override
	public Object set(PageContext pc, Collection.Key propertyName, Object value) throws PageException {
		Dispatch.put(dispatch, propertyName.getString(), unwrap(value));
		return value;
	}

	@Override
	public Object setEL(PageContext pc, Collection.Key propertyName, Object value) {
		Dispatch.put(dispatch, propertyName.getString(), unwrap(value));
		return value;
	}

	@Override
	public Object call(PageContext pc, Collection.Key key, Object[] args) throws PageException {
		String methodName = key.getString();
		Object[] arr = new Object[args.length];
		for (int i = 0; i < args.length; i++) {
			arr[i] = unwrap(args[i]);
		}
		return COMUtil.toObject(this, Dispatch.callN(dispatch, methodName, arr), methodName);
	}

	@Override
	public Object callWithNamedValues(PageContext pc, Collection.Key key, Struct args) throws PageException {
		Iterator<Object> it = args.valueIterator();
		List<Object> values = new ArrayList<Object>();
		while (it.hasNext()) {
			values.add(it.next());
		}
		return call(pc, key, values.toArray(new Object[values.size()]));
	}

	private static Object unwrap(Object value) {
		return value instanceof COMObject ? ((COMObject) value).dispatch : value;
	}

	@Override
	public DumpData toDumpData(PageContext pageContext, int maxlevel, DumpProperties dp) {
		DumpTable table = new DumpTable("com", "#ff3300", "#ff9966", "#660000");
		table.appendRow(1, simpleDumpData("COM Object"), simpleDumpData(name));
		return table;
	}

	// SimpleDumpData is a class of the Lucee core and not part of the loader API
	private static DumpData simpleDumpData(String str) {
		try {
			Class<?> clazz = eng.getClassUtil().loadClass("lucee.runtime.dump.SimpleDumpData");
			return (DumpData) clazz.getConstructor(String.class).newInstance(str);
		}
		catch (Exception e) {
			throw eng.getExceptionUtil().createPageRuntimeException(eng.getCastUtil().toPageException(e));
		}
	}

	@Override
	public String castToString() throws PageException {
		throw eng.getExceptionUtil().createExpressionException("can't cast Com Object to a String");
	}

	@Override
	public String castToString(String defaultValue) {
		return defaultValue;
	}

	@Override
	public boolean castToBooleanValue() throws PageException {
		throw eng.getExceptionUtil().createExpressionException("can't cast Com Object to a boolean value");
	}

	@Override
	public Boolean castToBoolean(Boolean defaultValue) {
		return defaultValue;
	}

	@Override
	public double castToDoubleValue() throws PageException {
		throw eng.getExceptionUtil().createExpressionException("can't cast Com Object to a number");
	}

	@Override
	public double castToDoubleValue(double defaultValue) {
		return defaultValue;
	}

	@Override
	public DateTime castToDateTime() throws PageException {
		throw eng.getExceptionUtil().createExpressionException("can't cast Com Object to a Date");
	}

	@Override
	public DateTime castToDateTime(DateTime defaultValue) {
		return defaultValue;
	}

	@Override
	public int compareTo(boolean b) throws PageException {
		throw eng.getExceptionUtil().createExpressionException("can't compare Com Object with a boolean value");
	}

	@Override
	public int compareTo(DateTime dt) throws PageException {
		throw eng.getExceptionUtil().createExpressionException("can't compare Com Object with a DateTime Object");
	}

	@Override
	public int compareTo(double d) throws PageException {
		throw eng.getExceptionUtil().createExpressionException("can't compare Com Object with a numeric value");
	}

	@Override
	public int compareTo(String str) throws PageException {
		throw eng.getExceptionUtil().createExpressionException("can't compare Com Object with a String");
	}

	public boolean isInitalized() {
		return true;
	}

	public String getName() {
		return name;
	}

	public Dispatch getDispatch() {
		return dispatch;
	}

	public Variant getParent() {
		return parent;
	}

	/**
	 * release the com Object
	 */
	public void release() {
		dispatch.safeRelease();
	}

	public Iterator<Object> iterator() {
		return valueIterator();
	}

	@Override
	public Iterator<Collection.Key> keyIterator() {
		return new COMKeyWrapperIterator(this);
	}

	@Override
	public Iterator<String> keysAsStringIterator() {
		final Iterator<Collection.Key> it = keyIterator();
		return new Iterator<String>() {
			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public String next() {
				return it.next().getString();
			}

			@Override
			public void remove() {
				it.remove();
			}
		};
	}

	@Override
	public Iterator<Object> valueIterator() {
		return new COMValueWrapperIterator(this);
	}

	@Override
	public Iterator<Entry<Key, Object>> entryIterator() {
		final Iterator<Collection.Key> it = keyIterator();
		final COMObject obj = this;
		return new Iterator<Entry<Key, Object>>() {
			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public Entry<Key, Object> next() {
				Key key = it.next();
				return new SimpleEntry<Key, Object>(key, obj.get(null, key, null));
			}

			@Override
			public void remove() {
				it.remove();
			}
		};
	}
}
