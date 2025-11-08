<cfif structKeyExists( request, "compileErrors" ) && isStruct( request.compileErrors ) && structCount( request.compileErrors ) GT 0>
	<div class="error">
		<h3><cfoutput>#stText.Mappings.compileErrorsTitle#</cfoutput></h3>
		<cfif isDefined( "errorContext" )>
			<p><cfoutput>#replace( stText.Mappings.compileErrorsIntro, "{context}", errorContext )#</cfoutput></p>
		<cfelse>
			<p><cfoutput>#stText.Mappings.compileErrorsDesc#</cfoutput></p>
		</cfif>
		<cfloop collection="#request.compileErrors#" item="errorFile">
			<cfset errorInfo = request.compileErrors[ errorFile ]>
			<div style="margin: 10px 0; padding: 10px; background: ##f9f9f9; border-left: 3px solid ##cc0000; font-family: monospace; font-size: 0.9em;">
				<div style="font-weight: bold; color: ##cc0000;">
					<cfoutput>
						#errorFile#<cfif structKeyExists( errorInfo, "line" ) && errorInfo.line NEQ "">:#errorInfo.line#<cfif structKeyExists( errorInfo, "column" ) && errorInfo.column NEQ "">,#errorInfo.column#</cfif></cfif>
					</cfoutput>
				</div>
				<cfoutput>
					<div style="margin-top: 5px;">#HTMLEditFormat( errorInfo.message ?: "" )#</div>
					<cfif structKeyExists( errorInfo, "detail" ) && len( trim( errorInfo.detail ) )>
						<div style="margin-top: 5px; color: ##666;">#HTMLEditFormat( errorInfo.detail )#</div>
					</cfif>
				</cfoutput>
			</div>
		</cfloop>
	</div>
</cfif>
