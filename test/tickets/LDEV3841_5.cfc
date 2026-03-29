component extends="org.lucee.cfml.test.LuceeTestCase" {

	function beforeAll(){
		cfapplication(action='update', cgiReadOnly="true");
	}

	function afterAll(){
		cfapplication(action='update', cgiReadOnly="true");
	}

	function run( testResults, testBox ) {


	}

	private string function createURI(string calledName, boolean contract=true){
		var base = getDirectoryFromPath( getCurrentTemplatePath() );
		var baseURI = contract ? contractPath( base ) : "/test/#listLast(base,"\/")#";
		return baseURI & "/" & calledName;
	}

	private string function cgiReadOnlyTest( required numeric scene ) {
		try {
			if (scene == 1) cfapplication(name="LDEV-3841", action='update', cgiReadOnly="false");
			if (scene == 2) cfapplication(action='update');
			if (scene == 3) cfapplication(action='update', cgiReadOnly="true");
			CGI.foo = "writable:#scene#";
			return CGI.foo;
		}
		catch(any e) {
			// systemOutput(e.message, true);
			return "readonly:#scene#";
		}
	}
}
