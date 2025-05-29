<cftry>
    <cfset obj = "#createObject("java", "io.opentelemetry.api.trace.Span").current()#">
    <cfoutput>
        success
    </cfoutput>
    <cfcatch type="any">
        <cfoutput>
            #cfcatch.message#
        </cfoutput>
    </cfcatch>
</cftry>