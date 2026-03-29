component extends="org.lucee.cfml.test.LuceeTestCase" {
	function run( testResults,testBox ) {
		describe("Testcase for LDEV-3833", function() {
			it( title="Checking runAsnyc() to get the applictionContext and scopes from pageContext", body=function( currentSpec ) {
				cfapplication (name="LDEV3833", mappings={"/test":expandpath("./test")});
				request.testReq = "testReq";
				application.testApp = "testApp";
				url.testURL = "testURL";
				form.testFORM = "testFORM";
				variables.testVar = "testVar";
				variables.result = runAsync(() => return [request, variables, application, url, form, getApplicationMetadata()]).get();
				sleep(50);
				expect(structKeyExists(result[1], "testReq")).toBeTrue();
				expect(structKeyExists(result[2], "testVar")).toBeTrue();
				expect(result[3].applicationname).toBe("LDEV3833");
				expect(structKeyExists(result[3], "testApp")).toBeTrue();
				expect(structKeyExists(result[6].mappings, "/test")).toBeTrue();
			});


		}); 
	}

	function afterAll(){
		structDelete(request, "testReq");
		structDelete(request, "testReqAsync");
	};
}