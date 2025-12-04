package lucee.runtime.functions.decision;

import lucee.runtime.PageContext;
import lucee.runtime.config.ConfigImpl;

public final class IsDebuggerEnabled {

	public static boolean call(PageContext pc) {
		return ConfigImpl.DEBUGGER_ENABLED;
	}
}
