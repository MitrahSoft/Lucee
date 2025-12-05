// Function-style syntax with struct literal
// Expected: Parse/compile error
component(
	output=false,
	javasettings = {
		maven = ["org.apache.commons:commons-lang3:3.12.0"]
	}
) {
	public function test() {
		return "function_struct";
	}
}
