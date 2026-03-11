component extends="org.lucee.cfml.test.LuceeTestCase" {

	function run( testResults , testBox ) {
		describe( title="Test suite for LDEV-6135", body=function() {

            it(title="cfthread join should wait for all threads", skip=true, body=function(currentSpec){
               var threadSize = 200; 
                var threadNames = [];
                for(i=1; i <= threadSize; i++){
                    var threadName = "thread_test_#i#_" & replace(createUUID(), "-", "", "all");
                    threadNames.append(threadName);
                    thread action="run" name="#threadName#" {
                        try {
                            sleep(randRange(500, 1500));
                            throw("here");
                        } catch (Any e){
                            thread.errored = true;
                        }
                        thread.finished = true;
                    }
                }
                thread action="join" name="#threadNames.toList()#" timeout="60000";

                // Check all threads finished
                var finishedCount = 0;
                structEach(cfthread, function(tname, tresult){
                    var finished = tresult.FINISHED ?: false;
                    if(finished) finishedCount++;
                });
                
                expect(finishedCount).toBe(threadSize);
            });

		});
	}   

}
