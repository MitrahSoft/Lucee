component extends="org.lucee.cfml.test.LuceeTestCase" labels="esapi"{
	function run( testResults , testBox ) {
		describe( "test case for EncodeForXMLAttribute", function() {
			it(title = "Checking with EncodeForXMLAttribute", body = function( currentSpec ) {
				var enc=EncodeForXMLAttribute('<script>');
				assertEquals('&lt;script>',enc);
			});

			it(title = "Checking with EncodeForXMLAttributeMember", body = function( currentSpec ) {
				var enc='<script>'.EncodeForXMLAttribute();
				assertEquals('&lt;script>',enc);
			});
		});	
	}
}