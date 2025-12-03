package lucee.runtime.debug;

import lucee.runtime.PageContext;

/**
 * Listener interface for external debuggers (e.g., luceedebug/VS Code DAP).
 * Allows debuggers to be notified of suspend/resume events and to register breakpoints.
 *
 * Only one debugger listener is supported at a time.
 */
public interface DebuggerListener {

	/**
	 * Called when a thread is about to suspend (before blocking).
	 * This is called on the suspended thread's stack, so the debugger should
	 * quickly record state and return - the thread will block after this returns.
	 *
	 * @param pc The PageContext that is suspending
	 * @param file The source file path (from PageSource.getDisplayPath())
	 * @param line The current line number
	 * @param label Optional label from breakpoint() BIF, may be null
	 */
	void onSuspend(PageContext pc, String file, int line, String label);

	/**
	 * Called when a thread resumes after suspension.
	 *
	 * @param pc The PageContext that is resuming
	 */
	void onResume(PageContext pc);

	/**
	 * Check if execution should suspend at the given location.
	 * Called by DebuggerExecutionLog.start() on every line execution.
	 * Must be fast - avoid synchronization if possible.
	 *
	 * The debugger can return true for breakpoints, stepping, or any other reason.
	 *
	 * @param pc The PageContext executing
	 * @param file The source file path (from PageSource.getDisplayPath())
	 * @param line The current line number
	 * @return true if execution should suspend at this location
	 */
	boolean shouldSuspend(PageContext pc, String file, int line);
}
