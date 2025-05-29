component extends="org.lucee.cfml.test.LuceeTestCase" {
    function beforeAll(){
		variables.uri = createURI("LDEV5632");
	}
    function run(testResults, testBox) {
        describe("Test case for LDEV-5632", function() {

            it( title="should load OpenTelemetry Span and get current span", body=function( currentSpec) {
                local.result = _InternalRequest(
					template:"#variables.uri#/LDEV5632.cfm");
                expect(result.filecontent.trim()).toBe("success");
            });

        });
    }
    private string function createURI(string calledName){
		var baseURI="/test/#listLast(getDirectoryFromPath(getCurrenttemplatepath()),"\/")#/";
		return baseURI&""&calledName;
	}

}