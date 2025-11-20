// Tag-in-script syntax with string format
// Expected: WORKS
component output=false
	javasettings = '{
		maven = ["org.apache.commons:commons-lang3:3.12.0"]
	}'
{
	public function test() {
		return "json_string";
	}

	function helper() {
		return "helper";
	}

}
