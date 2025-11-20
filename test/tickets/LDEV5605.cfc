component extends="org.lucee.cfml.test.LuceeTestCase" skip=false {

	function run( testResults , testBox ) {
		describe( title="Test inline component exception has stacktrace", body=function() {

			it(title="test stacktrace for inline component", body = function( currentSpec ) {
				var z = new component {
					function test(){
						throw "ERROR IN INLINE COMPONENT METHOD"; // stack trace should start here
					}
				}
				var v = false;
				try {
					z.test();
				} catch ( e ){
					v = e;
				}
				expect( v ).toBeStruct();
				expect( v.tagContext[ 1 ].line ).toBe( 9 );
				expect( v.tagContext[ 1 ].template ).toInclude( "LDEV5605.cfc" );
			});

			it(title="test stacktrace for nested inline component", body = function( currentSpec ) {
				var outer = new component {
					function getInner(){
						return new component {
							function test(){
								throw "ERROR IN NESTED INLINE COMPONENT"; // line 28
							}
						};
					}
				}
				var inner = outer.getInner();
				var v = false;
				try {
					inner.test();
				} catch ( e ){
					v = e;
				}
				expect( v ).toBeStruct();
				expect( v.tagContext[ 1 ].line ).toBe( 28 );
				expect( v.tagContext[ 1 ].template ).toInclude( "LDEV5605.cfc" );
			});

			it(title="test stacktrace for sub component", body = function( currentSpec ) {
				var sub = new LDEV5605.SubComp$SubComp();
				var v = false;
				try {
					sub.test();
				} catch ( e ){
					v = e;
				}
				expect( v ).toBeStruct();
				expect( v.tagContext[ 1 ].line ).toBe( 9 );
				expect( v.tagContext[ 1 ].template ).toInclude( "SubComp.cfc" );
			});
	
		});
	}

}