component extends = "org.lucee.cfml.test.LuceeTestCase" {

	function run( testResults, textbox ) {

		describe( title="LDEV-5763 javasettings attribute variations", body=function() {

			describe( title="Tag-in-script syntax", body=function() {

				it( title="struct literal (now throws validation error)", body=function( currentSpec ) {
					expect( function() {
						var obj = new LDEV5763.LDEV5763_cfml();
					}).toThrow();
				});

				it( title="string format (WORKS)", body=function( currentSpec ) {
					var obj = new LDEV5763.LDEV5763_json();
					var meta = getMetadata( obj );
					expect( meta.functions ).toHaveLength( 2 );
				});

			});

			describe( title="Function-style syntax", body=function() {

				it( title="struct literal (parse error)", body=function( currentSpec ) {
					expect( function() {
						var obj = new LDEV5763.LDEV5763_function_struct();
					}).toThrow();
				});

				xit( title="string format (ASI bug - separate issue)", body=function( currentSpec ) {
					var obj = new LDEV5763.LDEV5763_function_string();
					var meta = getMetadata( obj );
					expect( meta.functions ).toHaveLength( 1 );
				});

			});

			describe( title="Traditional tag syntax", body=function() {

				it( title="string format (WORKS)", body=function( currentSpec ) {
					var obj = createObject( "component", "LDEV5763.LDEV5763_tag_string" );
					var meta = getMetadata( obj );
					expect( meta.functions ).toHaveLength( 1 );
				});

				it( title="unquoted struct literal", body=function( currentSpec ) {
					expect( function() {
						var obj = createObject( "component", "LDEV5763.LDEV5763_tag_unquoted_struct" );
					}).toThrow();
				});

				it( title="struct via expression (WORKS)", body=function( currentSpec ) {
					var obj = createObject( "component", "LDEV5763.LDEV5763_tag_struct" );
					var meta = getMetadata( obj );
					expect( meta.functions ).toHaveLength( 1 );
				});

			});

			describe( title="Edge cases", body=function() {

				it( title="multiple maven coordinates string (WORKS)", body=function( currentSpec ) {
					var obj = new LDEV5763.LDEV5763_multiple();
					var meta = getMetadata( obj );
					expect( meta.functions ).toHaveLength( 1 );
					expect( meta.javasettings ).toInclude( "commons-lang3" );
					expect( meta.javasettings ).toInclude( "commons-io" );
				});

				it( title="multiple maven coordinates struct (now throws validation error)", body=function( currentSpec ) {
					expect( function() {
						var obj = new LDEV5763.LDEV5763_multiple_struct();
					}).toThrow();
				});

			});

		});

	}

}