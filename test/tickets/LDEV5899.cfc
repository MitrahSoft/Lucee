component extends="org.lucee.cfml.test.LuceeTestCase" labels="admin" {

	function run( testResults, testBox ) {
		describe( "LDEV-5899: updateMapping cache refresh", function() {

			it( "should refresh cache with type='server' (first)", function() {
				testMappingRefresh( "server", "first" );
			});

			it( "should refresh cache with type='server' (second)", function() {
				testMappingRefresh( "server", "second" );
			});

			it( "should refresh cache with type='web' (first)", function() {
				testMappingRefresh( "web", "first" );
			});

			it( "should refresh cache with type='web' (second)", function() {
				testMappingRefresh( "web", "second" );
			});

		});

		describe( "LDEV-5899: removeMapping cache refresh", function() {

			it( "should refresh cache with type='server' (first)", function() {
				testRemoveMapping( "server", "first" );
			});

			it( "should refresh cache with type='server' (second)", function() {
				testRemoveMapping( "server", "second" );
			});

			it( "should refresh cache with type='web' (first)", function() {
				testRemoveMapping( "web", "first" );
			});

			it( "should refresh cache with type='web' (second)", function() {
				testRemoveMapping( "web", "second" );
			});

		});

		describe( "LDEV-5899: createArchive with append cache refresh", function() {

			it( "should refresh cache with type='server' (first)", function() {
				testCreateArchiveAppend( "server", "first" );
			});

			it( "should refresh cache with type='server' (second)", function() {
				testCreateArchiveAppend( "server", "second" );
			});

			it( "should refresh cache with type='web' (first)", function() {
				testCreateArchiveAppend( "web", "first" );
			});

			it( "should refresh cache with type='web' (second)", function() {
				testCreateArchiveAppend( "web", "second" );
			});

		});
	}

	function afterAll() {
		var adminPassword = request.SERVERADMINPASSWORD;
		var orders = [ "first", "second" ];
		var types = [ "server", "web" ];
		var prefixes = [ "update", "remove", "archive" ];

		for ( var order in orders ) {
			for ( var type in types ) {
				for ( var prefix in prefixes ) {
					admin action="removeMapping" type="#type#" password="#adminPassword#" virtual="/ldev5899-#prefix#-#type#-#order#";
				}
			}
		}
	};

	private function testMappingRefresh( required string type, string order="first" ) {
		var adminPassword = request.SERVERADMINPASSWORD;
		var testVirtual = "/ldev5899-update-#type#-#order#";
		var testPath = expandPath( "{temp-directory}cache/LDEV5899/update/#type#/#order#/" );

		directoryCreate( testPath, true, true );

		// Populate cache first by getting all mappings
		admin
			action="getMappings"
			type="#type#"
			password="#adminPassword#"
			returnVariable="local.existingMappings";

		// Now create mapping - this should refresh the cache
		admin
			action="updateMapping"
			type="#type#"
			password="#adminPassword#"
			virtual="#testVirtual#"
			physical="#testPath#"
			toplevel="true"
			archive=""
			primary="physical"
			trusted="no";

		// Try to get the mapping - if cache wasn't refreshed, this will fail
		admin
			action="getMapping"
			type="#type#"
			password="#adminPassword#"
			virtual="#testVirtual#"
			returnVariable="local.mapping";

		expect( mapping ).toHaveKey( "physical" );
	}

	private function testRemoveMapping( required string type, string order="first" ) {
		var adminPassword = request.SERVERADMINPASSWORD;
		var testVirtual = "/ldev5899-remove-#type#-#order#";
		var testPath = expandPath( "{temp-directory}cache/LDEV5899/remove/#type#/#order#/" );

		directoryCreate( testPath, true, true );

		// Create mapping
		admin
			action="updateMapping"
			type="#type#"
			password="#adminPassword#"
			virtual="#testVirtual#"
			physical="#testPath#"
			toplevel="true"
			archive=""
			primary="physical"
			trusted="no";

		// Populate cache by getting mappings
		admin
			action="getMappings"
			type="#type#"
			password="#adminPassword#"
			returnVariable="local.mappings";

		// Remove mapping - this should refresh the cache
		admin
			action="removeMapping"
			type="#type#"
			password="#adminPassword#"
			virtual="#testVirtual#";

		// Check if mapping is gone from cache
		admin
			action="getMappings"
			type="#type#"
			password="#adminPassword#"
			returnVariable="local.mappings";

		var found = queryReduce( mappings, function( result, row ) {
			return result || row.virtual == testVirtual;
		}, false );

		expect( found ).toBeFalse();
	}

	private function testCreateArchiveAppend( required string type, string order="first" ) {
		var adminPassword = request.SERVERADMINPASSWORD;
		var testVirtual = "/ldev5899-archive-#type#-#order#";
		var testPath = expandPath( "{temp-directory}cache/LDEV5899/archive/#type#/#order#/" );
		var archiveFile = expandPath( "{temp-directory}cache/LDEV5899/archive/#type#-#order#.lar" );

		directoryCreate( testPath, true, true );
		fileWrite( testPath & "test.cfm", "<cfoutput>test</cfoutput>" );

		// Create mapping
		admin
			action="updateMapping"
			type="#type#"
			password="#adminPassword#"
			virtual="#testVirtual#"
			physical="#testPath#"
			toplevel="true"
			archive=""
			primary="physical"
			trusted="no";

		// Populate cache by getting the mapping
		admin
			action="getMapping"
			type="#type#"
			password="#adminPassword#"
			virtual="#testVirtual#"
			returnVariable="local.mapping1";

		// Create archive with append=true - this should update the mapping and refresh cache
		admin
			action="createArchive"
			type="#type#"
			password="#adminPassword#"
			file="#archiveFile#"
			virtual="#testVirtual#"
			addCFMLFiles="true"
			addNonCFMLFiles="false"
			append="true";

		// Get mapping again - if cache wasn't refreshed, archive will be missing
		admin
			action="getMapping"
			type="#type#"
			password="#adminPassword#"
			virtual="#testVirtual#"
			returnVariable="local.mapping2";

		expect( mapping2 ).toHaveKey( "archive" );
		expect( mapping2.archive ).notToBeNull();
	}

}
