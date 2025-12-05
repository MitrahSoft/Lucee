package lucee.runtime.debug;

import java.io.PrintStream;

/**
 * PrintStream wrapper that tees output to the DebuggerListener.
 * Installed on System.out/err when DEBUGGER_ENABLED is true.
 * Always passes through to the original stream; only notifies listener when active.
 */
public class DebuggerPrintStream extends PrintStream {
	private final PrintStream original;
	private final boolean isStdErr;

	public DebuggerPrintStream(PrintStream original, boolean isStdErr) {
		super(original, true); // autoFlush=true
		this.original = original;
		this.isStdErr = isStdErr;
	}

	@Override
	public void write(byte[] buf, int off, int len) {
		super.write(buf, off, len);
		notifyListener(new String(buf, off, len));
	}

	@Override
	public void write(int b) {
		super.write(b);
		// Single bytes get buffered, don't notify for each one
	}

	// Note: We only override write() methods to notify the listener.
	// Do NOT override print/println - they internally call write() and
	// would cause double notifications.

	private void notifyListener(String text) {
		if (text == null || text.isEmpty()) return;
		DebuggerListener listener = DebuggerRegistry.getListener();
		if (listener != null && listener.isActive()) {
			listener.onOutput(text, isStdErr);
		}
	}

	/**
	 * Get the original stream (for unwrapping if needed).
	 */
	public PrintStream getOriginal() {
		return original;
	}
}
