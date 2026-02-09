package lucee.runtime.functions.decision;

import lucee.runtime.PageContext;
import lucee.runtime.config.ConfigServerImpl;
import lucee.runtime.ext.function.Function;

public final class IsDebuggerEnabled implements Function {

	public static boolean call(PageContext pc) {
		return ConfigServerImpl.DEBUGGER;
	}
}
