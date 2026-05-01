component extends="org.lucee.cfml.test.LuceeTestCase" {

	// Generic CFC accessor contracts:
	//   - accessors="true" generates getX/setX from property declarations
	//   - manual getX/setX functions override the generated ones
	//   - typed properties cast on set
	//   - getter=false / setter=false suppress generation
	//   - duplicate() preserves accessor functionality, isolation, casting, mixed manual/auto
	// LDEV-6236's UDFGSProperty share-don't-clone optimization must keep all of this intact.

	function run( testResults, testBox ){
		describe( "CFC accessors", function(){

			describe( "basic accessor functionality", function(){
				it( title="basic accessors work", body=function( currentSpec ){
					var cfc = new accessors.testWithAccessors();
					cfc.setA("test value");
					expect(cfc.getA()).toBe("test value");
				});
				it( title="multiple instances are isolated", body=function( currentSpec ){
					var inst1 = new accessors.testWithAccessors();
					var inst2 = new accessors.testWithAccessors();
					var inst3 = new accessors.testWithAccessors();
					inst1.setA("inst1");
					inst2.setA("inst2");
					inst3.setA("inst3");
					expect(inst1.getA()).toBe("inst1");
					expect(inst2.getA()).toBe("inst2");
					expect(inst3.getA()).toBe("inst3");
				});
				it( title="default property values work", body=function( currentSpec ){
					var cfc = new accessors.testWithAccessors();
					expect(cfc.getA()).toBe("1");
				});
			});

			describe( "manual method overrides", function(){
				it( title="manual getter overrides auto-generated accessor", body=function( currentSpec ){
					var cfc = new accessors.testManualGetter();
					expect(cfc.getName()).toBe("UPPERCASE");
				});
				it( title="manual setter overrides auto-generated accessor", body=function( currentSpec ){
					var cfc = new accessors.testManualSetter();
					cfc.setValue(5);
					expect(cfc.getValue()).toBe(10); // manual setter doubles
				});
			});

			describe( "property attributes", function(){
				it( title="property types are respected (numeric)", body=function( currentSpec ){
					var cfc = new accessors.testPropertyTypes();
					expect(isNumeric(cfc.getAge())).toBeTrue();
				});
				it( title="property types are respected (boolean)", body=function( currentSpec ){
					var cfc = new accessors.testPropertyTypes();
					expect(isBoolean(cfc.getActive())).toBeTrue();
				});
				it( title="getter=false / setter=false flags respected", body=function( currentSpec ){
					var cfc = new accessors.testGetterSetterFlags();
					expect(cfc.getReadOnly()).toBe("readonly value");
					expect(function(){
						cfc.setReadOnly("new");
					}).toThrow();
				});
			});

			describe( "duplicate() preserves accessor behaviour", function(){
				it( title="duplicate() preserves accessor functionality", body=function( currentSpec ){
					var orig = new accessors.testPropertyTypes();
					orig.setAge( 25 );
					orig.setName( "original" );
					var copy = duplicate( orig );
					expect( copy.getAge() ).toBe( 25 );
					expect( copy.getName() ).toBe( "original" );
					copy.setAge( 99 );
					copy.setName( "copy" );
					expect( orig.getAge() ).toBe( 25 );
					expect( orig.getName() ).toBe( "original" );
					expect( copy.getAge() ).toBe( 99 );
					expect( copy.getName() ).toBe( "copy" );
				});
				it( title="duplicate() preserves typed property casting", body=function( currentSpec ){
					var orig = new accessors.testPropertyTypes();
					var copy = duplicate( orig );
					copy.setAge( "42" );
					expect( copy.getAge() ).toBe( 42 );
					expect( isNumeric( copy.getAge() ) ).toBeTrue();
				});
				it( title="duplicate() with manual override - override preserved", body=function( currentSpec ){
					var orig = new accessors.testManualSetter();
					var copy = duplicate( orig );
					copy.setValue( 5 );
					expect( copy.getValue() ).toBe( 10 );
				});
				it( title="multiple duplicates are fully isolated", body=function( currentSpec ){
					var orig = new accessors.testWithAccessors();
					orig.setA( "v1" );
					var copy1 = duplicate( orig );
					var copy2 = duplicate( orig );
					copy1.setA( "c1" );
					copy2.setA( "c2" );
					expect( orig.getA() ).toBe( "v1" );
					expect( copy1.getA() ).toBe( "c1" );
					expect( copy2.getA() ).toBe( "c2" );
				});
				it( title="mixed generated and manual accessors on same component", body=function( currentSpec ){
					var orig = new accessors.testManualGetter();
					orig.setName( "hello" );
					expect( orig.getName() ).toBe( "HELLO" );
					var copy = duplicate( orig );
					expect( copy.getName() ).toBe( "HELLO" );
					copy.setName( "world" );
					expect( copy.getName() ).toBe( "WORLD" );
					expect( orig.getName() ).toBe( "HELLO" );
				});
				it( title="mixed generated getter and manual setter on same component", body=function( currentSpec ){
					var orig = new accessors.testManualSetter();
					orig.setValue( 5 );
					expect( orig.getValue() ).toBe( 10 );
					var copy = duplicate( orig );
					expect( copy.getValue() ).toBe( 10 );
					copy.setValue( 7 );
					expect( copy.getValue() ).toBe( 14 );
					expect( orig.getValue() ).toBe( 10 );
				});
				it( title="setter chaining works on duplicate", body=function( currentSpec ){
					var orig = new accessors.testPropertyTypes();
					var copy = duplicate( orig );
					var result = copy.setAge( 30 );
					expect( result.getName() ).toBe( "John" );
				});
			});

		});
	}
}
