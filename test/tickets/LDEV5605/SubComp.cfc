component {
	function dummy() {
		return "main";
	}
}

component name="SubComp" {
	function test(){
		throw "ERROR IN SUB COMPONENT"; // line 9
	}
}
