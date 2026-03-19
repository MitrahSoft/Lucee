component extends="org.lucee.cfml.test.LuceeTestCase" labels="internalRequest" {

	function beforeAll() {
		variables.uri = createURI( "LDEV6086" );
	}

	function run( testResults, testBox ) {
		describe( "LDEV-6086", function() {

			it( title="InternalRequest should not swallow RequestTimeoutException", body=function() {
				var result = _InternalRequest(
					template: "#variables.uri#/timeout.cfm",
					throwonerror: false
				);
				expect( result ).toHaveKey( "error" );
				expect( result.error.type ).toBe( "lucee.runtime.exp.RequestTimeoutException" );
			});

			it( title="InternalRequest should throw RequestTimeoutException with throwonerror=true", body=function() {
				expect( function() {
					_InternalRequest(
						template: "#variables.uri#/timeout.cfm",
						throwonerror: true
					);
				}).toThrow( "lucee.runtime.exp.RequestTimeoutException" );
			});

		});
	}

	private string function createURI( string calledName ) {
		var baseURI = "/test/#listLast( getDirectoryFromPath( getCurrentTemplatePath() ), "\/" )#/";
		return baseURI & "" & calledName;
	}

}
