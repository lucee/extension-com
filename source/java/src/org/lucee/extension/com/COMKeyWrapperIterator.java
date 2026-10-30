package org.lucee.extension.com;

import com.jacob.com.Variant;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Collection;

/**
 * iterates over the elements of a COM collection, the elements are the keys
 */
public final class COMKeyWrapperIterator extends COMEnumIterator<Collection.Key> {

	private static final CFMLEngine eng = CFMLEngineFactory.getInstance();

	public COMKeyWrapperIterator(COMObject wrapper) {
		super(wrapper);
	}

	@Override
	protected Collection.Key convert(Variant variant) {
		try {
			return eng.getCastUtil().toKey(COMUtil.toObject(wrapper, variant, "", null));
		}
		catch (PageException e) {
			throw eng.getExceptionUtil().createPageRuntimeException(e);
		}
	}
}
