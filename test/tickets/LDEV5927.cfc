component extends="org.lucee.cfml.test.LuceeTestCase" {

	function run( testResults, testBox ) {
		describe( "LDEV-5927: javasettings JSON validation at compile time", function() {

			it( "should fail at compile time with invalid JSON", function() {
				expect( function() {
					new LDEV5927.invalid_json();
				}).toThrow();
			});

		});
	}
}
