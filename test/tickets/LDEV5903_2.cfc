component extends="org.lucee.cfml.test.LuceeTestCase" labels="classloader,memory" {
 
	function beforeAll() {
		variables.curr=getDirectoryFromPath(getCurrentTemplatePath());
	}

	function run( testResults, testBox ) {
		describe( "LDEV-5903 - Per-class classloader regression (memory leak)", function() {

			it( "test to change the same cfml template over and over again", function() {
				var beforeUsage=usage();
				var templateName="ldev5903_test.cfm";
				var templatePath=variables.curr&templateName;
				var prefix="<";
				loop times=10000 {
					// write cfml template
					var content=repeatString("#prefix#cfscript>ldev5903='#createUUID()#';#prefix#/cfscript>",100);
					fileWrite(templatePath,content);
					inspecttemplates(); // force Lucee to check for changes
					include templateName;
				}
				// force the garbage collector to clean up
				java.lang.System::gc();
				// take a nap, normally GC should stop all the threads, but some future implmentation may do not
				sleep(1000);
				expect(usage()).toBeLT(0.5);


			});
		});
	}

	private function usage() {
		var usage=getmemoryusage("non_heap");
		var used=0;
		var max=0;
		loop query=usage {
			if(usage.used==-1 || usage.max==-1) continue;
			local.used+=usage.used;
			local.max+=usage.max;
		}
		return 1/max*used;
	}
}
