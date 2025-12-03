package lucee.runtime.engine;

import java.util.Map;

import lucee.runtime.PageContext;
import lucee.runtime.PageContextImpl;
import lucee.runtime.debug.DebuggerListener;
import lucee.runtime.debug.DebuggerRegistry;

/**
 * ExecutionLog implementation for external debuggers (e.g., VS Code DAP).
 * Updates the current debugger frame's line number on each block start.
 * Also checks for breakpoints and suspends execution if needed.
 *
 * This is used when DEBUGGER_ENABLED=true to provide line tracking
 * without requiring bytecode instrumentation in the debugger extension.
 */
public final class DebuggerExecutionLog implements ExecutionLog {

	private PageContextImpl pci;

	@Override
	public void init(PageContext pc, Map<String, String> arguments) {
		this.pci = (PageContextImpl) pc;
	}

	@Override
	public void start(int pos, String id) {
		// Legacy - no line info available
	}

	@Override
	public void start(int pos, int line, String id) {
		// Update the debugger frame's line number
		PageContextImpl.DebuggerFrame frame = pci.getTopmostDebuggerFrame();
		if (frame != null) {
			frame.setLine(line);

			// Check if debugger wants to suspend (breakpoint, stepping, etc.)
			DebuggerListener listener = DebuggerRegistry.getListener();
			if (listener != null && listener.shouldSuspend(pci, frame.getFile(), line)) {
				pci.debuggerSuspend(null);
			}
		}
	}

	@Override
	public void end(int pos, String id) {
		// No-op
	}

	@Override
	public void end(int pos, int line, String id) {
		// No-op - we only care about start positions for line tracking
	}

	@Override
	public void release() {
		// Nothing to clean up
	}
}
