<cfcomponent output="false" javasettings={ maven: ["org.apache.commons:commons-lang3:3.12.0"] }>
	<!--- Traditional tag syntax with unquoted struct literal --->
	<!--- Expected: Probably parse error or misparsing --->

	<cffunction name="test" access="public" returntype="string">
		<cfreturn "tag_unquoted_struct">
	</cffunction>

</cfcomponent>
