// Multiple maven coordinates with struct literal
// Expected: BUG - currently compiles but results in 0 functions
component output=false
	javasettings = {
		maven = [
			"org.apache.commons:commons-lang3:3.12.0",
			"commons-io:commons-io:2.11.0"
		]
	}
{
	public function test() {
		return "multiple_struct";
	}

}
