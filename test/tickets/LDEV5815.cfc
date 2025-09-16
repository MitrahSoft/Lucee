component extends="org.lucee.cfml.test.LuceeTestCase" skip=true {

    function run(testResults, textbox)    {
        describe(title = "expandPath() absolute path behavior on Linux", body = function() {

            it(title = "should resolve relative path to current template directory", skip="#isLinux()#", body = function(currentSpec) {
                // Only run this test on Linux
                var relPath = "LDEV5815.log";
                var expectedDir = getDirectoryFromPath(getCurrentTemplatePath());
                var expanded = expandPath(relPath);

                expect( expanded.left( len(expectedDir) ) ).toBe( expectedDir );
            });

        });  
    }
    function isLinux() {
		return server.os.name != "Linux";
	}
}