package org.lucee.extension.com;

import com.jacob.com.Variant;

/**
 * iterates over the elements of a COM collection
 */
public final class COMValueWrapperIterator extends COMEnumIterator<Object> {

	public COMValueWrapperIterator(COMObject wrapper) {
		super(wrapper);
	}

	@Override
	protected Object convert(Variant variant) {
		return COMUtil.toObject(wrapper, variant, "", null);
	}
}
