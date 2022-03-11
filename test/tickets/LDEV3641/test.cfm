<cfparam name="FORM.scene" default="1">
<cfscript>

	if( form.scene == 1 ) {
		res = ORMExecuteQuery("select name From test3641 where id = ? and name= ?" , [1,'lucee']);
	}
	
	if( form.scene == 2 ) {
		res = ORMExecuteQuery("select name From test3641 where id = ?1 and name= ?2" , [1,'lucee']);
	}
	
	writeoutput(res[1]);
</cfscript>
