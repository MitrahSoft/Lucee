component extends="org.lucee.cfml.test.LuceeTestCase" labels="esapi"{
	function run( testResults , testBox ) {
		describe( "test case for EncodeForXML", function() {
			it(title = "Checking with EncodeForXML", body = function( currentSpec ) {
				var enc=EncodeForXML('<script>');
				// cover result from esapi and owasp encoder library that are both valid
				var isEncoded=findNoCase(enc,'&##x3c;script&##x3e;')>0 || findNoCase(enc,'&lt;script&gt;')>0;
				assertEquals(isEncoded,true);
			});

			it(title = "Checking with EncodeForXMLMember", body = function( currentSpec ) {
				var enc='<script>'.EncodeForXML();
				// cover result from esapi and owasp encoder library that are both valid
				var isEncoded=findNoCase(enc,'&##x3c;script&##x3e;')>0 || findNoCase(enc,'&lt;script&gt;')>0;
				assertEquals(isEncoded,true);
			});
		});	
	}
}