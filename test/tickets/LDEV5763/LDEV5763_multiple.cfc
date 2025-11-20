// Multiple maven coordinates in string format
// Expected: WORKS
component output=false
	javasettings = '{
		maven = [
			"org.apache.commons:commons-lang3:3.12.0",
			"commons-io:commons-io:2.11.0"
		]
	}'
{
	public function test() {
		return "multiple_maven";
	}
}
