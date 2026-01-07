component extends="org.lucee.cfml.test.LuceeTestCase" labels="guard" {

    function run( testResults , testBox ) {
        describe( title = "Guard: Canonicalize", body = function() {
            
            it( "reduces double-encoded characters (Standard Unmasking)", function() {
                // %25 is '%', so %253c becomes %3c which becomes <
                var input = "Hello %253cscript%253e";
                expect( canonicalize( input, false, false ) ).toBe( "Hello <script>" );
            });

            it( "detects multiple encoding and throws an exception", function() {
                var input = "%25252522"; // Triple encoded "
                
                expect( function(){
                    canonicalize( input, true, false );
                }).toThrow( type="java.lang.SecurityException" );
            });

            it( "preserves the plus sign (No URL-space conversion)", function() {
                var input = "1+1=2";
                // Our Lucee-native decoder ensures + does not become a space
                expect( canonicalize( input, false, false ) ).toBe( "1+1=2" );
            });

            it( "strips unsafe characters when simplify is enabled", function() {
                // The input contains ^ and \ which our isSafe() method excludes
                var input = "Safe!@## but ^ and \ are not.";
                var expected = "Safe!@## but  and  are not."; 
                
                // Third argument is 'simplify'
                expect( canonicalize( input, false, true ) ).toBe( expected );
            });

            it( "preserves all special characters when simplify is disabled", function() {
                var input = "!@##$^&*()_+{}[]:;''<>, .?/|\~`.";
                
                // When simplify is false, carets and backslashes remain
                expect( canonicalize( input, false, false ) ).toBe( input );
            });

            it( "handles mixed HTML and URL encoding", function() {
                // %26 is '&', so %26lt; becomes &lt; which becomes <
                var input = "Mixed: %26lt;script%26gt;";
                expect( canonicalize( input, false, false ) ).toBe( "Mixed: <script>" );
            });

        });
    }
}