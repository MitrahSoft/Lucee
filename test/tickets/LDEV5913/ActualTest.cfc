// Exact replica of Preside's PresideObjectReaderTest structure
component extends="BaseTest" {

	function runTest() {
		super.assert( true, "No methods key was returned" );
		describe( "readObject()", function(){
			super.assert( true, "No methods key was returned" );
			it( "should return a list of public method when component has public methods", function(){
				// This is the exact pattern from Preside - nested closures calling super.assert()
				super.assert( true, "No methods key was returned" );
				return true;
			} );
		} );
	}
}
