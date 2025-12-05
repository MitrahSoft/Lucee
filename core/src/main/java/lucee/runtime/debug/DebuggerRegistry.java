package lucee.runtime.debug;

import lucee.commons.io.log.Log;
import lucee.commons.io.log.LogUtil;
import lucee.runtime.config.ConfigImpl;

/**
 * Static registry for the debugger listener.
 * Only one debugger listener is supported at a time.
 *
 * External debuggers (e.g., luceedebug) can access this via reflection:
 *
 *   Class<?> registryClass = Class.forName("lucee.runtime.debug.DebuggerRegistry");
 *   Method setListener = registryClass.getMethod("setListener", Class.forName("lucee.runtime.debug.DebuggerListener"), String.class);
 *   setListener.invoke(null, myListener, secret);
 */
public final class DebuggerRegistry {

	private static volatile DebuggerListener listener;

	private DebuggerRegistry() {
		// Static utility class
	}

	/**
	 * Set the debugger listener. Requires the correct secret from LUCEE_DEBUGGER_SECRET.
	 * Only one listener is supported - setting a new one replaces the old.
	 *
	 * @param l The listener to register, or null to unregister
	 * @param secret The secret that must match LUCEE_DEBUGGER_SECRET
	 * @return true if registration succeeded, false if secret is invalid
	 */
	public static boolean setListener(DebuggerListener l, String secret) {
		// Unregister always allowed
		if (l == null) {
			listener = null;
			return true;
		}
		// Registration requires valid secret
		String expectedSecret = ConfigImpl.DEBUGGER_SECRET;
		if (expectedSecret == null || !expectedSecret.equals(secret)) {
			return false;
		}
		listener = l;
		LogUtil.log(Log.LEVEL_INFO, "application", "External debugger registered [" + l.getName() + "]");
		return true;
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

	/**
	 * Check if a debugger client is connected and ready to handle breakpoints.
	 * Returns false if no listener is registered, or if the listener is registered
	 * but no debugger client is attached.
	 *
	 * @return true if a debugger client is connected and ready
	 */
	public static boolean isClientConnected() {
		DebuggerListener l = listener;
		return l != null && l.isClientConnected();
	}
}
