component extends="org.lucee.cfml.test.LuceeTestCase" labels="java,classloader" {

	function beforeAll() {
		variables.tempDir = getTempDirectory() & "LDEV6226/";
		if ( directoryExists( variables.tempDir ) ) directoryDelete( variables.tempDir, true );
		directoryCreate( variables.tempDir );
		directoryCreate( variables.tempDir & "src/" );
		directoryCreate( variables.tempDir & "classes1/" );
		directoryCreate( variables.tempDir & "classes2/" );

		var javaSource = 'public class LDEV6226Helper {
	private String value;
	public LDEV6226Helper() { this.value = "default"; }
	public LDEV6226Helper( String value ) { this.value = value; }
	public LDEV6226Helper( LDEV6226Helper other ) { this.value = other.getValue(); }
	public String getValue() { return this.value; }
	public String extractFrom( LDEV6226Helper other ) { return other.getValue(); }
}';

		fileWrite( variables.tempDir & "src/LDEV6226Helper.java", javaSource );

		var javac = createObject( "java", "javax.tools.ToolProvider" ).getSystemJavaCompiler();
		if ( isNull( javac ) )
			throw( message="JDK required - javac not available", type="application" );

		var result = javac.run(
			javaCast( "null", "" ), javaCast( "null", "" ), javaCast( "null", "" ),
			[ "-d", variables.tempDir & "classes1", variables.tempDir & "src/LDEV6226Helper.java" ]
		);
		if ( result != 0 )
			throw( message="javac compilation failed with exit code #result#", type="application" );
		fileCopy(
			variables.tempDir & "classes1/LDEV6226Helper.class",
			variables.tempDir & "classes2/LDEV6226Helper.class"
		);
	}

	private function createFileClassLoader( required string classDir ) {
		var file = createObject( "java", "java.io.File" ).init( arguments.classDir );
		var urlArray = createObject( "java", "java.lang.reflect.Array" )
			.newInstance( createObject( "java", "java.net.URL" ).getClass(), 1 );
		urlArray[ 1 ] = file.toURI().toURL();
		// null parent classloader forces each instance to load its own copy of the class,
		// giving us two Class objects with the same FQN but different identity (simulates OSGi bundle refresh)
		return createObject( "java", "java.net.URLClassLoader" )
			.init( urlArray, javaCast( "null", "" ) );
	}

	private function getDynamicInvokerClazz( required clazz ) {
		var di = createObject( "java", "lucee.transformer.dynamic.DynamicInvoker" ).getExistingInstance();
		if ( isNull( di ) )
			throw( message="DynamicInvoker not initialised in this context", type="application" );
		return di.toClazz( arguments.clazz );
	}

	function run( testResults, testBox ) {
		describe( "LDEV-6226 - Java method resolution fails with cross-classloader arguments", function() {

			it( "isAssignableFrom fails across classloaders but name-based matching works", function() {
				var cl1 = createFileClassLoader( variables.tempDir & "classes1/" );
				var cl2 = createFileClassLoader( variables.tempDir & "classes2/" );

				try {
					var class1 = cl1.loadClass( "LDEV6226Helper" );
					var class2 = cl2.loadClass( "LDEV6226Helper" );

					// same name, different classloaders
					expect( class1.getName() ).toBe( class2.getName() );
					expect( class1.getClassLoader().toString() )
						.notToBe( class2.getClassLoader().toString() );

					// strict identity check fails - root cause of the bug
					expect( class1.isAssignableFrom( class2 ) ).toBeFalse();

					// name-based check works
					var Reflector = createObject( "java", "lucee.runtime.reflection.Reflector" );
					expect( Reflector.isInstaneOf( class2, class1, false ) ).toBeTrue();
				}
				finally {
					cl1.close();
					cl2.close();
				}
			});

			it( "Clazz.getMethod should find methods with cross-classloader arguments", function() {
				// simulates the real scenario: receiver persisted from old classloader,
				// argument created from new classloader after bundle refresh
				var cl1 = createFileClassLoader( variables.tempDir & "classes1/" );
				var cl2 = createFileClassLoader( variables.tempDir & "classes2/" );

				try {
					var class1 = cl1.loadClass( "LDEV6226Helper" );
					var class2 = cl2.loadClass( "LDEV6226Helper" );

					// create an argument from cl2 (fresh classloader, like after bundle refresh)
					var ctor = class2.getConstructor([ createObject( "java", "java.lang.String" ).getClass() ]);
					var foreignArg = ctor.newInstance([ "test-value" ]);

					// resolve method on class1 (old classloader, like persisted receiver)
					// this failed before the fix with: "No matching method for
					// LDEV6226Helper.extractFrom(LDEV6226Helper) found."
					var clazzz = getDynamicInvokerClazz( class1 );
					var method = clazzz.getMethod( "extractFrom", [ foreignArg ], true, true, true );
					expect( method ).notToBeNull();
					expect( method.getName() ).toBe( "extractFrom" );
				}
				finally {
					cl1.close();
					cl2.close();
				}
			});

			it( "Clazz.getConstructor should resolve with cross-classloader arguments", function() {
				var cl1 = createFileClassLoader( variables.tempDir & "classes1/" );
				var cl2 = createFileClassLoader( variables.tempDir & "classes2/" );

				try {
					var class1 = cl1.loadClass( "LDEV6226Helper" );
					var class2 = cl2.loadClass( "LDEV6226Helper" );

					// create an instance from cl2 to use as a constructor arg
					var foreignArg = class2.getConstructor([ createObject( "java", "java.lang.String" ).getClass() ])
						.newInstance([ "ctor-test" ]);

					// resolve constructor on class1 with a cross-classloader argument
					var clazzz = getDynamicInvokerClazz( class1 );
					var ctor = clazzz.getConstructor( [ foreignArg ], true, true, javaCast( "null", "" ) );
					expect( ctor ).notToBeNull();
				}
				finally {
					cl1.close();
					cl2.close();
				}
			});

		});
	}

}
