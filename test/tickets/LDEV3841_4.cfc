component extends="org.lucee.cfml.test.LuceeTestCase" {

	function beforeAll(){
		cfapplication(action='update', cgiReadOnly="true");
	}

	function afterAll(){
		cfapplication(action='update', cgiReadOnly="true");
	}

	function run( testResults, testBox ) {


	}

}
