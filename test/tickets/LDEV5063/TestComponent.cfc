component {

	variables.wasCalled = false;

	public void function run() {
		variables.wasCalled = true;
	}

	public boolean function getWasCalled() {
		return variables.wasCalled;
	}
}
