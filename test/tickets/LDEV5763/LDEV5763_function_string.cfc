// Function-style syntax with string
// Expected: WORKS
component( output=false, javasettings = '{ maven = ["org.apache.commons:commons-lang3:3.12.0"] }' ) {
	public function test() {
		return "function_string";
	}
}
