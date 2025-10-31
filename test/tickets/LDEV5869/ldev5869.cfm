<cfif url.method == "testCookie">
	<cfset cookie[ url.cookieName ] = url.cookieValue>
	<cfoutput>#cookie[ url.cookieName ]#</cfoutput>
<cfelseif url.method == "testMultipleCookies">
	<cfset cookie.cookie1 = "value1=test">
	<cfset cookie.cookie2 = "value2==">
	<cfset cookie.cookie3 = "normalValue">
	<cfset result = {
		"cookie1": cookie.cookie1 ?: "",
		"cookie2": cookie.cookie2 ?: "",
		"cookie3": cookie.cookie3 ?: ""
	}>
	<cfoutput>#serializeJSON( result )#</cfoutput>
</cfif>
