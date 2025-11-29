component extends="org.lucee.cfml.test.LuceeTestCase" labels="config" {

	function beforeAll() {
		variables.admin = new org.lucee.cfml.Administrator( "server", request.ServerAdminPassword );
	}

	function run( testResults, testBox ) {
		describe( "test case for LDEV-5943", function() {

			it( title="inspectTemplate setting should persist", body=function( currentSpec ) {
				var original = admin.getPerformanceSettings().inspectTemplate;
				try {
					// set to a different value
					var newValue = ( original == "once" ) ? "never" : "once";
					admin.updatePerformanceSettings( inspectTemplate=newValue );
					var updated = admin.getPerformanceSettings().inspectTemplate;
					expect( updated ).toBe( newValue );
				}
				finally {
					// restore original value
					admin.updatePerformanceSettings( inspectTemplate=original );
					var restored = admin.getPerformanceSettings().inspectTemplate;
					expect( restored ).toBe( original );
				}
			});

			it( title="all inspectTemplate values should work", body=function( currentSpec ) {
				var original = admin.getPerformanceSettings().inspectTemplate;
				var values = [ "auto", "never", "once", "always" ];
				try {
					for ( var val in values ) {
						admin.updatePerformanceSettings( inspectTemplate=val );
						var current = admin.getPerformanceSettings().inspectTemplate;
						expect( current ).toBe( val );
					}
				}
				finally {
					// restore original value
					admin.updatePerformanceSettings( inspectTemplate=original );
				}
			});

		});
	}

}
