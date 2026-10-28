package org.lucee.extension.com;

import java.util.Iterator;

import com.jacob.com.EnumVariant;

/**
 * iterates over the elements of a COM collection
 */
public final class COMValueWrapperIterator implements Iterator<Object> {

	private final EnumVariant enumVariant;
	private final COMObject wrapper;

	public COMValueWrapperIterator(COMObject wrapper) {
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
	public Object next() {
		return COMUtil.toObject(wrapper, enumVariant.Next(), "", null);
	}
}
