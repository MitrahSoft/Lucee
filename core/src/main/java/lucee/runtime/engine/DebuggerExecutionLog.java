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

	/**
	 * Thread-local to track the current file and line for top-level code.
	 * Used by debuggerSuspend() when there's no DebuggerFrame.
	 */
	private static final ThreadLocal<int[]> currentTopLevelLine = new ThreadLocal<>();
	private static final ThreadLocal<String> currentTopLevelFile = new ThreadLocal<>();

	/**
	 * Get the current top-level file for this thread (set by ExecutionLog).
	 * Returns null if not in top-level code or ExecutionLog hasn't run yet.
	 */
	public static String getCurrentFile() {
		return currentTopLevelFile.get();
	}

	/**
	 * Get the current top-level line for this thread (set by ExecutionLog).
	 * Returns 0 if not in top-level code or ExecutionLog hasn't run yet.
	 */
	public static int getCurrentLine() {
		int[] line = currentTopLevelLine.get();
		return line != null ? line[0] : 0;
	}

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
		// Get file from debugger frame if available, otherwise from current page source
		String file = null;
		PageContextImpl.DebuggerFrame frame = pci.getTopmostDebuggerFrame();
		if (frame != null) {
			frame.setLine(line);
			file = frame.getFile();
		} else {
			// Top-level code (outside functions) - get file from page source
			lucee.runtime.PageSource ps = pci.getCurrentPageSource(null);
			if (ps != null) {
				lucee.commons.io.res.Resource res = ps.getPhyscalFile();
				if (res != null) {
					file = res.getAbsolutePath();
				}
			}
			// Store in thread-local for breakpoint() to use
			currentTopLevelFile.set(file);
			int[] lineHolder = currentTopLevelLine.get();
			if (lineHolder == null) {
				currentTopLevelLine.set(new int[] { line });
			} else {
				lineHolder[0] = line;
			}
		}

		// Check if debugger wants to suspend (breakpoint, stepping, etc.)
		if (file != null) {
			DebuggerListener listener = DebuggerRegistry.getListener();
			if (listener != null && listener.shouldSuspend(pci, file, line)) {
				pci.debuggerSuspend(file, line, null);
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
		// Clean up thread-locals to avoid memory leaks in thread pools
		currentTopLevelLine.remove();
		currentTopLevelFile.remove();
	}
}
