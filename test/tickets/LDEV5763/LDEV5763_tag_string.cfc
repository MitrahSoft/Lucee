<cfcomponent output="false" javasettings='{
		maven = ["org.apache.commons:commons-lang3:3.12.0"]
	}'>
	<!--- Traditional tag syntax with string --->
	<!--- Expected: WORKS --->
	<cffunction name="test" access="public" returntype="string">
		<cfreturn "tag_string">
	</cffunction>
</cfcomponent>
