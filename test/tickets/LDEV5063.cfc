component extends="org.lucee.cfml.test.LuceeTestCase" labels="java,proxy" {

	function run( testResults, testBox ) {
		describe( "LDEV-5063 - PhysicalClassLoader proxy class loading", function() {

			it( "can create Java proxy from component implementing interface", function() {
				// Create a component that implements a Java interface
				var testCFC = new LDEV5063.TestComponent();

				// This should work - creating a Java proxy from the component
				var proxy = createDynamicProxy( testCFC, [ "java.lang.Runnable" ] );

				expect( proxy ).toBeInstanceOf( "java.lang.Runnable" );
			});

			it( "can load proxy classes from PhysicalClassLoader", function() {
				// Create multiple proxies to test classloader behavior
				var testCFC1 = new LDEV5063.TestComponent();
				var testCFC2 = new LDEV5063.TestComponent();

				var proxy1 = createDynamicProxy( testCFC1, [ "java.lang.Runnable" ] );
				var proxy2 = createDynamicProxy( testCFC2, [ "java.lang.Runnable" ] );

				// Both should be valid instances
				expect( proxy1 ).toBeInstanceOf( "java.lang.Runnable" );
				expect( proxy2 ).toBeInstanceOf( "java.lang.Runnable" );

				// They should be different instances
				expect( proxy1 ).notToBe( proxy2 );
			});

			it( "proxy methods are callable", function() {
				var testCFC = new LDEV5063.TestComponent();
				var proxy = createDynamicProxy( testCFC, [ "java.lang.Runnable" ] );

				// Should be able to call the run() method without error
				proxy.run();

				expect( testCFC.getWasCalled() ).toBe( true );
			});
		});
	}
}
