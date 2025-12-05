<cfscript>
	try {
		test = new ActualTest();
		result = test.runTest();
		writeOutput( "SUCCESS" );
	}
	catch ( any e ) {
		writeOutput( e.message & chr(10) & e.stacktrace );
	}
</cfscript>
