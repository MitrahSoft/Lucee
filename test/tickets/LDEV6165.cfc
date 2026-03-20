component extends="org.lucee.cfml.test.LuceeTestCase" labels="mapping,archive" {

	variables.testVirtual = "/ldev6165-test";
	variables.adminPassword = "";

	function beforeAll() {
		variables.adminPassword = request.WEBADMINPASSWORD;
		variables.oldMappings = GetApplicationSettings().mappings;
		// cleanup mapping from previous runs
		try {
			admin action="removeMapping" type="web" password="#variables.adminPassword#" virtual="#variables.testVirtual#";
		} catch ( any e ) {}
	}

	function afterAll() {
		application action="update" mappings=variables.oldMappings;
		try {
			admin action="removeMapping" type="web" password="#variables.adminPassword#" virtual="#variables.testVirtual#";
		} catch ( any e ) {}
	}

	private function createLarArchive( required string srcDir, required string larFile, boolean addNonCFMLFiles=false ) {
		// create mapping pointing to physical source
		admin
			action="updateMapping"
			type="web"
			password="#variables.adminPassword#"
			virtual="#variables.testVirtual#"
			physical="#arguments.srcDir#"
			toplevel="true"
			archive=""
			primary="physical"
			trusted="no";

		// create an archive (compiles CFML to .class files)
		admin
			action="createArchive"
			type="web"
			password="#variables.adminPassword#"
			file="#arguments.larFile#"
			virtual="#variables.testVirtual#"
			addCFMLFiles="false"
			addNonCFMLFiles="#arguments.addNonCFMLFiles#";

		expect( fileExists( arguments.larFile ) ).toBeTrue( "LAR file should exist" );

		// switch mapping to archive-only, no physical
		admin
			action="updateMapping"
			type="web"
			password="#variables.adminPassword#"
			virtual="#variables.testVirtual#"
			physical=""
			toplevel="true"
			archive="#arguments.larFile#"
			primary="archive"
			trusted="no";

		// remove the physical source so we know it's coming from the archive
		directoryDelete( arguments.srcDir, true );
	}

	function run( testResults, testBox ) {
		describe( "LDEV-6165: LAR archive mapping with only compiled CFML", function() {

			it( "should resolve pages from a CFML-only LAR archive", function() {
				var testDir = getTempDirectory() & "LDEV6165/cfml-only/";
				var srcDir = testDir & "src/";
				var larFile = testDir & "test.lar";

				// cleanup from previous runs
				if ( directoryExists( testDir ) ) directoryDelete( testDir, true );
				directoryCreate( srcDir, true, true );

				// create a simple CFML file — no non-CFML resources
				fileWrite( srcDir & "hello.cfm", "<cfset greeting = 'cfml-only'>" );

				createLarArchive( srcDir, larFile, false );

				// this is where the bug manifests — include from the archive
				include "#variables.testVirtual#/hello.cfm";
				expect( greeting ).toBe( "cfml-only" );
			});

			it( "should resolve pages from a LAR archive with mixed content", function() {
				var testDir = getTempDirectory() & "LDEV6165/mixed/";
				var srcDir = testDir & "src/";
				var larFile = testDir & "test.lar";

				// cleanup from previous runs
				if ( directoryExists( testDir ) ) directoryDelete( testDir, true );
				directoryCreate( srcDir, true, true );

				// CFML file plus a static resource
				fileWrite( srcDir & "hello.cfm", "<cfset greeting = 'mixed-content'>" );
				fileWrite( srcDir & "style.css", "body { color: red; }" );

				createLarArchive( srcDir, larFile, true );

				include "#variables.testVirtual#/hello.cfm";
				expect( greeting ).toBe( "mixed-content" );
			});

		});
	}

}
