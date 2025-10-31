<cfset cookie.testThread = "threadValue=equals">

<cfthread name="cookieThread" action="run">
	<cfset thread.cookieValue = cookie.testThread ?: "">
</cfthread>

<cfthread name="cookieThread" action="join" />

<cfset result = {
	"success": true,
	"cookieInThread": cfthread.cookieThread.cookieValue
}>
<cfoutput>#serializeJSON( result )#</cfoutput>
