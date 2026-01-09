component extends="org.lucee.cfml.test.LuceeTestCase" labels="esapi"{
    function run( testResults , testBox ) {
        describe( "test case for EncodeForHTMLAttribute", function() {
            
            it( "encodes quotes consistently across libraries", function() {
                // Testing a double quote "
                // Both ESAPI and OWASP will convert this to &quot;
                var input = ' " ';
                var enc = EncodeForHTMLAttribute(input);
                
				var isEncoded=findNoCase( "&quot;", enc ) || findNoCase( "&##34;", enc );

                expect( isEncoded ).toBeTrue( "Expected [#enc#] to contain either &quot; or &##34;" );
            });

            it( "encodes angle brackets consistently (Testing <)", function() {
                // Both libraries encode the opening bracket < to &lt;
                var enc = EncodeForHTMLAttribute('<');
                assertEquals('&lt;', enc);
            });
        }); 
    }
}