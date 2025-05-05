component extends="org.lucee.cfml.test.LuceeTestCase" {
    function run( testResults, testBox ) {
        describe("Testcase for LDEV-5349", function() {
            it(title='Check deserializeJSON with strictMapping as query type', skip=true, body=function(currentSpec) {
                // JSON data
                testJSON = '
                [
                    {
                        "id": "1",
                        "name": "pothys"
                    },
                    {
                        "id": "2",
                        "name": "lucee"
                    }
                ]'; 
                deserializedQuery = deserializeJSON(testJSON, "query");
                // Create the expected query object manually
                expectedQuery = queryNew("id, name");
                queryAddRow(expectedQuery, [1, "pothys"]);
                queryAddRow(expectedQuery, [2, "lucee"]);
                expect( deserializedQuery ).toBe( expectedQuery );
            });
        });
    }
}