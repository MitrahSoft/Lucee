component extends="org.lucee.cfml.test.LuceeTestCase" {

	function run() {
		describe("Tests for cfprocessingdirective", function() {

			it("should correctly set page encoding", function() {
				var result = _InternalRequest(template="#createURI('processingDirective')#/cfprocessingdirective_encoding.cfm");
				debug(result);
				expect(result.fileContent.trim()).toBe("Encoding set to UTF-8");
			});

			// ACF 2023 doesn't seem to strip whitespace at all?
			it("should suppress whitespace when specified", function() {
				var result = _InternalRequest(template="#createURI('processingDirective')#/cfprocessingdirective_whitespace.cfm");
				var text = replace(
						replace(
							replace(result.fileContent,chr(10),"LF","all"),
						chr(13),"CR","all"),
					" ",".","all"
				);
				// build expected result based on actual file line endings
				var expected = buildExpectedWhitespaceResult();
				expect( text ).toBe( expected );
			});
		});

		// disabled hangs https://luceeserver.atlassian.net/browse/LDEV-5378

		xdescribe("Tests for cfprocessingdirective - preservecase", function() {

			it("should preserve case when enabled", function() {
				var result = _InternalRequest(
					template="#createURI('processingDirective')#/cfprocessingdirective_preservecase.cfm",
					url: {
						preserve: true
					}
				);
				expect(result.fileContent).toBeWithCase('{"camelCase":true}');
			});

			it("shouldn't preserve case when disabled", function() {
				var result = _InternalRequest(
					template="#createURI('processingDirective')#/cfprocessingdirective_preservecase.cfm",
					url: {
						preserve: false
					}
				);
				expect(result.fileContent).toBeWithCase('{"CAMELCASE":false}');
			});

			it("shouldn't preserve case by default, no processingdirective", function() {
				var result = _InternalRequest(
					template="#createURI('processingDirective')#/cfprocessingdirective_preservecase.cfm",
					url: {
						default: true
					}
				);
				expect(result.fileContent).toBeWithCase('{"CAMELCASE":"default"}');
			});
		});
	}

	private string function createURI(string calledName){
		var baseURI="/test/#listLast(getDirectoryFromPath(getCurrentTemplatePath()),"\/")#/";
		return baseURI&""&calledName;
	}

	private function isWindows(){
		return (server.os.name contains "windows");
	}

	private string function buildExpectedWhitespaceResult(){
		// StringUtil.suppressWhiteSpace() normalizes all \r and \n to \n (LF only)
		// see lucee.commons.lang.StringUtil:713
		// however, the line ending after the opening tag itself is output before
		// the tag processes the body content, so it preserves the file's line ending style
		var filePath = expandPath( createURI('processingDirective') & "/cfprocessingdirective_whitespace.cfm" );
		var fileContent = fileRead( filePath );

		// determine the file's line ending style (depends on OS and git autocrlf settings)
		var firstLineEnding = "LF";
		if ( fileContent contains chr(13) & chr(10) ) {
			firstLineEnding = "CRLF";
		}

		// after whitespace suppression we expect:
		// - first line ending (after opening tag) preserves file's line ending style
		// - all subsequent line endings are normalized to LF by StringUtil.suppressWhiteSpace()
		return firstLineEnding & "LF" & "Line1" & "LF" & "Line2" & "LF" & "Line3" & "LF";
	}

}
