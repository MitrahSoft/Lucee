// Tag-in-script syntax with struct literal
// Expected: BUG - currently compiles but results in 0 functions
component output=false
	javasettings = {
		maven = ["org.apache.commons:commons-lang3:3.12.0"]
	}
{
	public function test() {
		return "cfml_struct";
	}

	function helper() {
		return "helper";
	}

}
