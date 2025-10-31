# Lucee Server

Lucee Server is an open source CFML Server which gets deployed via a Java Servlet.
The Lucee code base was forked from the Railo Server Project (Version 4.2) in January 2015.

## Architecture

- Documentation is published at `https://docs.lucee.org/`
- Java baseline version is 11

## Folder Structure

- `/ant`: Ant Build scripts
- `/loader`: The Loader Interface API used by for Lucee Core and its extensions, do not modify any interfaces
- `/core`: The main source code for Lucee Server
- `/test`: Contains the CFML Test suites

## Build & Commands

- Build, in the `/loader` directory, execute `mvn fast`
- Build and test, in the `/loader` directory, execute `mvn test`
- Build and execute a specific CFML test suite in the `/loader` directory, execute `mvn test -DtestFilter="{testFilename}"`

### Development Environment

- Build requires Java, Maven and Ant
- Build usually is run with Java 21
- All artifacts are compiled to bytecode targeting Java 11

### Issue Tracking

Lucee tickets are in the style `LDEV-xxxx`, where `xxx` is a number

Test cases for tickets are created under `/test/tickets/LDEVxxxx.cfc`, with any additional files under a folder /`test/tickets/LDEVxxxx/`

To read an issue from jira, rewrite the url `https://luceeserver.atlassian.net/browse/LDEV-5850` to read the xml version using `https://luceeserver.atlassian.net/si/jira.issueviews:issue-xml/LDEV-5850/LDEV-5850.xml`

## Lucee Ant Script Runner

Lucee Ant Script Runner allows you to run Lucee CFML scripts headless (without a web server) from the command line or CI/CD pipelines.

It is ideal for automation, testing, and running scripts with custom Lucee builds.

**Requirements:**
- You must have a local copy of the [script-runner repository](https://github.com/lucee/script-runner) checked out.
  If you do not have it, please clone it first.
  > **Tip:** The script-runner directory may already exist in the parent directory of this repo.

- To use a custom Lucee JAR, first build it by running:

```sh
ant fast
```

in the `/loader` directory of this repo.
The resulting JAR will be found in `loader/target` and its filename will include the version (e.g., `lucee-7.0.0.1.jar`).

**Example usage:**

```sh
ant -buildfile="..\script-runner\build.xml" -DluceeJar="/full/pathot/loader/target/lucee-{version}.jar" -Dwebroot="D:\work\yourproject" -Dexecute="test.cfm"
```


- `-DluceeJar` being the full path to the built Lucee JAR in `loader/target`, use exact version and full path
- `-Dwebroot` is your project directory.
- `-Dexecute` is the script to run (relative to webroot).

See [script-runner README](https://github.com/lucee/script-runner/blob/main/README.md) for full details and troubleshooting.

## Contribution Workflow

- Before starting work, consider filing a proposal on the mailing list.
- File a ticket for your issue on JIRA, assuming one does not already exist.
- Fork the repository on GitHub.
- Create a feature branch off the appropriate version branch. `7.0` is the active development branch. `6.2` is the active stable branch. `5.4` is for LTS security fixes.
- Create or update unit tests for your changes.
- Make sure your branch is rebased with the latest changes from the upstream repo before submitting.
- Commit messages must include the ticket number, e.g., `LDEV-007 Add support to James Bond's watch for OSGI bundles`.
- Include a link to the JIRA ticket in your pull request description.
- All contributors must accept the LAS Contributor License Agreement (CLA).

### Documentation

If your change affects a documented feature, please also submit a pull request to the Lucee docs repo.

## Code Style

- Follow the Eclipse settings for Java code in `/org.eclipse.jdt.core.prefs`
- Use Tabs for indentation (2 spaces for YAML/JSON/MD)
- Avoid adding comments, unless they add important additional context
- Never remove existing comments

## Testing

[Testing Guidelines](test/README.md)

- CFML Tests are written using TestBox [TestBox](https://testbox.ortusbooks.com/)
- All CFML tests should extend `org.lucee.cfml.test.LuceeTestCase`
- CFML tests should not use Java unless absolutely required, prefer CFML functionality.
- Tests should cleanup after themselves and any temporary files should be created under the directory returned from `getTempDirectory()`
- Test framework code, specifically files in the root of the `/test` directory should be compatible with Lucee 5.4, therefore, do not use newer cfml functionality.


## Security

- Use appropriate data types that limit exposure of sensitive information
- Never commit secrets or API keys to the repository
- Use environment variables for sensitive data
- Validate all user inputs on both client and server
- Follow the principle of least privilege

### Reporting a Vulnerability

Please send an email to security@lucee.org to report a vulnerability.

## Configuration

When adding new configuration options, update all relevant places:

1. Variables are always strings and should be cast to the correct type, with an appropriate default.
2. Variables should be read once into a static variable using `getSystemPropOrEnvVar(String name, String defaultValue)`
3. Document variables in `core/src/main/java/resource/setting/sysprop-envvar.json`

When updating a Java library

1. Update both the `pom.xml` files under `/loader` and `/core`
2. Update the corresponding entry under `Require-Bundle:` in `core/src/main/java\META-INF\MANIFEST.MF`

## Getting Help

- [Lucee Documentation](https://docs.lucee.org/)
- [Lucee Mailing List / Forum](https://dev.lucee.org/)
- [Lucee Bug Tracker](https://luceeserver.atlassian.net/)

## License

Lucee Server is licensed under the Lesser GNU General Public License Version 2.1 (or later).
