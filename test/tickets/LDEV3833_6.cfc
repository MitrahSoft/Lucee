component extends="org.lucee.cfml.test.LuceeTestCase" {
	function run( testResults,testBox ) {
		describe("Testcase for LDEV-3833", function() {
			it( title="Checking runAsnyc() to get the applictionContext and scopes from pageContext", body=function( currentSpec ) {
				
				systemOutput("xxxxxxxxxxxx",1,1);
				systemOutput(expandpath("./test"),1,1);
				systemOutput(directoryExists (expandpath("./test")),1,1);
				if(!directoryExists (expandpath("./test"))) directoryCreate(expandpath("./test"));
				
				cfapplication (name="LDEV3833", mappings={"/test":expandpath("./test")});
				
				
			});


		}); 
	}

	function afterAll(){
		// structDelete(request, "testReq");
		// structDelete(request, "testReqAsync");
	};
}