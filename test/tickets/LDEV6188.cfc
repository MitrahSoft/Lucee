component extends="org.lucee.cfml.test.LuceeTestCase" {

	function afterAll() {
		application action="update" nullSupport=false;
	}

	function run( testResults, testBox ) {

		describe( title="LDEV-6188 nullSupport=true: optional args should exist in arguments scope", body=function() {

			beforeEach( function( currentSpec, data ) {
				application action="update" nullSupport=true;
			});

			afterEach( function( currentSpec, data ) {
				application action="update" nullSupport=false;
			});

			it( title="positional call, no defaults — args should exist as null", skip=true, body=function() {
				var args = _noDefaults();
				expect( args ).toHaveKey( "arg1" );
				expect( args ).toHaveKey( "arg2" );
				expect( isNull( args.arg1 ) ).toBeTrue();
				expect( isNull( args.arg2 ) ).toBeTrue();
			});

			it( title="positional call, typed args, no defaults — args should exist as null", skip=true, body=function() {
				var args = _typedNoDefaults();
				expect( args ).toHaveKey( "memberId" );
				expect( args ).toHaveKey( "name" );
				expect( isNull( args.memberId ) ).toBeTrue();
				expect( isNull( args.name ) ).toBeTrue();
			});

			it( title="argumentCollection call, no defaults — args should exist as null", skip=true, body=function() {
				var args = _proxyNoDefaults();
				expect( args ).toHaveKey( "arg1" );
				expect( args ).toHaveKey( "arg2" );
				expect( isNull( args.arg1 ) ).toBeTrue();
				expect( isNull( args.arg2 ) ).toBeTrue();
			});

			it( title="positional call, with defaults — args should use defaults", body=function() {
				var args = _withDefaults();
				expect( args ).toHaveKey( "arg1" );
				expect( args ).toHaveKey( "arg2" );
				expect( args.arg1 ).toBe( "default1" );
				expect( args.arg2 ).toBe( "default2" );
			});

			it( title="argumentCollection call, with defaults — args should use defaults", body=function() {
				var args = _proxyWithDefaults();
				expect( args ).toHaveKey( "arg1" );
				expect( args ).toHaveKey( "arg2" );
				expect( args.arg1 ).toBe( "default1" );
				expect( args.arg2 ).toBe( "default2" );
			});

			it( title="partial args passed — missing optional should exist as null", skip=true, body=function() {
				var args = _noDefaults( "passed" );
				expect( args ).toHaveKey( "arg1" );
				expect( args ).toHaveKey( "arg2" );
				expect( args.arg1 ).toBe( "passed" );
				expect( isNull( args.arg2 ) ).toBeTrue();
			});

			it( title="partial args via argumentCollection — missing optional should exist as null", skip=true, body=function() {
				var args = _proxyNoDefaults( "passed" );
				expect( args ).toHaveKey( "arg1" );
				expect( args ).toHaveKey( "arg2" );
				expect( args.arg1 ).toBe( "passed" );
				expect( isNull( args.arg2 ) ).toBeTrue();
			});

		});

		describe( title="LDEV-6188 nullSupport=false: optional args should not exist (control tests)", body=function() {

			beforeEach( function( currentSpec, data ) {
				application action="update" nullSupport=false;
			});

			it( title="positional call, no defaults — keys should not exist", body=function() {
				var args = _noDefaults();
				expect( args ).notToHaveKey( "arg1" );
				expect( args ).notToHaveKey( "arg2" );
			});

			it( title="positional call, with defaults — keys should exist with defaults", body=function() {
				var args = _withDefaults();
				expect( args ).toHaveKey( "arg1" );
				expect( args.arg1 ).toBe( "default1" );
			});

		});

	}

	// helper functions

	private function _noDefaults( arg1, arg2 ) {
		return arguments;
	}

	private function _typedNoDefaults( Numeric memberId, String name ) {
		return arguments;
	}

	private function _withDefaults( arg1="default1", arg2="default2" ) {
		return arguments;
	}

	private function _proxyNoDefaults( arg1, arg2 ) {
		return _noDefaults( argumentCollection=arguments );
	}

	private function _proxyWithDefaults( arg1, arg2 ) {
		return _withDefaults( argumentCollection=arguments );
	}

}
