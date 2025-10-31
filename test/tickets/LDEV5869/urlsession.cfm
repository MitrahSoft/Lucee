<cfset cookie.testCookie = "value=with=equals">
<cfset session.test = "sessionValue">

<cfset testUrl = "http://example.com/test.cfm">
<cfset formattedUrl = URLSessionFormat( testUrl )>

<cfset cookieValue = "">
<cfif structKeyExists( cookie, "testCookie" )>
	<cfset cookieValue = cookie.testCookie>
</cfif>

<cfset result = {
	"success": true,
	"url": formattedUrl,
	"urlContainsSession": ( findNoCase( "jsessionid", formattedUrl ) GT 0 OR findNoCase( "cfid", formattedUrl ) GT 0 ),
	"cookieValue": cookieValue,
	"sessionExists": structKeyExists( session, "test" )
}>
<cfoutput>#serializeJSON( result )#</cfoutput>
