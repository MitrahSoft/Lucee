// Mimics TestBox's BaseSpec
component {

	function assert( required expression, message = "" ) {
		return true;
	}

	function describe( required string title, required function body ) {
		// Execute the closure - this changes the execution context
		return arguments.body();
	}

	function it( required string title, required function body ) {
		// Execute the closure - this changes the execution context
		return arguments.body();
	}

	function runTest() {
		// This is meant to be overridden
		return true;
	}
}
