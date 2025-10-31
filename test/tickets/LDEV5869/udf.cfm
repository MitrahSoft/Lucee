<cfset cookie.testUDF = "udfValue=test">

<cffunction name="testFunction" returntype="struct">
	<cfreturn {
		"success": true,
		"cookieInUDF": cookie.testUDF ?: ""
	}>
</cffunction>

<cfset result = testFunction()>
<cfoutput>#serializeJSON( result )#</cfoutput>
