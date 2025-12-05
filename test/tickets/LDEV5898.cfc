component extends="org.lucee.cfml.test.LuceeTestCase" labels="ast" {

	function run( testResults, testBox ) {
		describe( "LDEV-5898: For.dump() should handle null init/condition/update", function() {

			it( title="astFromPath() should work with for loop with null init", body=function() {
				var testFile = getDirectoryFromPath( getCurrentTemplatePath() ) & "LDEV5898/for-null-init.cfm";
				var ast = astFromPath( testFile );
				expect( ast ).toBeStruct();
				expect( ast.type ).toBe( "Program" );
			});

			it( title="astFromPath() should work with for loop with null condition", body=function() {
				var testFile = getDirectoryFromPath( getCurrentTemplatePath() ) & "LDEV5898/for-null-condition.cfm";
				var ast = astFromPath( testFile );
				expect( ast ).toBeStruct();
				expect( ast.type ).toBe( "Program" );
			});

			it( title="astFromPath() should work with for loop with null update", body=function() {
				var testFile = getDirectoryFromPath( getCurrentTemplatePath() ) & "LDEV5898/for-null-update.cfm";
				var ast = astFromPath( testFile );
				expect( ast ).toBeStruct();
				expect( ast.type ).toBe( "Program" );
			});

		});
	}

}
