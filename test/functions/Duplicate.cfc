component extends="org.lucee.cfml.test.LuceeTestCase" labels="xml" {
	function run( testResults , testBox ) {
		describe( "test case for Duplicate", function() {
			it(title = "Checking with Duplicate", body = function( currentSpec ) {
		 		cfapplication (action="update" clientmanagement="true");
				<!--- begin old test code --->
				<!--- String --->
					var str="String";
					variables.test={};
					var str2=duplicate(str);
					var str="String 2";
					assertEquals("String", "#str2#");
				<!--- Number --->
					str=1+1;
					str2=duplicate(str);
					str=str+1;
					assertEquals("2", "#str2#");
				<!--- boolean --->
					str=true;
					str2=duplicate(str);
					str=false;
					assertEquals("true", "#str2#");
				<!--- struct --->
					str=structNew();
					str.data="aaaaa";
					str2=duplicate(str);
					str.data="bbbbb";
					assertEquals("aaaaa", "#str2.data#");
				<!--- array --->
					str=arrayNew(1);
					str[1]="aaaaa";
					str2=duplicate(str);
					str[1]="bbbbb";
					assertEquals("aaaaa", "#str2[1]#");
				<!--- query --->
					var qry=queryNew("col");
					QueryAddRow(qry);
					QuerySetCell(qry,"col","aaaaa");
					var qry2=duplicate(qry);
					QuerySetCell(qry,"col","bbbbb");
					assertEquals("aaaaa", "#qry2.col#");
				if(server.ColdFusion.ProductName eq "RAILO"){
					cfobject(type="component",name="c",component="duplicate.comps.some.Hello");
					assertEquals("0", "#c.get()#");
					var d=duplicate(c);
					local.c.set(1);
					assertEquals("1", "#c.get()#");
					assertEquals("0", "#d.get()#");
				}
				<!--- not working in JSR223env --->
				if(server.lucee.environment=="servlet"){
					duplicate(client);
					duplicate(session);
					duplicate(application);
					duplicate(cgi);
				}
				duplicate(request);
				// duplicate(variables);
				duplicate(server);

				savecontent variable="local.xrds"{
					writeOutput('<?xml version="1.0" encoding="UTF-8"?><xrd><Service priority="10"><Type>http://openid.net/signon/1.0</Type><URI priority="15">http://resolve2.example.com</URI><URI priority="10">http://resolve.example.com</URI><URI>https://resolve.example.com</URI></Service></xrd>');
				}
				var xrds = xmlParse(trim(xrds)).xmlRoot;
				var xrdsService = xrds.xmlChildren[1];
				xrdsService.xmlChildren[2] = duplicate(xrdsService.URI[2]);
				xrdsService.URI[1] = duplicate(xrdsService.URI[2]);
				<!--- end old test code --->
			});

			describe( "duplicate of a CFC — per-instance state isolation", function(){
				it( title="cfc.variables.x assigned then duplicated — duplicate has independent value", body=function( currentSpec ){
					var orig = new Duplicate.Tagged();
					orig.variables.adhoc = "orig-adhoc";
					var dup = duplicate( orig );
					expect( dup.variables.adhoc ).toBe( "orig-adhoc" );
					dup.variables.adhoc = "dup-adhoc";
					expect( orig.variables.adhoc ).toBe( "orig-adhoc" );
					expect( dup.variables.adhoc ).toBe( "dup-adhoc" );
				});
				it( title="cfc.this.x assigned then duplicated — duplicate has independent value", body=function( currentSpec ){
					var orig = new Duplicate.Tagged();
					orig.adhoc = "orig-this";
					var dup = duplicate( orig );
					expect( dup.adhoc ).toBe( "orig-this" );
					dup.adhoc = "dup-this";
					expect( orig.adhoc ).toBe( "orig-this" );
					expect( dup.adhoc ).toBe( "dup-this" );
				});
			});

			describe( "ThreadLocalDuplication re-entry (self-referential / cyclic CFCs)", function(){
				// Identity is asserted via mutation-through-reference rather than equality compare,
				// because TestBox's toBe() recurses through CFC content and stack-overflows on cycles.
				it( title="duplicate of CFC holding reference to itself doesn't infinite-loop", body=function( currentSpec ){
					var c = new Duplicate.Tagged( tag="root" );
					c.self = c;
					var d = duplicate( c );
					expect( d.tag ).toBe( "root" );
					d.marker = "before";
					d.self.marker = "via-self";
					expect( d.marker ).toBe( "via-self" );
					expect( structKeyExists( c, "marker" ) ).toBeFalse();
				});
				it( title="duplicate of two-CFC cycle (a.peer = b, b.peer = a) — both duplicates form a cycle of their own", body=function( currentSpec ){
					var a = new Duplicate.Tagged( tag="a" );
					var b = new Duplicate.Tagged( tag="b" );
					a.peer = b;
					b.peer = a;
					var dupA = duplicate( a );
					expect( dupA.tag ).toBe( "a" );
					expect( dupA.peer.tag ).toBe( "b" );
					dupA.peer.peer.marker = "via-cycle";
					expect( dupA.marker ).toBe( "via-cycle" );
					expect( structKeyExists( a, "marker" ) ).toBeFalse();
					expect( structKeyExists( b, "marker" ) ).toBeFalse();
				});
				it( title="concurrent self-referential duplicates — ThreadLocal scoped per thread", body=function( currentSpec ){
					var threadCount = 20;
					var threadNames = [];
					for ( var t=1; t<=threadCount; t++ ) {
						var tname = "duplicate-reentry-" & t;
						arrayAppend( threadNames, tname );
						thread name="#tname#" tid=t {
							var c = new Duplicate.Tagged( tag="t-" & attributes.tid );
							c.self = c;
							var d = duplicate( c );
							d.self.marker = "m-" & attributes.tid;
							thread.tagOut = d.tag;
							thread.markerOut = d.marker;
							thread.sourceUntouched = !structKeyExists( c, "marker" );
						}
					}
					thread action="join" name="#arrayToList( threadNames )#";
					for ( var t=1; t<=threadCount; t++ ) {
						var tname = "duplicate-reentry-" & t;
						var th = cfthread[ tname ];
						if ( th.status != "COMPLETED" ) throw( object=th.error );
						expect( th.tagOut ).toBe( "t-" & t );
						expect( th.markerOut ).toBe( "m-" & t );
						expect( th.sourceUntouched ).toBeTrue();
					}
				});
			});

		});
	}
}