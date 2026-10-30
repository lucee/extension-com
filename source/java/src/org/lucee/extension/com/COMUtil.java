package org.lucee.extension.com;

import com.jacob.com.Variant;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Array;

public final class COMUtil {

	private static final CFMLEngine eng = CFMLEngineFactory.getInstance();

	/**
	 * translate a Variant Object to Object, when it is a Dispatch translate it to COMObject
	 */
	public static Object toObject(COMObject parent, Variant variant, String key, Object defaultValue) {
		try {
			return toObject(parent, variant, key);
		}
		catch (PageException pe) {
			return defaultValue;
		}
	}

	public static Object toObject(COMObject parent, Variant variant, String key) throws PageException {
		short type = variant.getvt();

		if (type == Variant.VariantEmpty) return null;
		else if (type == Variant.VariantNull) return null;
		else if (type == Variant.VariantShort) return Short.valueOf(variant.getShort());
		else if (type == Variant.VariantInt) return Integer.valueOf(variant.getInt());
		else if (type == Variant.VariantFloat) return Float.valueOf(variant.getFloat());
		else if (type == Variant.VariantDouble) return Double.valueOf(variant.getDouble());
		// the raw COM value is the amount multiplied by 10000
		else if (type == Variant.VariantCurrency) return java.math.BigDecimal.valueOf(variant.getCurrency().longValue(), 4);
		else if (type == Variant.VariantObject) return variant.toEnumVariant();
		// getDate() is the raw OLE date (days since 1899-12-30), getJavaDate() the converted one
		else if (type == Variant.VariantDate) {
			java.util.Date date = variant.getJavaDate();
			return date == null ? null : eng.getCreationUtil().createDateTime(date.getTime());
		}
		else if (type == Variant.VariantString) return variant.getString();
		else if (type == Variant.VariantBoolean) return variant.getBoolean() ? Boolean.TRUE : Boolean.FALSE;
		else if (type == Variant.VariantByte) return Byte.valueOf(variant.getByte());
		else if (type == Variant.VariantVariant) {
			throw eng.getExceptionUtil().createExpressionException("type variant is not supported");
		}
		else if (type == Variant.VariantArray) {
			Variant[] varr = variant.getVariantArrayRef();
			Array arr = eng.getCreationUtil().createArray();
			for (int i = 0; i < varr.length; i++) {
				arr.appendEL(toObject(parent, varr[i], String.valueOf(i)));
			}
			return arr;
		}
		else if (type == Variant.VariantDispatch) {
			return new COMObject(variant, variant.toDispatch(), parent.getName() + "." + key);
		}

		throw eng.getExceptionUtil().createExpressionException("COM Type [" + toStringType(type) + "] not supported");
	}

	/**
	 * translate a short Variant Type Definition to a String (string,empty,null,short ...)
	 */
	public static String toStringType(short type) {
		if (type == Variant.VariantEmpty) return "empty";
		else if (type == Variant.VariantNull) return "null";
		else if (type == Variant.VariantShort) return "Short";
		else if (type == Variant.VariantInt) return "Integer";
		else if (type == Variant.VariantFloat) return "Float";
		else if (type == Variant.VariantDouble) return "Double";
		else if (type == Variant.VariantCurrency) return "Currency";
		else if (type == Variant.VariantDate) return "Date";
		else if (type == Variant.VariantString) return "String";
		else if (type == Variant.VariantBoolean) return "Boolean";
		else if (type == Variant.VariantByte) return "Byte";
		else if (type == Variant.VariantArray) return "Array";
		else if (type == Variant.VariantDispatch) return "Dispatch";
		else if (type == Variant.VariantByref) return "Byref";
		else if (type == Variant.VariantError) return "Error";
		else if (type == Variant.VariantObject) return "Object";
		else if (type == Variant.VariantTypeMask) return "TypeMask";
		else if (type == Variant.VariantVariant) return "Variant";
		else return "unknown";
	}
}
