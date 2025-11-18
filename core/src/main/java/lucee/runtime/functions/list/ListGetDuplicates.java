package lucee.runtime.functions.list;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import lucee.runtime.PageContext;
import lucee.runtime.exp.FunctionException;
import lucee.runtime.exp.PageException;
import lucee.runtime.ext.function.BIF;
import lucee.runtime.op.Caster;
import lucee.runtime.type.Array;
import lucee.runtime.type.util.ListUtil;

public final class ListGetDuplicates extends BIF {

	private static final long serialVersionUID = -5596215135196751629L;

	public static String call(PageContext pc, String list) throws PageException {
		return call(pc, list, ",", false);
	}

	public static String call(PageContext pc, String list, String delimiter) throws PageException {
		return call(pc, list, delimiter, false);
	}

	public static String call(PageContext pc, String list, String delimiter, boolean ignoreCase) throws PageException {
		return call(pc, list, delimiter, ignoreCase, false);
	}

	public static String call(PageContext pc, String list, String delimiter, boolean ignoreCase, boolean includeEmptyFields) throws PageException {
		if (list == null) return "";
		if (delimiter == null) delimiter = ",";
		Array array = ListUtil.listToArray(list, delimiter, includeEmptyFields, false);
		Set<String> existing;
		Set<String> found;
		if (ignoreCase){
			existing = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
			found = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		} else {
			existing = new HashSet<String>();
			found = new HashSet<String>();
		}
		StringBuilder sb = new StringBuilder();
		Iterator<Object> it = array.valueIterator();
		String value;
		while (it.hasNext()) {
			value = Caster.toString(it.next());
			if (existing.contains(value) && !found.contains(value)) {
				if (sb.length() > 0) sb.append(delimiter);
				else if (includeEmptyFields && sb.length() == 0 && found.size() == 1) sb.append(delimiter); // when the first duplicate is empty
				sb.append(value);
				found.add(value);
			}
			existing.add(value);
		}
		return sb.toString();
	}

	@Override
	public Object invoke(PageContext pc, Object[] args) throws PageException {
		if (args.length == 1) return call(pc, Caster.toString(args[0]));
		if (args.length == 2) return call(pc, Caster.toString(args[0]), Caster.toString(args[1]));
		if (args.length == 3) return call(pc, Caster.toString(args[0]), Caster.toString(args[1]), Caster.toBooleanValue(args[2]));
		if (args.length == 4) return call(pc, Caster.toString(args[0]), Caster.toString(args[1]), Caster.toBooleanValue(args[2]), Caster.toBooleanValue(args[3]));

		throw new FunctionException(pc, "ListGetDuplicates", 1, 4, args.length);
	}
}