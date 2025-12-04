package lucee.runtime.functions.system;

import lucee.aprint;
import lucee.runtime.PageContext;
import lucee.runtime.PageContextImpl;
import lucee.runtime.config.ConfigImpl;
import lucee.runtime.debug.DebuggerRegistry;

public final class Breakpoint {

	public static boolean call(PageContext pc) {
		return call(pc, null, true);
	}

	public static boolean call(PageContext pc, String label) {
		return call(pc, label, true);
	}

	public static boolean call(PageContext pc, String label, boolean condition) {
		aprint.o("Breakpoint.call() - condition=" + condition + " DEBUGGER_ENABLED=" + ConfigImpl.DEBUGGER_ENABLED + " isActive=" + DebuggerRegistry.isActive());
		// Only suspend if debugger is enabled AND a debugger client is connected
		if (condition && ConfigImpl.DEBUGGER_ENABLED && DebuggerRegistry.isActive()) {
			aprint.o("Breakpoint.call() - SUSPENDING");
			((PageContextImpl) pc).debuggerSuspend(label);
			return true;
		}
		aprint.o("Breakpoint.call() - NOT suspending");
		return false;
	}
}
