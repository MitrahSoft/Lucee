component extends="org.lucee.cfml.test.LuceeTestCase" labels="qoq" {

	function run( testResults, testBox ) {
		describe( title="LDEV-5845 - QoQ LEFT OUTER JOIN", body=function() {

			it( title="with table aliases", body=function( currentSpec ) {
				var Local_Count = queryNew( "completedon,id" );

				var Test_Table_1 = queryExecute(
					"SELECT CompletedOn FROM Local_Count",
					{},
					{ dbtype="query" }
				);

				var Test_Table_2 = queryExecute(
					"SELECT CompletedOn FROM Local_Count",
					{},
					{ dbtype="query" }
				);

				// This should not throw an error
				var Test_Join = queryExecute(
					"SELECT a.* FROM Test_Table_2 a LEFT OUTER JOIN Test_Table_2 b ON a.CompletedOn = b.CompletedOn",
					{},
					{ dbtype="query" }
				);

				expect( Test_Join ).toBeQuery();
				expect( Test_Join.recordCount ).toBe( 0 );
			});

			it( title="without table aliases", body=function( currentSpec ) {
				var qry1 = queryNew( "id,name", "integer,varchar", [ [ 1, "Alice" ], [ 2, "Bob" ] ] );
				var qry2 = queryNew( "id,dept", "integer,varchar", [ [ 1, "Sales" ] ] );

				var result = queryExecute(
					"SELECT qry1.id, qry1.name, qry2.dept FROM qry1 LEFT OUTER JOIN qry2 ON qry1.id = qry2.id",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 2 );
			});

		});

		describe( title="LDEV-5845 - QoQ LEFT JOIN (no OUTER)", body=function() {

			it( title="with table aliases", body=function( currentSpec ) {
				var qry1 = queryNew( "id,name", "integer,varchar", [ [ 1, "Alice" ], [ 2, "Bob" ] ] );
				var qry2 = queryNew( "id,dept", "integer,varchar", [ [ 1, "Sales" ] ] );

				var result = queryExecute(
					"SELECT a.id, a.name, b.dept FROM qry1 a LEFT JOIN qry2 b ON a.id = b.id",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 2 );
				expect( result.name[ 1 ] ).toBe( "Alice" );
				expect( result.dept[ 1 ] ).toBe( "Sales" );
				expect( result.name[ 2 ] ).toBe( "Bob" );
				expect( result.dept[ 2 ] ).toBe( "" ); // NULL becomes empty string
			});

			it( title="without table aliases", body=function( currentSpec ) {
				var qry1 = queryNew( "id,name", "integer,varchar", [ [ 1, "Alice" ], [ 2, "Bob" ] ] );
				var qry2 = queryNew( "id,dept", "integer,varchar", [ [ 1, "Sales" ] ] );

				var result = queryExecute(
					"SELECT qry1.id, qry1.name, qry2.dept FROM qry1 LEFT JOIN qry2 ON qry1.id = qry2.id",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 2 );
			});

		});

		describe( title="LDEV-5845 - QoQ INNER JOIN", body=function() {

			it( title="with table aliases", body=function( currentSpec ) {
				var qry1 = queryNew( "id,name", "integer,varchar", [ [ 1, "Alice" ], [ 2, "Bob" ] ] );
				var qry2 = queryNew( "id,dept", "integer,varchar", [ [ 1, "Sales" ] ] );

				var result = queryExecute(
					"SELECT a.id, a.name, b.dept FROM qry1 a INNER JOIN qry2 b ON a.id = b.id",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 1 );
				expect( result.name[ 1 ] ).toBe( "Alice" );
			});

			it( title="without table aliases", body=function( currentSpec ) {
				var qry1 = queryNew( "id,name", "integer,varchar", [ [ 1, "Alice" ], [ 2, "Bob" ] ] );
				var qry2 = queryNew( "id,dept", "integer,varchar", [ [ 1, "Sales" ] ] );

				var result = queryExecute(
					"SELECT qry1.id, qry1.name, qry2.dept FROM qry1 INNER JOIN qry2 ON qry1.id = qry2.id",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 1 );
			});

		});

		describe( title="LDEV-5845 - QoQ RIGHT JOIN", body=function() {

			it( title="with table aliases", body=function( currentSpec ) {
				var qry1 = queryNew( "id,name", "integer,varchar", [ [ 1, "Alice" ] ] );
				var qry2 = queryNew( "id,dept", "integer,varchar", [ [ 1, "Sales" ], [ 2, "Marketing" ] ] );

				var result = queryExecute(
					"SELECT a.name, b.id, b.dept FROM qry1 a RIGHT JOIN qry2 b ON a.id = b.id",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 2 );
				expect( result.dept[ 1 ] ).toBe( "Sales" );
				expect( result.dept[ 2 ] ).toBe( "Marketing" );
			});

			it( title="without table aliases", body=function( currentSpec ) {
				var qry1 = queryNew( "id,name", "integer,varchar", [ [ 1, "Alice" ] ] );
				var qry2 = queryNew( "id,dept", "integer,varchar", [ [ 1, "Sales" ], [ 2, "Marketing" ] ] );

				var result = queryExecute(
					"SELECT qry1.name, qry2.id, qry2.dept FROM qry1 RIGHT JOIN qry2 ON qry1.id = qry2.id",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 2 );
			});

		});

		describe( title="LDEV-5845 - QoQ RIGHT OUTER JOIN", body=function() {

			it( title="with table aliases", body=function( currentSpec ) {
				var qry1 = queryNew( "id,name", "integer,varchar", [ [ 1, "Alice" ] ] );
				var qry2 = queryNew( "id,dept", "integer,varchar", [ [ 1, "Sales" ], [ 2, "Marketing" ] ] );

				var result = queryExecute(
					"SELECT a.name, b.id, b.dept FROM qry1 a RIGHT OUTER JOIN qry2 b ON a.id = b.id",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 2 );
			});

			it( title="without table aliases", body=function( currentSpec ) {
				var qry1 = queryNew( "id,name", "integer,varchar", [ [ 1, "Alice" ] ] );
				var qry2 = queryNew( "id,dept", "integer,varchar", [ [ 1, "Sales" ], [ 2, "Marketing" ] ] );

				var result = queryExecute(
					"SELECT qry1.name, qry2.id, qry2.dept FROM qry1 RIGHT OUTER JOIN qry2 ON qry1.id = qry2.id",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 2 );
			});

		});

		describe( title="LDEV-5845 - QoQ FULL OUTER JOIN", body=function() {

			it( title="with table aliases", body=function( currentSpec ) {
				var qry1 = queryNew( "id,name", "integer,varchar", [ [ 1, "Alice" ], [ 3, "Charlie" ] ] );
				var qry2 = queryNew( "id,dept", "integer,varchar", [ [ 1, "Sales" ], [ 2, "Marketing" ] ] );

				var result = queryExecute(
					"SELECT a.id as aid, a.name, b.id as bid, b.dept FROM qry1 a FULL OUTER JOIN qry2 b ON a.id = b.id",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 3 ); // Alice+Sales, Charlie (no match), Marketing (no match)
			});

			it( title="without table aliases", body=function( currentSpec ) {
				var qry1 = queryNew( "id,name", "integer,varchar", [ [ 1, "Alice" ], [ 3, "Charlie" ] ] );
				var qry2 = queryNew( "id,dept", "integer,varchar", [ [ 1, "Sales" ], [ 2, "Marketing" ] ] );

				var result = queryExecute(
					"SELECT qry1.id, qry1.name, qry2.dept FROM qry1 FULL OUTER JOIN qry2 ON qry1.id = qry2.id",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 3 );
			});

		});

		describe( title="LDEV-5845 - QoQ CROSS JOIN", body=function() {

			it( title="with table aliases", body=function( currentSpec ) {
				var qry1 = queryNew( "id,name", "integer,varchar", [ [ 1, "Alice" ], [ 2, "Bob" ] ] );
				var qry2 = queryNew( "dept", "varchar", [ [ "Sales" ], [ "Marketing" ] ] );

				var result = queryExecute(
					"SELECT a.id, a.name, b.dept FROM qry1 a CROSS JOIN qry2 b",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 4 ); // Cartesian product: 2 x 2
			});

			it( title="without table aliases", body=function( currentSpec ) {
				var qry1 = queryNew( "id,name", "integer,varchar", [ [ 1, "Alice" ], [ 2, "Bob" ] ] );
				var qry2 = queryNew( "dept", "varchar", [ [ "Sales" ], [ "Marketing" ] ] );

				var result = queryExecute(
					"SELECT qry1.id, qry1.name, qry2.dept FROM qry1 CROSS JOIN qry2",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 4 );
			});

		});

		describe( title="LDEV-5845 - QoQ generic JOIN", body=function() {

			it( title="with table aliases", body=function( currentSpec ) {
				var qry1 = queryNew( "id,name", "integer,varchar", [ [ 1, "Alice" ], [ 2, "Bob" ] ] );
				var qry2 = queryNew( "id,dept", "integer,varchar", [ [ 1, "Sales" ] ] );

				var result = queryExecute(
					"SELECT a.id, a.name, b.dept FROM qry1 a JOIN qry2 b ON a.id = b.id",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 1 );
			});

			it( title="without table aliases", body=function( currentSpec ) {
				var qry1 = queryNew( "id,name", "integer,varchar", [ [ 1, "Alice" ], [ 2, "Bob" ] ] );
				var qry2 = queryNew( "id,dept", "integer,varchar", [ [ 1, "Sales" ] ] );

				var result = queryExecute(
					"SELECT qry1.id, qry1.name, qry2.dept FROM qry1 JOIN qry2 ON qry1.id = qry2.id",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 1 );
			});

		});

		describe( title="LDEV-5845 - QoQ multiple chained JOINs", body=function() {

			it( title="with table aliases", body=function( currentSpec ) {
				var employees = queryNew( "id,name", "integer,varchar", [ [ 1, "Alice" ], [ 2, "Bob" ] ] );
				var departments = queryNew( "id,dept", "integer,varchar", [ [ 1, "Sales" ] ] );
				var locations = queryNew( "dept,city", "varchar,varchar", [ [ "Sales", "NYC" ] ] );

				var result = queryExecute(
					"SELECT e.name, d.dept, l.city FROM employees e INNER JOIN departments d ON e.id = d.id LEFT JOIN locations l ON d.dept = l.dept",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 1 );
				expect( result.name[ 1 ] ).toBe( "Alice" );
				expect( result.dept[ 1 ] ).toBe( "Sales" );
				expect( result.city[ 1 ] ).toBe( "NYC" );
			});

			it( title="without table aliases", body=function( currentSpec ) {
				var employees = queryNew( "id,name", "integer,varchar", [ [ 1, "Alice" ], [ 2, "Bob" ] ] );
				var departments = queryNew( "id,dept", "integer,varchar", [ [ 1, "Sales" ] ] );
				var locations = queryNew( "dept,city", "varchar,varchar", [ [ "Sales", "NYC" ] ] );

				var result = queryExecute(
					"SELECT employees.name, departments.dept, locations.city FROM employees INNER JOIN departments ON employees.id = departments.id LEFT JOIN locations ON departments.dept = locations.dept",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 1 );
			});

		});

		describe( title="LDEV-5845 - QoQ complex ON conditions", body=function() {

			it( title="with table aliases", body=function( currentSpec ) {
				var qry1 = queryNew( "id,status", "integer,varchar", [ [ 1, "active" ], [ 2, "inactive" ] ] );
				var qry2 = queryNew( "id,dept", "integer,varchar", [ [ 1, "Sales" ], [ 2, "Marketing" ] ] );

				var result = queryExecute(
					"SELECT a.id, a.status, b.dept FROM qry1 a LEFT JOIN qry2 b ON (a.id = b.id AND a.status = 'active')",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 2 );
				expect( result.dept[ 1 ] ).toBe( "Sales" ); // Alice matched
				expect( result.dept[ 2 ] ).toBe( "" ); // Bob didn't match (status != 'active')
			});

			it( title="without table aliases", body=function( currentSpec ) {
				var qry1 = queryNew( "id,status", "integer,varchar", [ [ 1, "active" ], [ 2, "inactive" ] ] );
				var qry2 = queryNew( "id,dept", "integer,varchar", [ [ 1, "Sales" ], [ 2, "Marketing" ] ] );

				var result = queryExecute(
					"SELECT qry1.id, qry1.status, qry2.dept FROM qry1 LEFT JOIN qry2 ON (qry1.id = qry2.id AND qry1.status = 'active')",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 2 );
			});

		});

		describe( title="LDEV-5845 - QoQ mixed aliases and no aliases", body=function() {

			it( title="first table with alias, second without", body=function( currentSpec ) {
				var qry1 = queryNew( "id,name", "integer,varchar", [ [ 1, "Alice" ], [ 2, "Bob" ] ] );
				var qry2 = queryNew( "id,dept", "integer,varchar", [ [ 1, "Sales" ] ] );

				var result = queryExecute(
					"SELECT a.id, a.name, qry2.dept FROM qry1 a INNER JOIN qry2 ON a.id = qry2.id",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 1 );
			});

			it( title="first table without alias, second with", body=function( currentSpec ) {
				var qry1 = queryNew( "id,name", "integer,varchar", [ [ 1, "Alice" ], [ 2, "Bob" ] ] );
				var qry2 = queryNew( "id,dept", "integer,varchar", [ [ 1, "Sales" ] ] );

				var result = queryExecute(
					"SELECT qry1.id, qry1.name, b.dept FROM qry1 LEFT JOIN qry2 b ON qry1.id = b.id",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 2 );
			});

			it( title="mixed aliases in three table join", body=function( currentSpec ) {
				var qry1 = queryNew( "id,name", "integer,varchar", [ [ 1, "Alice" ] ] );
				var qry2 = queryNew( "id,dept", "integer,varchar", [ [ 1, "Sales" ] ] );
				var qry3 = queryNew( "dept,city", "varchar,varchar", [ [ "Sales", "NYC" ] ] );

				var result = queryExecute(
					"SELECT a.name, qry2.dept, c.city FROM qry1 a INNER JOIN qry2 ON a.id = qry2.id LEFT JOIN qry3 c ON qry2.dept = c.dept",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 1 );
			});

		});

		describe( title="LDEV-5845 - QoQ joins with 3 and 4 tables", body=function() {

			it( title="three tables with INNER JOINs", body=function( currentSpec ) {
				var employees = queryNew( "id,name,deptid", "integer,varchar,integer", [ [ 1, "Alice", 10 ], [ 2, "Bob", 20 ] ] );
				var departments = queryNew( "id,deptname,locid", "integer,varchar,integer", [ [ 10, "Sales", 100 ], [ 20, "Marketing", 200 ] ] );
				var locations = queryNew( "id,city", "integer,varchar", [ [ 100, "NYC" ], [ 200, "LA" ] ] );

				var result = queryExecute(
					"SELECT e.name, d.deptname, l.city
					 FROM employees e
					 INNER JOIN departments d ON e.deptid = d.id
					 INNER JOIN locations l ON d.locid = l.id",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 2 );
				expect( result.name[ 1 ] ).toBe( "Alice" );
				expect( result.city[ 1 ] ).toBe( "NYC" );
			});

			it( title="three tables with mixed JOIN types", body=function( currentSpec ) {
				var employees = queryNew( "id,name,deptid", "integer,varchar,integer", [ [ 1, "Alice", 10 ], [ 2, "Bob", 99 ] ] );
				var departments = queryNew( "id,deptname,locid", "integer,varchar,integer", [ [ 10, "Sales", 100 ] ] );
				var locations = queryNew( "id,city", "integer,varchar", [ [ 100, "NYC" ], [ 200, "LA" ] ] );

				var result = queryExecute(
					"SELECT e.name, d.deptname, l.city
					 FROM employees e
					 LEFT JOIN departments d ON e.deptid = d.id
					 LEFT JOIN locations l ON d.locid = l.id",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 2 ); // Alice with dept, Bob without
			});

			it( title="four tables with various JOIN types", body=function( currentSpec ) {
				var employees = queryNew( "id,name,deptid,mgrid", "integer,varchar,integer,integer", [ [ 1, "Alice", 10, 100 ], [ 2, "Bob", 20, 100 ] ] );
				var departments = queryNew( "id,deptname", "integer,varchar", [ [ 10, "Sales" ], [ 20, "Marketing" ] ] );
				var managers = queryNew( "id,mgrname", "integer,varchar", [ [ 100, "Charlie" ] ] );
				var locations = queryNew( "deptname,city", "varchar,varchar", [ [ "Sales", "NYC" ] ] );

				var result = queryExecute(
					"SELECT e.name, d.deptname, m.mgrname, l.city
					 FROM employees e
					 INNER JOIN departments d ON e.deptid = d.id
					 INNER JOIN managers m ON e.mgrid = m.id
					 LEFT JOIN locations l ON d.deptname = l.deptname",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 2 );
				expect( result.name[ 1 ] ).toBe( "Alice" );
				expect( result.mgrname[ 1 ] ).toBe( "Charlie" );
				expect( result.city[ 1 ] ).toBe( "NYC" );
				expect( result.city[ 2 ] ).toBe( "" ); // Marketing has no location
			});

			it( title="four tables without aliases", body=function( currentSpec ) {
				var employees = queryNew( "id,name,deptid", "integer,varchar,integer", [ [ 1, "Alice", 10 ] ] );
				var departments = queryNew( "id,deptname,locid", "integer,varchar,integer", [ [ 10, "Sales", 100 ] ] );
				var locations = queryNew( "id,city,regionid", "integer,varchar,integer", [ [ 100, "NYC", 1 ] ] );
				var regions = queryNew( "id,region", "integer,varchar", [ [ 1, "East" ] ] );

				var result = queryExecute(
					"SELECT employees.name, departments.deptname, locations.city, regions.region
					 FROM employees
					 INNER JOIN departments ON employees.deptid = departments.id
					 INNER JOIN locations ON departments.locid = locations.id
					 INNER JOIN regions ON locations.regionid = regions.id",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 1 );
				expect( result.name[ 1 ] ).toBe( "Alice" );
				expect( result.region[ 1 ] ).toBe( "East" );
			});

		});

		describe( title="LDEV-5845 - QoQ with subqueries", body=function() {

			it( title="WHERE IN with subquery", body=function( currentSpec ) {
				var employees = queryNew( "id,name,deptid", "integer,varchar,integer", [ [ 1, "Alice", 10 ], [ 2, "Bob", 20 ], [ 3, "Charlie", 30 ] ] );
				var activedepts = queryNew( "deptid", "integer", [ [ 10 ], [ 20 ] ] );

				var result = queryExecute(
					"SELECT id, name FROM employees WHERE deptid IN (SELECT deptid FROM activedepts)",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 2 );
				expect( result.name[ 1 ] ).toBe( "Alice" );
				expect( result.name[ 2 ] ).toBe( "Bob" );
			});

		});

		describe( title="LDEV-5845 - QoQ edge cases with string literals and special characters", body=function() {

			it( title="ON clause with string literal containing parentheses", body=function( currentSpec ) {
				var qry1 = queryNew( "id,name", "integer,varchar", [ [ 1, "Alice" ], [ 2, "Bob" ] ] );
				var qry2 = queryNew( "id,dept,note", "integer,varchar,varchar", [ [ 1, "Sales", "(main)" ], [ 2, "Marketing", "(test)" ] ] );

				var result = queryExecute(
					"SELECT a.id, a.name, b.dept FROM qry1 a LEFT JOIN qry2 b ON (a.id = b.id AND b.note = '(main)')",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 2 );
				expect( result.dept[ 1 ] ).toBe( "Sales" ); // Matched with (main)
				expect( result.dept[ 2 ] ).toBe( "" ); // No match (note != '(main)')
			});

			it( title="ON clause with string literal containing closing paren", body=function( currentSpec ) {
				var qry1 = queryNew( "id,name", "integer,varchar", [ [ 1, "Alice" ], [ 2, "Bob" ] ] );
				var qry2 = queryNew( "id,marker", "integer,varchar", [ [ 1, ")test" ], [ 2, "normal" ] ] );

				var result = queryExecute(
					"SELECT a.id, a.name, b.marker FROM qry1 a LEFT JOIN qry2 b ON a.id = b.id AND b.marker = ')test'",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 2 );
				expect( result.marker[ 1 ] ).toBe( ")test" );
			});

			it( title="ON clause with escaped quotes in string literal", body=function( currentSpec ) {
				var qry1 = queryNew( "id,name", "integer,varchar", [ [ 1, "Alice" ], [ 2, "Bob" ] ] );
				var qry2 = queryNew( "id,status", "integer,varchar", [ [ 1, "it's ok" ], [ 2, "normal" ] ] );

				var result = queryExecute(
					"SELECT a.id, a.name, b.status FROM qry1 a LEFT JOIN qry2 b ON (a.id = b.id AND b.status = 'it''s ok')",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 2 );
				expect( result.status[ 1 ] ).toBe( "it's ok" );
			});

			it( title="ON clause with nested parentheses in conditions", body=function( currentSpec ) {
				var qry1 = queryNew( "id,val1,val2", "integer,integer,integer", [ [ 1, 10, 20 ], [ 2, 30, 40 ] ] );
				var qry2 = queryNew( "id,total", "integer,integer", [ [ 1, 30 ], [ 2, 70 ] ] );

				var result = queryExecute(
					"SELECT a.id, a.val1, a.val2, b.total FROM qry1 a LEFT JOIN qry2 b ON (a.id = b.id AND ((a.val1 + a.val2) = b.total))",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 2 );
				expect( result.total[ 1 ] ).toBe( 30 ); // 10 + 20 = 30, matches
				expect( result.total[ 2 ] ).toBe( 70 ); // 30 + 40 = 70, matches
			});

			it( title="Multiple JOINs with complex ON conditions containing parens", body=function( currentSpec ) {
				var employees = queryNew( "id,name,salary", "integer,varchar,integer", [ [ 1, "Alice", 50000 ], [ 2, "Bob", 80000 ] ] );
				var departments = queryNew( "id,dept,minsal", "integer,varchar,integer", [ [ 1, "Sales", 40000 ], [ 2, "Engineering", 70000 ] ] );
				var bonuses = queryNew( "empid,bonus", "integer,integer", [ [ 1, 5000 ], [ 2, 10000 ] ] );

				var result = queryExecute(
					"SELECT e.name, d.dept, b.bonus
					 FROM employees e
					 INNER JOIN departments d ON (e.id = d.id AND (e.salary >= d.minsal))
					 LEFT JOIN bonuses b ON (e.id = b.empid)",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 2 );
				expect( result.name[ 1 ] ).toBe( "Alice" );
				expect( result.bonus[ 1 ] ).toBe( 5000 );
			});

			it( title="JOIN with WHERE clause after ON clause", body=function( currentSpec ) {
				var qry1 = queryNew( "id,name,active", "integer,varchar,varchar", [ [ 1, "Alice", "Y" ], [ 2, "Bob", "N" ], [ 3, "Charlie", "Y" ] ] );
				var qry2 = queryNew( "id,dept", "integer,varchar", [ [ 1, "Sales" ], [ 2, "Marketing" ], [ 3, "Engineering" ] ] );

				var result = queryExecute(
					"SELECT a.id, a.name, b.dept FROM qry1 a LEFT JOIN qry2 b ON a.id = b.id WHERE a.active = 'Y'",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 2 ); // Only active employees
				expect( result.name[ 1 ] ).toBe( "Alice" );
				expect( result.name[ 2 ] ).toBe( "Charlie" );
			});

			it( title="JOIN with GROUP BY after ON clause", body=function( currentSpec ) {
				var sales = queryNew( "id,empid,amount", "integer,integer,integer", [ [ 1, 1, 100 ], [ 2, 1, 200 ], [ 3, 2, 150 ] ] );
				var employees = queryNew( "id,name", "integer,varchar", [ [ 1, "Alice" ], [ 2, "Bob" ] ] );

				var result = queryExecute(
					"SELECT e.name, SUM(s.amount) as total FROM sales s INNER JOIN employees e ON s.empid = e.id GROUP BY e.name",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 2 );
			});

		});

		describe( title="LDEV-5845 - QoQ with comments in JOINs", body=function() {

			// NOTE: These tests pass because the CFML engine strips out SQL comments before they reach the QoQ parser.
			// The tests are kept to document this behavior.

			it( title="should handle single-line comments in ON clause", body=function( currentSpec ) {
				var qry1 = queryNew( "id,name", "integer,varchar", [ [ 1, "Alice" ] ] );
				var qry2 = queryNew( "id,dept", "integer,varchar", [ [ 1, "Sales" ] ] );

				var result = queryExecute(
					"SELECT a.id, b.dept FROM qry1 a LEFT JOIN qry2 b ON a.id = b.id -- comment with a ' quote and ( paren",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 1 );
				expect( result.dept[ 1 ] ).toBe( "Sales" );
			});

			it( title="should handle multi-line comments in ON clause", body=function( currentSpec ) {
				var qry1 = queryNew( "id,name", "integer,varchar", [ [ 1, "Alice" ] ] );
				var qry2 = queryNew( "id,dept", "integer,varchar", [ [ 1, "Sales" ] ] );

				var result = queryExecute(
					"SELECT a.id, b.dept FROM qry1 a LEFT JOIN qry2 b ON a.id = b.id /* block comment with a ' quote and ( paren */",
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 1 );
				expect( result.dept[ 1 ] ).toBe( "Sales" );
			});

			it( title="should handle multi-line comments spanning multiple lines in ON clause", body=function( currentSpec ) {
				var qry1 = queryNew( "id,name", "integer,varchar", [ [ 1, "Alice" ] ] );
				var qry2 = queryNew( "id,dept", "integer,varchar", [ [ 1, "Sales" ] ] );

				var sql = "SELECT a.id, b.dept
									   FROM qry1 a
									   LEFT JOIN qry2 b ON a.id = b.id
									   /*
											   block comment with a ' quote and ( paren
											   and another line
									   */
									   WHERE 1=1";

				var result = queryExecute(
					sql,
					{},
					{ dbtype="query" }
				);

				expect( result ).toBeQuery();
				expect( result.recordCount ).toBe( 1 );
				expect( result.dept[ 1 ] ).toBe( "Sales" );
			});

		});

	}
}
