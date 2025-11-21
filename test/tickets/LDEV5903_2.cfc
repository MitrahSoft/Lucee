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
					var content=repeatString("#prefix#cfscript>variables.ldev5903_1='#createUUID()#';#prefix#/cfscript>",20);
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

			it( "test to change the same cfc template over and over again", function() {
				var beforeUsage=usage();
				var componentName="_LDEV5903Test";
				var templateName=componentName&".cfc";
				var templatePath=variables.curr&templateName;
				var prefix="<";
				loop times=10000 {
					// write cfml template
					var content=repeatString("variables.ldev5903_2='#createUUID()#';",20);
					var content="component {#content# }";
					fileWrite(templatePath,content);
					inspecttemplates(); // force Lucee to check for changes
					createObject("component",componentName);
				}
				// force the garbage collector to clean up
				java.lang.System::gc();
				// take a nap, normally GC should stop all the threads, but some future implmentation may do not
				sleep(1000);
				expect(usage()).toBeLT(0.5);


			});

			it( "test to create a lot of cfml templates filling the classloader", function() {
				var beforeUsage=usage();
				var prefix="<";
				var content=repeatString("#prefix#cfscript>variables.ldev5903_3='#createUUID()#';#prefix#/cfscript>",20);
				loop from=1 to=10000 index="local.i" {
					var templateName="ldev5903_test#i#.cfm";
					var templatePath=variables.curr&templateName;
					// write cfml template
					fileWrite(templatePath,content);
					include templateName;
				}
				// force the garbage collector to clean up
				java.lang.System::gc();
				// take a nap, normally GC should stop all the threads, but some future implmentation may do not
				sleep(1000);
				expect(usage()).toBeLT(0.5);


			});
			it( "test to create a lot of cfc templates filling the classloader", function() {
				var beforeUsage=usage();
				var prefix="<";
				var content=repeatString("variables.ldev5903_4='#createUUID()#';",20);
				var content="component {#content# }";
				loop from=1 to=10000 index="local.i" {
					var componentName="_LDEV5903Test#i#";
					var templateName=componentName&".cfc";
					var templatePath=variables.curr&templateName;
					// write cfml template
					fileWrite(templatePath,content);
					createObject("component",componentName);
				}
				// force the garbage collector to clean up
				java.lang.System::gc();
				// take a nap, normally GC should stop all the threads, but some future implmentation may do not
				sleep(1000);
				expect(usage()).toBeLT(0.5);
				systemOutput("----------------------",1,1);
				systemOutput(beforeUsage,1,1);
				systemOutput(usage,1,1);

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
