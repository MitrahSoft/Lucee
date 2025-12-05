<cfscript>
// For loop with null condition - infinite loop with break
for (i = 1; ; i++) {
	if (i > 3) break;
	writeOutput(i);
}
</cfscript>
