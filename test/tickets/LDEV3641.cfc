component extends="org.lucee.cfml.test.LuceeTestCase" skip = true{
	function beforeAll(){
		variables.uri = createURI("LDEV3641");
	}
	function run( testResults, testBox ) {
		describe("Testcase for LDEV-3641", function() {
			it( title="ORMExecuteQuery() - HQL with positional parameters", body=function( currentSpec ) {
				local.result = _InternalRequest(
					template : "#uri#\test.cfm",
					forms : { Scene = 1 }
				);
				expect(trim(result.filecontent)).toBe("lucee");
			});
			it( title="ORMExecuteQuery() - HQL with JPA positional parameters", body=function( currentSpec ) {
				local.result = _InternalRequest(
					template : "#uri#\test.cfm",
					forms : { Scene = 2 }
					);
				expect(trim(result.filecontent)).toBe("lucee");
			});
		});
	}
	private string function createURI(string calledName){
		var baseURI = "/test/#listLast(getDirectoryFromPath(getCurrenttemplatepath()),"\/")#/";
		return baseURI&""&calledName;
	}
}