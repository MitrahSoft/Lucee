package lucee.runtime.functions.decision;

import lucee.runtime.PageContext;
import lucee.runtime.PageContextImpl;

public final class IsDebuggerEnabled {

	public static boolean call(PageContext pc) {
		return PageContextImpl.DEBUGGER_ENABLED;
	}
}
