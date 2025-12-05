<cfcomponent output="false" javasettings="#{ maven: ['org.apache.commons:commons-lang3:3.12.0'] }#">
	<!--- Traditional tag syntax with struct via # expression --->
	<!--- Expected: Probably errors or doesn't work --->
	<cffunction name="test" access="public" returntype="string">
		<cfreturn "tag_struct">
	</cffunction>
</cfcomponent>
