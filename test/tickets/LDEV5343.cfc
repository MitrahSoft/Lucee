component extends="org.lucee.cfml.test.LuceeTestCase" skip=true {

    function run(testResults, testBox) {
        describe( title = "Test case for LDEV-5343 Null coalescing operator (??)", body = function() {
            var defaultValue = "lucee";
            var initialValue = "CFML";
            it( title = "should return the default value when the left side is null", body = function( currentSpec ) {
                expect(javacast("null","") ?? defaultValue).toBe("lucee");
            });

            it( title = "should return the default value when the variable is undefined", body = function( currentSpec ) {
                expect( undefinedVar ?? defaultValue ).toBe("lucee");
            });

            it( title = "should return the left value when it is not null", body = function( currentSpec ) {
                expect( initialValue ?? defaultValue ).toBe("CFML");
            });

        });
    }

}