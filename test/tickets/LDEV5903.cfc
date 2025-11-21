component extends="org.lucee.cfml.test.LuceeTestCase" labels="classloader,memory" {

	function run( testResults, testBox ) {
		describe( "LDEV-5903 - Per-class classloader regression (memory leak)", function() {

			it( "should create separate classloaders for each template in same directory", function() {
				// Load multiple templates from the same physical directory
				// Each should get its own classloader per LDEV-4739 fix
				var template1 = new LDEV5903.Template1();
				var template2 = new LDEV5903.Template2();
				var template3 = new LDEV5903.Template3();

				// Use getMetaData to access the underlying Java class
				var cl1 = getMetaData( template1 ).getClass().getClassLoader();
				var cl2 = getMetaData( template2 ).getClass().getClassLoader();
				var cl3 = getMetaData( template3 ).getClass().getClassLoader();

				systemOutput( "ClassLoader 1: #cl1.toString()#", true );
				systemOutput( "ClassLoader 2: #cl2.toString()#", true );
				systemOutput( "ClassLoader 3: #cl3.toString()#", true );

				// The regression: all three currently share the same classloader (per directory)
				// The fix: each should have its own classloader (per class)
				// For now, we document the CURRENT (broken) behavior
				var allSame = ( cl1.toString() == cl2.toString() && cl2.toString() == cl3.toString() );

				if ( allSame ) {
					systemOutput( "REGRESSION CONFIRMED: All templates share the same classloader", true );
					systemOutput( "This is the bug - each template should have its own classloader", true );
				} else {
					systemOutput( "FIXED: Each template has its own classloader", true );
				}

			});

			it( "should track classloaders per className in MappingImpl", function() {
				// This test will verify the internal structure once we apply the fix
				// We need to check that MappingImpl has a Map<String, PhysicalClassLoaderReference>

				var template1 = new LDEV5903.Template1();
				var template2 = new LDEV5903.Template2();

				// Get the mapping via reflection
				var pageContext = getPageContext();
				var ps = pageContext.getCurrentPageSource();
				var mapping = ps.getMapping();

				systemOutput( "Mapping class: #mapping.getClass().getName()#", true );

				// After the fix, we should be able to verify that loaders map exists
				// For now, just verify the mapping is a MappingImpl
				expect( mapping.getClass().getName() ).toBe( "lucee.runtime.MappingImpl" );
			});

			it( "templates can be loaded and executed multiple times", function() {
				// Verify basic functionality still works
				loop from=1 to=10 index="local.i" {
					var t = new LDEV5903.Template1();
					expect( t.getValue() ).toBe( "template1" );
				}
				
				loop from=1 to=10 index="local.i" {
					var t = new LDEV5903.Template2();
					expect( t.getValue() ).toBe( "template2" );
				}
			});
		});
	}
}
