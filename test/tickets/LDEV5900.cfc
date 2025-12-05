component extends="org.lucee.cfml.test.LuceeTestCase" labels="admin" {

	function run( testResults, testBox ) {
		describe( "LDEV-5900: createArchive should collect all errors", function() {

			it( "should fail fast on first error (default stopOnError=true)", function() {
				testCreateArchive( stopOnError=true, expectedErrorCount=1 );
			});

			it( "should collect all errors when stopOnError=false", function() {
				testCreateArchive( stopOnError=false, expectedErrorCount=2 );
			});

			it( "should collect errors in variable when errorVariable is provided", function() {
				testCreateArchiveWithErrorVariable();
			});

		});
	}

	private function testCreateArchive( required boolean stopOnError, required numeric expectedErrorCount ) {
		var adminPassword = request.SERVERADMINPASSWORD;
		var testVirtual = "/ldev5900-test";
		var testPath = getDirectoryFromPath( getCurrentTemplatePath() ) & "LDEV5900/";
		var archiveFile = expandPath( "{temp-directory}LDEV5900-test.lar" );

		// Clean up any existing mapping and archive
		try {
			admin action="removeMapping" type="web" password="#adminPassword#" virtual="#testVirtual#";
		} catch( any e ) {}

		if ( fileExists( archiveFile ) ) {
			fileDelete( archiveFile );
		}

		// Create mapping pointing to our test assets
		admin
			action="updateMapping"
			type="web"
			password="#adminPassword#"
			virtual="#testVirtual#"
			physical="#testPath#"
			toplevel="true"
			archive=""
			primary="physical"
			trusted="no";

		// Try to create archive
		var caughtError = false;
		var errorMessage = "";
		try {
			admin
				action="createArchive"
				type="web"
				password="#adminPassword#"
				file="#archiveFile#"
				virtual="#testVirtual#"
				addCFMLFiles="true"
				addNonCFMLFiles="false"
				stopOnError="#stopOnError#";
		} catch( any e ) {
			caughtError = true;
			errorMessage = e.message;
		}

		// Should have caught an error
		expect( caughtError ).toBeTrue( "Expected an error to be thrown" );

		// Archive file should NOT have been created when there are errors
		expect( fileExists( archiveFile ) ).toBeFalse( "Archive should not be created when errors occur" );

		// Count how many bad files are mentioned in the error
		var bad1Mentioned = findNoCase( "bad1.cfm", errorMessage ) > 0;
		var bad2Mentioned = findNoCase( "bad2.cfm", errorMessage ) > 0;
		var errorCount = ( bad1Mentioned ? 1 : 0 ) + ( bad2Mentioned ? 1 : 0 );

		expect( errorCount ).toBe( expectedErrorCount, "stopOnError=#stopOnError#: expected #expectedErrorCount# errors" );
	}

	private function testCreateArchiveWithErrorVariable() {
		var adminPassword = request.SERVERADMINPASSWORD;
		var testVirtual = "/ldev5900-test-errorvar";
		var testPath = getDirectoryFromPath( getCurrentTemplatePath() ) & "LDEV5900/";
		var archiveFile = expandPath( "{temp-directory}LDEV5900-test-errorvar.lar" );

		// Clean up any existing mapping and archive
		try {
			admin action="removeMapping" type="web" password="#adminPassword#" virtual="#testVirtual#";
		} catch( any e ) {}

		if ( fileExists( archiveFile ) ) {
			fileDelete( archiveFile );
		}

		// Create mapping pointing to our test assets
		admin
			action="updateMapping"
			type="web"
			password="#adminPassword#"
			virtual="#testVirtual#"
			physical="#testPath#"
			toplevel="true"
			archive=""
			primary="physical"
			trusted="no";

		// Try to create archive with errorVariable - should NOT throw
		var caughtError = false;
		var compilationErrors = {};
		try {
			admin
				action="createArchive"
				type="web"
				password="#adminPassword#"
				file="#archiveFile#"
				virtual="#testVirtual#"
				addCFMLFiles="true"
				addNonCFMLFiles="false"
				errorVariable="compilationErrors";
		} catch( any e ) {
			caughtError = true;
		}

		// Should NOT have thrown an error
		expect( caughtError ).toBeFalse( "Should not throw when errorVariable is provided" );

		// Should have errors in the variable
		expect( compilationErrors ).toBeStruct();
		expect( structCount( compilationErrors ) ).toBe( 2, "Should have 2 errors collected" );

		for ( var filePath in compilationErrors ) {
			var errorInfo = compilationErrors[ filePath ];

			// Verify error info structure
			expect( errorInfo ).toBeStruct( "Error should be a struct" );
			expect( errorInfo ).toHaveKey( "message", "Error should have message" );
			expect( errorInfo ).toHaveKey( "detail", "Error should have detail" );
		}

		// Archive file should NOT have been created when there are errors
		expect( fileExists( archiveFile ) ).toBeFalse( "Archive should not be created when errors occur" );

		// Verify both bad files are in the error struct
		var bad1Found = false;
		var bad2Found = false;
		for ( var filePath in compilationErrors ) {
			if ( findNoCase( "bad1.cfm", filePath ) ) bad1Found = true;
			if ( findNoCase( "bad2.cfm", filePath ) ) bad2Found = true;
		}

		expect( bad1Found ).toBeTrue( "bad1.cfm should be in errors" );
		expect( bad2Found ).toBeTrue( "bad2.cfm should be in errors" );

		// Clean up
		try {
			admin action="removeMapping" type="web" password="#adminPassword#" virtual="#testVirtual#";
		} catch( any e ) {}
	}

	function afterAll() {
		var adminPassword = request.SERVERADMINPASSWORD;
		try {
			admin action="removeMapping" type="web" password="#adminPassword#" virtual="/ldev5900-test";
		} catch( any e ) {}
		try {
			admin action="removeMapping" type="web" password="#adminPassword#" virtual="/ldev5900-test-errorvar";
		} catch( any e ) {}
	}

}
