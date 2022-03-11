component {
	this.name = "LDEV3641";
	this.ormEnabled = true;
	
	this.datasource={
		class: 'org.h2.Driver'
		, bundleName: 'org.h2'
		, connectionString: 'jdbc:h2:#getDirectoryFromPath(getCurrentTemplatePath())#/datasource/db;MODE=MySQL'
	};
	
	this.ormEnabled = true;
	this.ormSettings = {
		dbcreate = "dropcreate"
	};
	
	function onRequestStart(){
		query {
			echo("DROP TABLE IF EXISTS test3641");
		}
		
		query {
			echo("CREATE TABLE test3641 (
				id int(11) NOT NULL AUTO_INCREMENT,
				name varchar(50),
				PRIMARY KEY (id)
				)"
			);
		}
	
		query {
			echo("INSERT INTO test3641 (name) VALUES ( 'lucee')");
		}
	}

	function onRequestEnd() {
		var javaIoFile=createObject("java","java.io.File");
		loop array=DirectoryList(
			path=getDirectoryFromPath(getCurrentTemplatePath()), 
			recurse=true, filter="*.db") item="local.path"  {
			fileDeleteOnExit(javaIoFile,path);
			}
		}
		
	private function fileDeleteOnExit(required javaIoFile, required string path) {
		var file=javaIoFile.init(arguments.path);
		if(!file.isFile())file=javaIoFile.init(expandPath(arguments.path));
		if(file.isFile()) file.deleteOnExit();
	}
}