package lucee.runtime.engine;

/**
 * Extended ExecutionLog interface with line number support.
 * Used by native debugger for line-level tracking.
 */
public interface ExecutionLogPro extends ExecutionLog {

	/**
	 * Start tracking execution at a position with line number info.
	 *
	 * @param pos position identifier
	 * @param line source line number
	 * @param id unique identifier for this execution block
	 */
	public void start(int pos, int line, String id);

	/**
	 * End tracking execution at a position with line number info.
	 *
	 * @param pos position identifier
	 * @param line source line number
	 * @param id unique identifier for this execution block
	 */
	public void end(int pos, int line, String id);
}
