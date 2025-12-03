package lucee.runtime.debug;

/**
 * Static registry for the debugger listener.
 * Only one debugger listener is supported at a time.
 *
 * External debuggers (e.g., luceedebug) can access this via reflection:
 *
 *   Class<?> registryClass = Class.forName("lucee.runtime.debug.DebuggerRegistry");
 *   Method setListener = registryClass.getMethod("setListener", Class.forName("lucee.runtime.debug.DebuggerListener"));
 *   setListener.invoke(null, myListener);
 */
public final class DebuggerRegistry {

	private static volatile DebuggerListener listener;

	private DebuggerRegistry() {
		// Static utility class
	}

	/**
	 * Set the debugger listener. Pass null to unregister.
	 * Only one listener is supported - setting a new one replaces the old.
	 *
	 * @param l The listener to register, or null to unregister
	 */
	public static void setListener(DebuggerListener l) {
		listener = l;
	}

	/**
	 * Get the current debugger listener.
	 *
	 * @return The registered listener, or null if none
	 */
	public static DebuggerListener getListener() {
		return listener;
	}

	/**
	 * Check if a debugger listener is registered.
	 *
	 * @return true if a listener is registered
	 */
	public static boolean hasListener() {
		return listener != null;
	}
}
