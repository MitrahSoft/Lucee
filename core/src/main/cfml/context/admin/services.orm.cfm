<cfset error.message="">
<cfset error.detail="">
<cfparam name="url.action2" default="list">
<cfparam name="form.mainAction" default="none">
<cfparam name="form.subAction" default="none">

<cfset hasOrtusORM = extensionExists( "D062D72F-F8A2-46F0-8CBC91325B2F067B" )>
<cfset hasLegacyORM = extensionExists( "FAD1E8CB-4F45-4184-86359145767C29DE" )>

<cftry>
	<cfadmin
		action="getORMEngine"
		type="#request.adminType#"
		password="#session["password"&request.adminType]#"
		returnVariable="engine">
	<cfset hasORMEngine = structKeyExists( engine, "class" ) and len( trim( engine.class ) ) and engine.class neq "lucee.runtime.orm.DummyORMEngine">
	<cfcatch>
		<cfset hasORMEngine = false>
	</cfcatch>
</cftry>

<cfadmin
	action="securityManager"
	type="#request.adminType#"
	password="#session["password"&request.adminType]#"
	returnVariable="hasAccess"
	secType="orm"
	secValue="yes">

<cfif not hasORMEngine>
	<cfset extLink = request.self & "?action=ext.applications&action2=detail&id=D062D72F-F8A2-46F0-8CBC91325B2F067B&name=" & URLEncodedFormat( 'Ortus ORM Extension' )>
	<cfoutput>
		<div class="error">
			#stText.Settings.orm.extensionmissing#
		</div>
		<p>
			<a href="#extLink#">#stText.Settings.orm.extensionmissinglink#</a>
		</p>
	</cfoutput>
<cfelse>
	<cfadmin
		action="getORMSetting"
		type="#request.adminType#"
		password="#session["password"&request.adminType]#"
		returnVariable="settings">
	<cfinclude template="services.orm.list.cfm"/>
</cfif>