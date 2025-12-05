component extends="org.lucee.cfml.test.LuceeTestCase" labels="listGetDuplicates" {
	function run( testResults , testBox ) {
		describe( "Test suite for listGetDuplicates", function() {
			it(title="checking listGetDuplicates function, having simple list with duplicate values", body = function( currentSpec ) {
				var list = '1,7,,7,10,6,7,8';
				var result = listGetDuplicates(list);
				expect(result).toBe('7', list);
			});

			it(title="checking listGetDuplicates function, having duplicate value at last", body = function( currentSpec ) {
				var list = '1,7,77,10,6,7';
				var result = listGetDuplicates(list);
				expect(result).toBe('7', list);
			});

			it(title="checking listGetDuplicates function, having duplicate value at last", body = function( currentSpec ) {
				var list = '1,7,7,,10,6,7';
				var result = listGetDuplicates(list);
				expect(result).toBe('7', list);
			});

			it(title="checking listGetDuplicates function, having empty value at last", body = function( currentSpec ) {
				var list = '1,7,7,10,6, ';
				var result = listGetDuplicates(list);
				expect(result).toBe('7', list);
			});

			it(title="checking listGetDuplicates function, having two duplicates, alt delim", body = function( currentSpec ) {
				var list = '1+7+1+7';
				var result = listGetDuplicates(list,"+");
				expect(result).toBe('1+7', list);
			});
		});

		describe( "Test suite for listGetDuplicates - multipleDelimiters", function() {
			it(title="checking listGetDuplicates function", body = function( currentSpec ) {
				var list = 'a,!b,!c,!d,!a';
				var result = listGetDuplicates(list=list, delimiter=",!");
				expect(result).toBe('a', list);
			});
		});

		describe( "Test suite for listGetDuplicates - ignore case", function() {
			it(title="checking listGetDuplicates function, ignoreCase=true", body = function( currentSpec ) {
				var list = 'a,b,c,d,A';
				var result = listGetDuplicates(list=list, ignoreCase=true);
				expect(result).toBe('a', list);
			});

			it(title="checking listGetDuplicates function, ignoreCase=false", body = function( currentSpec ) {
				var list = 'a,b,c,d,A';
				var result = listGetDuplicates(list=list, ignoreCase=false);
				expect(result).toBe('', list);
			});
		});

		describe( "Test suite for listGetDuplicates - includeEmptyFields", function() {
			it(title="checking listGetDuplicates function, includeEmptyFields=true, empty duplicate first", body = function( currentSpec ) {
				var list = 'a,b,c,,d,,a,';
				var result = listGetDuplicates(list=list, includeEmptyFields=true);
				expect(result).toBe(',a', list);
			});

			it(title="checking listGetDuplicates function, includeEmptyFields=true", body = function( currentSpec ) {
				var list = 'a,b,a,,d,,a,';
				var result = listGetDuplicates(list=list, includeEmptyFields=true);
				expect(result).toBe('a,', list);
			});

			it(title="checking listGetDuplicates function, includeEmptyFields=false", body = function( currentSpec ) {
				var list = 'a,b,,c,d,,a,';
				var result = listGetDuplicates(list=list, includeEmptyFields=false);
				expect(result).toBe('a', list);
			});
		});

		describe( "Test suite for string.listGetDuplicates() member function", function() {
			it(title="checking string.listGetDuplicates() member function, having simple list with duplicate values", body = function( currentSpec ) {
				var list = '1,7,,7,10,6,7,8';
				var result = list.listGetDuplicates();
				expect(result).toBe('7', list);
			});

			it(title="checking string.listGetDuplicates() member function, having duplicate value at last", body = function( currentSpec ) {
				var list = '1,7,77,10,6,7';
				var result = list.listGetDuplicates();
				expect(result).toBe('7', list);
			});

			it(title="checking string.listGetDuplicates() member function, having duplicate value at last", body = function( currentSpec ) {
				var list = '1,7,7,,10,6,7';
				var result = list.listGetDuplicates();
				expect(result).toBe('7', list);
			});

			it(title="checking string.listGetDuplicates() member function, having empty value at last", body = function( currentSpec ) {
				var list = '1,7,7,10,6, ';
				var result = list.listGetDuplicates();
				expect(result).toBe('7', list);
			});

			it(title="checking string.listGetDuplicates() member function, having two duplicates, alt delim", body = function( currentSpec ) {
				var list = '1+7+1+7';
				var result = list.listGetDuplicates("+");
				expect(result).toBe('1+7', list);
			});
		});

		describe( "Test suite for string.listGetDuplicates() member function - multipleDelimiters", function() {
			it(title="checking string.listGetDuplicates() member function", body = function( currentSpec ) {
				var list = 'a,!b,!c,!d,!a';
				var result = list.listGetDuplicates(delimiter=",!");
				expect(result).toBe('a', list);
			});
		});

		describe( "Test suite for string.listGetDuplicates() member function - ignore case", function() {
			it(title="checking string.listGetDuplicates() member function, ignoreCase=true", body = function( currentSpec ) {
				var list = 'a,b,c,d,A';
				var result = list.listGetDuplicates(ignoreCase=true);
				expect(result).toBe('a', list);
			});

			it(title="checking string.listGetDuplicates() member function, ignoreCase=false", body = function( currentSpec ) {
				var list = 'a,b,c,d,A';
				var result = list.listGetDuplicates(ignoreCase=false);
				expect(result).toBe('', list);
			});
		});

		describe( "Test suite for string.listGetDuplicates() member function - includeEmptyFields", function() {
			it(title="checking string.listGetDuplicates() member function, includeEmptyFields=true, empty duplicate first", body = function( currentSpec ) {
				var list = 'a,b,c,,d,,a,';
				var result = list.listGetDuplicates(includeEmptyFields=true);
				expect(result).toBe(',a', list);
			});

			it(title="checking string.listGetDuplicates() member function, includeEmptyFields=true", body = function( currentSpec ) {
				var list = 'a,b,a,,d,,a,';
				var result = list.listGetDuplicates(includeEmptyFields=true);
				expect(result).toBe('a,', list);
			});

			it(title="checking string.listGetDuplicates() member function, includeEmptyFields=false", body = function( currentSpec ) {
				var list = 'a,b,,c,d,,a,';
				var result = list.listGetDuplicates(includeEmptyFields=false);
				expect(result).toBe('a', list);
			});
		});
	}
}
