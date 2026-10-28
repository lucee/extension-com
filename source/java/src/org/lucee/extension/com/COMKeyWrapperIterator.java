package org.lucee.extension.com;

import java.util.Iterator;

import com.jacob.com.EnumVariant;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Collection;

/**
 * iterates over the elements of a COM collection, the elements are the keys
 */
public final class COMKeyWrapperIterator implements Iterator<Collection.Key> {

	private static final CFMLEngine eng = CFMLEngineFactory.getInstance();

	private final EnumVariant enumVariant;
	private final COMObject wrapper;

	public COMKeyWrapperIterator(COMObject wrapper) {
		this.enumVariant = new EnumVariant(wrapper.getDispatch());
		this.wrapper = wrapper;
	}

	@Override
	public void remove() {
		enumVariant.safeRelease();
	}

	@Override
	public boolean hasNext() {
		return enumVariant.hasMoreElements();
	}

	@Override
	public Collection.Key next() {
		try {
			return eng.getCastUtil().toKey(COMUtil.toObject(wrapper, enumVariant.Next(), "", null));
		}
		catch (PageException e) {
			throw eng.getExceptionUtil().createPageRuntimeException(e);
		}
	}
}
