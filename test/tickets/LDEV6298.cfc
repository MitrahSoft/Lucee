component extends="org.lucee.cfml.test.LuceeTestCase" {

	function run( testResults, testBox ){
		describe( title="LDEV-6298 share flyweight property accessors across Component duplicates", body=function(){

			describe( title="runtime mutation isolation", body=function(){
				it( title="overriding accessor on duplicate doesn't leak to source", body=function( currentSpec ){
					var orig = new LDEV3335.testPropertyTypes();
					orig.setAge( 30 );
					var copy = duplicate( orig );
					copy.getAge = function() { return 999; };
					expect( copy.getAge() ).toBe( 999 );
					expect( orig.getAge() ).toBe( 30 );
				});
				it( title="overriding accessor on source doesn't leak to existing duplicate", body=function( currentSpec ){
					var orig = new LDEV3335.testPropertyTypes();
					var copy = duplicate( orig );
					orig.getAge = function() { return 777; };
					expect( orig.getAge() ).toBe( 777 );
					// duplicate must still use real accessor
					copy.setAge( 42 );
					expect( copy.getAge() ).toBe( 42 );
				});
				it( title="injecting new method on duplicate doesn't leak to source", body=function( currentSpec ){
					var orig = new LDEV3335.testWithAccessors();
					var copy = duplicate( orig );
					copy.customMethod = function() { return "from copy"; };
					expect( copy.customMethod() ).toBe( "from copy" );
					expect( function(){ orig.customMethod(); } ).toThrow();
				});
			});

			describe( title="mass duplication (ORM hot path)", body=function(){
				it( title="50 duplicates from one prototype — all isolated", body=function( currentSpec ){
					var prototype = new LDEV3335.testPropertyTypes();
					var entities = [];
					for ( var i=1; i<=50; i++ ) {
						var entity = duplicate( prototype );
						entity.setAge( i );
						entity.setName( "entity-" & i );
						arrayAppend( entities, entity );
					}
					for ( var i=1; i<=50; i++ ) {
						expect( entities[ i ].getAge() ).toBe( i );
						expect( entities[ i ].getName() ).toBe( "entity-" & i );
					}
				});
				it( title="duplicates remain isolated when prototype is mutated mid-flight", body=function( currentSpec ){
					var prototype = new LDEV3335.testWithAccessors();
					prototype.setA( "before" );
					var early = duplicate( prototype );
					prototype.setA( "after" );
					var late = duplicate( prototype );
					expect( early.getA() ).toBe( "before" );
					expect( late.getA() ).toBe( "after" );
				});
			});

			describe( title="duplicate of duplicate", body=function(){
				it( title="three-level chain — each level isolated", body=function( currentSpec ){
					var a = new LDEV3335.testWithAccessors();
					a.setA( "level-a" );
					var b = duplicate( a );
					b.setA( "level-b" );
					var c = duplicate( b );
					c.setA( "level-c" );
					expect( a.getA() ).toBe( "level-a" );
					expect( b.getA() ).toBe( "level-b" );
					expect( c.getA() ).toBe( "level-c" );
				});
			});

			describe( title="reflection metadata still correct", body=function(){
				it( title="getMetadata() on duplicate returns same property list as source", body=function( currentSpec ){
					var orig = new LDEV3335.testPropertyTypes();
					var copy = duplicate( orig );
					var origMeta = getMetadata( orig );
					var copyMeta = getMetadata( copy );
					expect( copyMeta.name ).toBe( origMeta.name );
					expect( arrayLen( copyMeta.properties ) ).toBe( arrayLen( origMeta.properties ) );
				});
			});

			describe( title="inheritance preserved across duplicate", body=function(){
				it( title="parent property accessor works on duplicated child", body=function( currentSpec ){
					var child = new LDEV3335.testInheritChild();
					var copy = duplicate( child );
					expect( copy.getParentProp() ).toBe( "from parent" );
					copy.setParentProp( "set on copy" );
					expect( copy.getParentProp() ).toBe( "set on copy" );
					expect( child.getParentProp() ).toBe( "from parent" );
				});
				it( title="child override of parent accessor preserved on duplicate", body=function( currentSpec ){
					var orig = new LDEV3335.testInheritChildOverride();
					var copy = duplicate( orig );
					expect( copy.getParentProp() ).toBe( "CHILD OVERRIDE" );
				});
			});
		});
	}
}
