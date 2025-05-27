component extends="org.lucee.cfml.test.LuceeTestCase" {

    function run(testResults, testBox) {
        describe( title="Testcase for LDEV-5627: getClass() resolution order", body=function() {

            it( title="uses custom getClass() function if defined", body=function( currentSpec ) {
                var obj = new ldev5627.CustomGetClassMethod();
                var result = obj.getClass();
                expect(result).toBe("test_getClass");
            } );

            it( title="uses property getter when 'class' property with accessor=true exists", body=function( currentSpec ) {
                var obj = new ldev5627.ClassPropertyAccessor();
                var result = obj.getClass();
                expect(result).toBe("test_getClass"); 
            } );

            it( title="uses Java proxy class when no custom function or property exist", body=function( currentSpec ) {
                var obj = new ldev5627.DefaultProxyBehavior();
                var result = obj.getClass();
                // The result should be a Java proxy class object
                expect(isObject(result)).toBeTrue();
            });

        });
    }

}