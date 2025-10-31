component extends="org.lucee.cfml.test.LuceeTestCase" {
	function run( testResults, testBox ) {
		describe( title="Test suite for URLSessionFormat()", body=function() {

			it( title="checking URLSessionFormat() with simple URL", body=function( currentSpec ) {
				var testUrl = "http://example.com/page.cfm";
				var result = URLSessionFormat( testUrl );
				systemOutput(result, true);
				assertTrue( len( result ) > 0 );
				assertTrue( findNoCase( "example.com", result ) > 0 );
			});

			it( title="checking URLSessionFormat() with query string", body=function( currentSpec ) {
				var testUrl = "http://example.com/page.cfm?foo=bar";
				var result = URLSessionFormat( testUrl );
				systemOutput(result, true);
				assertTrue( findNoCase( "foo=bar", result ) > 0 );
			});

			it( title="checking URLSessionFormat() preserves URL structure", body=function( currentSpec ) {
				var testUrl = "http://example.com/test.cfm";
				var result = URLSessionFormat( testUrl );
				systemOutput(result, true);
				assertTrue( isSimpleValue( result ) );
				assertTrue( len( result ) >= len( testUrl ) );
			});

		});
	}
}
