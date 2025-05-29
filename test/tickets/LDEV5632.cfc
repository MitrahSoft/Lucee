component extends="org.lucee.cfml.test.LuceeTestCase" skip="true"{
    function beforeAll(){
		variables.uri = createURI("LDEV5632");
	}
    function run(testResults, testBox) {
        describe("Test case for LDEV-5632", function() {

            it( title="should load OpenTelemetry Span and get current span", body=function( currentSpec) {
                local.result = _InternalRequest(
					template:"#variables.uri#/LDEV5632.cfm");
                    // Set the java_opts or catalina_opts environment variable to match the expected agent options
                    //eg. java_opts=-javaagent:/LDEV5632/jars/dd-java-agent-1.49.0.jar -Ddd.trace.otel.enabled=true
                var agentOpts = "-javaagent:#expandPath('LDEV5632/jars/dd-java-agent-1.49.0.jar')# -Ddd.trace.otel.enabled=true";
                var envMatch = (
                                (structKeyExists(server.system.environment, "JAVA_OPTS") && server.system.environment["JAVA_OPTS"] == agentOpts)
                                ||
                                (structKeyExists(server.system.environment, "CATALINA_OPTS") && server.system.environment["CATALINA_OPTS"] == agentOpts)
                            );
                // Check if the environment variable or system property is set correctly
                expect(envMatch).toBeTrue();
                expect(result.filecontent.trim()).toBe("success");
            });

        });
    }
    private string function createURI(string calledName){
		var baseURI="/test/#listLast(getDirectoryFromPath(getCurrenttemplatepath()),"\/")#/";
		return baseURI&""&calledName;
	}

}