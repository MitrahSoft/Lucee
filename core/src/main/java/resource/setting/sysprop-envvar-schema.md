# System Property / Environment Variable Schema

This document describes the JSON schema used in `sysprop-envvar.json` for documenting Lucee system properties and environment variables.

## Purpose

The `sysprop-envvar.json` file is the source of truth for all Lucee system properties and environment variables. This data is used to:

- Generate documentation at https://docs.lucee.org/
- Provide metadata for IDE autocomplete and validation
- Power the `getSystemPropOrEnvVar()` function's documentation
- Help developers understand available configuration options

## JSON Schema

Each entry in the JSON array represents one system property/environment variable with the following fields:

### Required Fields

#### `sysprop` (string)
The Java system property name in dot notation (lowercase).

Example: `"lucee.admin.enabled"`

#### `envvar` (string)
The environment variable name in SCREAMING_SNAKE_CASE (uppercase with underscores).

Example: `"LUCEE_ADMIN_ENABLED"`

#### `desc` (string)
Human-readable description of what the property does.

Guidelines:
- Be concise but complete
- Mention default values where applicable
- Include valid values/ranges if relevant
- Reference related settings or LDEV tickets where appropriate
- Should not be empty - if unknown, investigate the codebase

Example: `"Boolean value to enable or disable the Lucee admin interface, default is true"`

#### `category` (string)
Primary functional category. Must be ONE of:

- `request` - Request handling, timeouts, queuing
- `security` - Authentication, encryption, access control
- `compiler` - Code compilation, whitespace, type checking
- `performance` - Caching, threading, optimization
- `logging` - Log configuration and output
- `application` - Application listener settings
- `library` - Tag/function library loading
- `debugging` - Debug options and logging
- `query` - Query settings
- `datasource` - Database connection settings
- `session` - Session and cookie management
- `charset` - Character encoding
- `deployment` - Installation and configuration
- `mail` - Email settings
- `osgi` - Maven / OSGI
- `extensions` - Extensions

#### `tags` (array of strings)
Array of related CFML tags (without `<>` brackets).

Examples:
- `["cfquery", "cfqueryparam"]`
- `["cflock"]`

Omit if empty

#### `functions` (array of strings)
Array of related CFML functions.

Examples:
- `["queryExecute", "queryNew"]`
- `["isDefined", "structKeyExists"]`

Omit if empty

#### `type` (string)
The CFML data type expected for this property value. Must be ONE of:

- `boolean` - true/false values
- `string` - text, paths, URLs, comma-separated lists
- `numeric` - integers or floating-point numbers
- `timespan` - duration/timeout values (milliseconds, seconds)

### Optional Fields

#### `default` (mixed)

The default value used when the property is not explicitly set.

Type depends on the property's `type` field:

- For **boolean**: `true` or `false`
- For **string**: `"string value"` or `null`
- For **numeric**: number value (e.g., `50`, `10000`) or `null`
- For **timespan**: string with units (e.g., `"20 seconds"`) or `null`

Special cases:

- `null` - No explicit default, falls through to other configuration
- Version-dependent: `"false (Lucee 6), true (Lucee 7)"` - Documents version-specific defaults

Examples:

- `"default": true` - Boolean true
- `"default": "cfmx_compat"` - String default
- `"default": 50` - Numeric default
- `"default": null` - No explicit default

#### `introduced` (string)
Version when this property was first introduced.

Format: `"X.Y.Z.B"` (major.minor.patch.build)

Example: `"7.0.1.13"`

#### `deprecated` (string)
Version when this property was deprecated or removed.

Format: `"X.Y"` (major.minor)

Example: `"6.2"`

#### `defaultSince` (string)
Version when the default behavior changed.

Format: `"X.Y"` (major.minor)

Example: `"7.0"`

## Complete Example

```json
{
	"sysprop": "lucee.tag.populate.localscope",
	"envvar": "LUCEE_TAG_POPULATE_LOCALSCOPE",
	"desc": "Boolean value that controls whether tags like cflock and cfquery populate their default result variables to local scope when inside a function. Default is true (variables go to local scope). Set to false to restore pre-LDEV-5416 behavior where variables go to variables scope",
	"category": "compiler",
	"tags": ["cflock", "cfquery", "cffile", "cfthread"],
	"functions": [],
	"type": "boolean",
	"default": true,
	"introduced": "7.0.1.13"
}
```

## How to Add a New Entry

1. **Find the Java code** - Search for where `SystemUtil.getSystemPropOrEnvVar()` is called with your property name

   ```bash
   grep -r "getSystemPropOrEnvVar(\"your.property" core/src/main/java
   ```

2. **Determine the type** - Look at how the value is cast:
   - `Caster.toBoolean()` → type: `"boolean"`
   - `Caster.toIntValue()` / `Caster.toLong()` → type: `"numeric"`
   - `Caster.toDoubleValue()` → type: `"numeric"`
   - Direct string or no casting → type: `"string"`
   - Timeout/duration values → type: `"timespan"`

3. **Extract the default value** - Look at the second parameter in `getSystemPropOrEnvVar(name, defaultValue)`
   - Add the `"default"` field with the appropriate value
   - Use `null` if no default is specified

4. **Write the description** - Based on code analysis and existing documentation
   - Do NOT repeat the default in the description if you're using the `default` field
   - Reference LDEV tickets where applicable

5. **Choose the category** - Pick the most relevant one from the list above
   - If multiple categories apply, choose the primary use case

6. **Add tags and functions** - Include any CFML tags or functions directly affected
   - Leave empty arrays if none apply (do not omit the fields)

7. **Add version info** - If this is a new property:
   - `"introduced"`: Current version from `loader/pom.xml` (format: `"X.Y.Z.B"`)
   - `"deprecated"`: If replacing an old property

8. **Validate JSON** - Ensure the file is still valid JSON after your changes

   ```bash
   python -m json.tool sysprop-envvar.json > /dev/null
   ```

## Validation

Before committing changes, validate the JSON:

```bash
# Using Python
python -m json.tool sysprop-envvar.json > /dev/null && echo "Valid" || echo "Invalid"

# Using jq
jq empty sysprop-envvar.json && echo "Valid" || echo "Invalid"
```

## Maintenance Notes

- When adding a new system property in Java code, add it to this file
- When deprecating a property, add the `"deprecated"` field
- Keep descriptions synchronized with published documentation
- Use exact wording from docs where applicable
- Review and update descriptions when behavior changes
- If mentioning a default in the description, ensure it matches the `default` field

## Related Files

- Java: `{lucee-root}/core/src/main/java/lucee/commons/io/SystemUtil.java` - Contains `getSystemPropOrEnvVar()`
- Docs: `{lucee-docs}/builds/html/recipes/environment-variables-system-properties.md` - Published documentation
- Config: Various `ConfigImpl.java` files where properties are read and used
