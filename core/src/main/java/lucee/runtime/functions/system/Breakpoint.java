package lucee.runtime.functions.system;

import lucee.runtime.PageContext;
import lucee.runtime.PageContextImpl;

public final class Breakpoint {

	public static boolean call(PageContext pc) {
		return call(pc, true, null);
	}

	public static boolean call(PageContext pc, boolean condition) {
		return call(pc, condition, null);
	}

	public static boolean call(PageContext pc, boolean condition, String label) {
		if (condition && PageContextImpl.DEBUGGER_ENABLED) {
			((PageContextImpl) pc).debuggerSuspend(label);
			return true;
		}
		return false;
	}
}
