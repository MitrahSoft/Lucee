component extends="org.lucee.cfml.test.LuceeTestCase" skip=true {
	function run( testResults, textbox ) {
		describe("Test case for LDEV-5834 - cfexecute handlers lose function scope", function() {

			it( title = "cfexecute onProgress handler should have access to local variables", body = function( currentSpec ) {
				var localVar = "localValue";
				var capturedLocal = "";

				cfexecute(
					name = "echo",
					arguments = "test output",
					timeout = 1,
					onProgress = function(line, process) {
						// This should be able to access localVar
						capturedLocal = localVar;
						return true;
					}
				);

				expect( capturedLocal ).toBe( "localValue" );
			});

			it( title = "cfexecute onError handler should have access to local variables", body = function( currentSpec ) {
				var localVar = "localValue";
				var hasError = false;
				var capturedLocal = "";

				cfexecute(
					name = "invalidcommandthatdoesnotexist",
					timeout = 1,
					onError = function(errorLine) {
						// This should be able to access and modify local variables
						hasError = true;
						capturedLocal = localVar;
						return true;
					}
				);

				// These should have been modified by the error handler
				expect( hasError ).toBe( true );
				expect( capturedLocal ).toBe( "localValue" );
			});

			it( title = "cfexecute handlers should have access to function arguments", body = function( currentSpec ) {
				runTestWithArgs("testValue", "testFilter");
			});

			it( title = "cfexecute handlers should be able to modify variables in parent scope", body = function( currentSpec ) {
				variables.sharedVar = "initial";
				var capturedValue = "";

				cfexecute(
					name = "echo",
					arguments = "test",
					timeout = 1,
					onProgress = function(line, process) {
						// Should be able to read and modify variables scope
						capturedValue = variables.sharedVar;
						variables.sharedVar = "modified";
						return true;
					}
				);

				expect( capturedValue ).toBe( "initial" );
				expect( variables.sharedVar ).toBe( "modified" );
			});
		});
	}

	private function runTestWithArgs(required string argValue, required string argFilter) {
		var capturedArg1 = "";
		var capturedArg2 = "";

		cfexecute(
			name = "echo",
			arguments = "test",
			timeout = 1,
			onProgress = function(line, process) {
				// Should be able to access function arguments
				capturedArg1 = arguments.argValue;
				capturedArg2 = arguments.argFilter;
				return true;
			}
		);

		expect( capturedArg1 ).toBe( arguments.argValue );
		expect( capturedArg2 ).toBe( arguments.argFilter );
	}
}