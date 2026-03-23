component extends="org.lucee.cfml.test.LuceeTestCase" {

    function run(testResults, testBox) {
        function beforeAll(){
            application action='update' nullSupport=false;
        }
        describe(title="LDEV-6185 NULL-JSON Serialization", body=function() {

            it(title="nullValue() keys should be omitted when nullSupport=false", skip=true, body=function() {

                var data = {name = "Pothys",middleName = nullValue()};
                var keyExists = structKeyExists(data, "middleName");
                var jsonOutput = serializeJSON(data);

                expect(keyExists).toBeFalse();
                expect(jsonOutput).toBe('{"NAME":"Pothys"}');

            });

        });

    }

}