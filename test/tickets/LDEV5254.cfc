component extends = "org.lucee.cfml.test.LuceeTestCase" skip="true" {
	function run( testResults, testBox ) {
		describe( "test query duplicate for LDEV-5254 ", function() {
			it( "test query duplicate", function() {
				qry1 = querynew("a,b", "integer,integer");
				queryaddrow(qry1);
				querysetcell(qry1, "a", 1);
				querysetcell(qry1, "b", 2);

				qry2 = duplicate(qry1);
				queryaddrow(qry2);
				querysetcell(qry2, "a", 4);
				querysetcell(qry2, "b", 5);

				expect( querycolumndata(qry2, "a").len() ).toBe( 2 );
				expect( querycolumndata(qry1, "a").len() ).toBe( 1 );
			});
		});
	}
}
