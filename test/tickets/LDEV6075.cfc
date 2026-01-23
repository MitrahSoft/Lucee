component extends="org.lucee.cfml.test.LuceeTestCase" labels="osgi" {

	function run( testResults, testBox ) {
		describe( "LDEV-6075: StackOverflowError when downgrading ESAPI extension", function() {

			it( title="Downgrade ESAPI and use encoding functions", body=function( currentSpec ) {
				var adminPassword = request.WEBADMINPASSWORD;
				var esapiExtId = "37C61C0A-5D7E-4256-8572639BE0CF5838";

				// Get current ESAPI version
				admin action="getExtensions"
					type="web"
					password="#adminPassword#"
					returnVariable="local.extensionsBefore";

				var esapiVersionBefore = "";
				for ( var ext in extensionsBefore ) {
					if ( ext.id == esapiExtId ) {
						esapiVersionBefore = ext.version;
						break;
					}
				}

				systemOutput( "ESAPI version before: " & esapiVersionBefore, true );

				// Get extension providers
				admin action="getRHExtensionProviders"
					type="web"
					password="#adminPassword#"
					returnVariable="local.providers";

				// Get available extensions
				admin action="getRHExtensions"
					type="web"
					password="#adminPassword#"
					returnVariable="local.availableExts";

				// Find ESAPI extension
				var esapiExt = {};
				for ( var row in availableExts ) {
					if ( findNoCase( "esapi", row.name ) || findNoCase( "guard", row.name ) || row.id == esapiExtId ) {
						esapiExt = row;
						break;
					}
				}

				expect( esapiExt ).notToBeEmpty( "ESAPI extension should be found" );

				// Download and install ESAPI 2.6.0.1
				var providerURL = providers.url[ 1 ];
				var downloadURL = providerURL & "/rest/extension/provider/full/" & esapiExt.id &
								  "?type=all&coreVersion=" & server.lucee.version & "&version=2.6.0.1";

				systemOutput( "Downloading ESAPI 2.6.0.1 from: " & downloadURL, true );

				cfhttp( url=downloadURL, result="local.httpResult" ) {
					cfhttpparam( type="header", name="accept", value="application/cfml" );
				}

				expect( httpResult.status_code ).toBe( 200, "Extension download should succeed" );

				systemOutput( "Downloaded " & len( httpResult.fileContent ) & " bytes", true );

				// Install the downgraded version
				systemOutput( "Installing ESAPI 2.6.0.1...", true );

				admin action="updateRHExtension"
					type="web"
					password="#adminPassword#"
					source="#httpResult.fileContent#";

				// Verify version changed
				admin action="getExtensions"
					type="web"
					password="#adminPassword#"
					returnVariable="local.extensionsAfter";

				var esapiVersionAfter = "";
				for ( var ext in extensionsAfter ) {
					if ( ext.id == esapiExtId ) {
						esapiVersionAfter = ext.version;
						break;
					}
				}

				systemOutput( "ESAPI version after: " & esapiVersionAfter, true );

				// This is where the StackOverflowError occurs in the buggy version
				// When calling encodeForHTML(), OSGi tries to load bundles and hits the infinite loop
				systemOutput( "Testing ESAPI encoding function (this triggers bundle loading)...", true );

				var testString = "<script>alert('xss')</script>";
				var encoded = encodeForHTML( testString );

				systemOutput( "SUCCESS: encodeForHTML() returned: " & encoded, true );

				expect( encoded ).notToBeEmpty( "Encoding should produce output" );
				expect( encoded ).notToInclude( "<script>", "HTML should be encoded" );
			});

		});
	}

}
