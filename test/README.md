# Lucee Test Suite

This directory contains the CFML test suites for Lucee Server.

Tests are written using [TestBox](https://testbox.ortusbooks.com/)

Lucee tests must extend `"org.lucee.cfml.test.LuceeTestCase"` unless overriding with `testSuiteExtends`

## Running Tests

Lucee tests are run using Ant and Maven. You can execute all tests or filter to specific suites.

### Basic Usage

From the `/loader` directory:

```bash
ant test
```

Or, to run a specific test suite:

```bash
mvn test -DtestFilter="{testFilename}"
```

Pipe the build output to a file, which you can then grep afterwards, rather than re-running the build just to grep again.

### Test Runner Options

You can use the following options to control which tests are run and how they are executed (see [Lucee Docs](https://docs.lucee.org/guides/working-with-source/build-from-source.html#build-performance-tips)):

- `-DskipTests=true` — Skip running tests during build
- `-Dmaven.javadoc.skip=true` — Skip Javadoc generation
- `-Dmaven.source.skip=true` — Skip source JAR generation
- `-DskipNpm=true` — Skip npm install and build steps
- `-DskipAntTests=true` — Skip Ant-based tests
- `-DskipIntegrationTests=true` — Skip integration tests
- `-DskipDeploy=true` — Skip deployment steps

#### Test Filtering and Execution

- `-DtestFilter="foo,bar"` — Comma-separated list of substrings; only tests with filenames containing these values are run.
- `-DtestLabels="image,orm"` — Comma-separated list of labels; only tests with these labels are run.
- `-DtestAdditional="/path/to/dir"` — Path to an additional directory of tests to include (virtual mapping `/testAdditional`).
- `-DtestExcludeDefault=true` — Exclude the default `/test` directory from test runs. Only additional or explicitly specified test suites will be executed, this is faster when using the test runner, but only testing tests defined using `testAdditional`
- `-DtestSkip=false` — If set to `false`, forces running tests marked `skip=true` or prefixed with `_`.
- `-DtestRandomSort=true` — Randomizes the order of tests. You can also provide a numeric seed (e.g., `-DtestRandomSort=42`) for reproducible order.
- `-DtestSuiteExtends="my.CustomBaseSpec"` — Specify a custom BaseSpec CFC to extend (default: `org.lucee.cfml.test.LuceeTestCase`).

#### Debugging and Output

- `-DtestDebug=true` — Enables debug output for the test run.
- `-DtestDebugAbort=true` — If true, aborts the test run after filtering (for debugging filter logic).
- `-DtestHideJavaStack=true` — Hides Java stack traces in the test run output.
- `-DtestServices="service1,service2"` — Restricts test services to a comma-separated list.

For more details, see the [Lucee build guide](https://docs.lucee.org/guides/working-with-source/build-from-source.html).

#### Avoid defensive code

Tests should fail if needed, too defensive code tends to mask underlying problems.

#### Try Catch

If you do use try catch to return an error, return the `e.stacktrace`, rather than just the `e.message` as it's more useful to action.