component extends="org.lucee.cfml.test.LuceeTestCase" labels="tld" {

	function run( testResults, testBox ) {
		describe( "LDEV-5901: Validate attribute-groups in TLDs", function() {

			it( "attribute-groups reference valid attributes for cfloop", function() {
				var tagData = getTagData( "cf", "loop" );
				var errors = validateAttributeGroups( tagData );

				expect( errors ).toBeEmpty(
					"cfloop has invalid attribute-groups: #serializeJSON( errors )#"
				);
			});

			it( "all tags with attribute-groups have valid references", function() {
				var tags = getTagList().cf;
				var allErrors = {};

				for( var tagName in tags ) {
					var tagData = getTagData( "cf", tagName );

					// Skip if no attribute groups
					if( !structKeyExists( tagData, "attributeGroups" ) || !arrayLen( tagData.attributeGroups ) ) {
						continue;
					}

					var errors = validateAttributeGroups( tagData );
					if( arrayLen( errors ) > 0 ) {
						allErrors[ tagName ] = errors;
					}
				}

				expect( allErrors ).toBeEmpty(
					"Tags have invalid attribute-groups: #serializeJSON( allErrors )#"
				);
			});
		});
	}

	private array function validateAttributeGroups( required struct tagData ) {
		var errors = [];

		// Get all valid attribute names for this tag
		var validAttributes = {};
		for( var attr in tagData.attributes ) {
			validAttributes[ attr ] = true;
		}

		// Check each group
		if( structKeyExists( tagData, "attributeGroups" ) ) {
			for( var group in tagData.attributeGroups ) {

				// Validate group has required fields
				if( !structKeyExists( group, "name" ) || !len( group.name ) ) {
					arrayAppend( errors, "Group missing 'name'" );
					continue;
				}

				if( !structKeyExists( group, "label" ) || !len( group.label ) ) {
					arrayAppend( errors, "Group '#group.name#' missing 'label'" );
				}

				if( !structKeyExists( group, "description" ) || !len( group.description ) ) {
					arrayAppend( errors, "Group '#group.name#' missing 'description'" );
				}

				if( !structKeyExists( group, "attributes" ) ) {
					arrayAppend( errors, "Group '#group.name#' missing 'attributes'" );
					continue;
				}

				// Check each attribute in the group exists (skip if empty string - means no action-specific attributes)
				if( len( group.attributes ) ) {
					var groupAttrs = listToArray( group.attributes );
					for( var attrName in groupAttrs ) {
						if( !structKeyExists( validAttributes, attrName ) ) {
							arrayAppend( errors,
								"Group '#group.name#' references non-existent attribute '#attrName#'"
							);
						}
					}
				}
			}
		}

		return errors;
	}
}
