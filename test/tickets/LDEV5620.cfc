component extends = "org.lucee.cfml.test.LuceeTestCase"{

    function run( testResults , testBox ) {
        describe( "Testcase for LDEV-5620", function() {

            it( title = "should load the internal Java class without error",  body = function( CurrentSpec ) {
                var result = "";
                try {
                    result = createObject("java", "org.mindrot.jbcrypt.BCrypt", expandPath("./LDEV5620/test.jar"));
                } catch (any e) {
                    result = e.message;
                }
                expect( isObject(result) ).toBeTrue();
                var className = result.getClass().getName();
                expect( className ).toBe( "org.mindrot.jbcrypt.BCrypt" );
            });

        });
    }

}