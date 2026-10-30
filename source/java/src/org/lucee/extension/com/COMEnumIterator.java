package org.lucee.extension.com;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.jacob.com.EnumVariant;
import com.jacob.com.Variant;

/**
 * Base of the iterators over the elements of a COM collection. The enumerator is released when the last element is read.
 */
abstract class COMEnumIterator<T> implements Iterator<T> {

	protected final COMObject wrapper;
	private final EnumVariant enumVariant;
	private boolean closed;

	COMEnumIterator(COMObject wrapper) {
		this.wrapper = wrapper;
		this.enumVariant = new EnumVariant(wrapper.getDispatch());
	}

	@Override
	public boolean hasNext() {
		if (closed) return false;
		if (enumVariant.hasMoreElements()) return true;
		close();
		return false;
	}

	@Override
	public T next() {
		if (!hasNext()) throw new NoSuchElementException();
		Variant variant = enumVariant.Next();
		if (variant == null) {
			close();
			throw new NoSuchElementException();
		}
		return convert(variant);
	}

	protected abstract T convert(Variant variant);

	@Override
	public void remove() {
		throw new UnsupportedOperationException("a COM collection cannot be modified with an iterator");
	}

	private void close() {
		if (closed) return;
		closed = true;
		enumVariant.safeRelease();
	}
}
