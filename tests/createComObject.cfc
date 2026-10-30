component extends="org.lucee.cfml.test.LuceeTestCase" labels="comextension" {

	function run( testResults, testBox ) {

		describe( title="createComObject on Windows", skip=isNotWindows(), body=function() {

			it( "Scripting.Dictionary: Add, Item, Count, Exists", function() {
				var dict = createComObject( "Scripting.Dictionary" );
				try {
					dict.Add( "a", 1 );
					dict.Add( "b", "two" );
					expect( dict.Count ).toBe( 2 );
					expect( dict.Exists( "a" ) ).toBeTrue();
					expect( dict.Exists( "c" ) ).toBeFalse();
					expect( dict.Item( "a" ) ).toBe( 1 );
					expect( dict.Item( "b" ) ).toBe( "two" );
				}
				finally {
					releaseComObject( dict );
				}
			});

			it( "Scripting.Dictionary: a method that returns an array", function() {
				var dict = createComObject( "Scripting.Dictionary" );
				try {
					dict.Add( "x", 1 );
					dict.Add( "y", 2 );
					var keys = dict.Keys();
					expect( isArray( keys ) ).toBeTrue();
					expect( arrayLen( keys ) ).toBe( 2 );
					expect( arrayToList( keys ) ).toBe( "x,y" );
				}
				finally {
					releaseComObject( dict );
				}
			});

			it( "Scripting.Dictionary: loop over the object", function() {
				var dict = createComObject( "Scripting.Dictionary" );
				try {
					dict.Add( "k1", 1 );
					dict.Add( "k2", 2 );
					var found = [];
					for ( var k in dict ) arrayAppend( found, k );
					expect( found ).toBe( [ "k1", "k2" ] );
				}
				finally {
					releaseComObject( dict );
				}
			});

			it( "Scripting.Dictionary: set a property", function() {
				var dict = createComObject( "Scripting.Dictionary" );
				try {
					dict.CompareMode = 1; // text compare, keys are not case sensitive
					dict.Add( "Key", 1 );
					expect( dict.Exists( "KEY" ) ).toBeTrue();
				}
				finally {
					releaseComObject( dict );
				}
			});

			it( "WScript.Shell: ExpandEnvironmentStrings", function() {
				var shell = createComObject( "WScript.Shell" );
				try {
					var windir = shell.ExpandEnvironmentStrings( "%windir%" );
					expect( len( windir ) ).toBeGT( 0 );
					expect( windir ).notToBe( "%windir%" );
					expect( directoryExists( windir ) ).toBeTrue();
				}
				finally {
					releaseComObject( shell );
				}
			});

			it( "WScript.Shell: a property that returns a COM object", function() {
				var shell = createComObject( "WScript.Shell" );
				try {
					var env = shell.Environment( "PROCESS" );
					expect( isObject( env ) ).toBeTrue();
					expect( len( env.Item( "windir" ) ) ).toBeGT( 0 );
					releaseComObject( env );
				}
				finally {
					releaseComObject( shell );
				}
			});

			it( "Scripting.FileSystemObject: folders and files, dates are real dates (not 1970)", function() {
				var fso = createComObject( "Scripting.FileSystemObject" );
				var dir = getTempDirectory() & "com-extension-" & createUUID() & "/";
				directoryCreate( dir );
				try {
					var path = dir & "test.txt";
					fileWrite( path, "hello" );
					expect( fso.FolderExists( dir ) ).toBeTrue();
					expect( fso.FileExists( path ) ).toBeTrue();
					expect( fso.FileExists( path & ".nope" ) ).toBeFalse();

					var file = fso.GetFile( path );
					expect( file.Name ).toBe( "test.txt" );
					expect( file.Size ).toBe( 5 );
					var modified = file.DateLastModified;
					expect( isDate( modified ) ).toBeTrue();
					expect( year( modified ) ).toBeGTE( 2024 );
					expect( abs( dateDiff( "s", modified, now() ) ) ).toBeLT( 300 );
					releaseComObject( file );
				}
				finally {
					releaseComObject( fso );
					directoryDelete( dir, true );
				}
			});

			it( "Scripting.FileSystemObject: the files of a folder", function() {
				var fso = createComObject( "Scripting.FileSystemObject" );
				var dir = getTempDirectory() & "com-extension-" & createUUID() & "/";
				directoryCreate( dir );
				try {
					fileWrite( dir & "a.txt", "a" );
					fileWrite( dir & "b.txt", "b" );
					var folder = fso.GetFolder( dir );
					var files = folder.Files;
					expect( files.Count ).toBe( 2 );
					// the elements of a collection of COM objects are COM objects, they are not keys, so no loop over the collection here
					expect( files.Item( "a.txt" ).Name ).toBe( "a.txt" );
					expect( files.Item( "b.txt" ).Name ).toBe( "b.txt" );
					releaseComObject( files );
					releaseComObject( folder );
				}
				finally {
					releaseComObject( fso );
					directoryDelete( dir, true );
				}
			});

			it( "a COM object can be passed as an argument to another COM object", function() {
				var fso = createComObject( "Scripting.FileSystemObject" );
				var dict = createComObject( "Scripting.Dictionary" );
				var dir = getTempDirectory() & "com-extension-" & createUUID() & "/";
				directoryCreate( dir );
				try {
					fileWrite( dir & "arg.txt", "x" );
					var file = fso.GetFile( dir & "arg.txt" );
					dict.Add( "file", file );
					expect( dict.Item( "file" ).Name ).toBe( "arg.txt" );
					releaseComObject( file );
				}
				finally {
					releaseComObject( dict );
					releaseComObject( fso );
					directoryDelete( dir, true );
				}
			});

			it( "an unknown property gives the default value, a missing method an exception", function() {
				var dict = createComObject( "Scripting.Dictionary" );
				try {
					expect( isNull( dict.thisDoesNotExist ) ).toBeTrue();
					expect( dict.thisDoesNotExist ?: "default" ).toBe( "default" );
					expect( function() {
						dict.thisMethodDoesNotExist();
					}).toThrow( "application", "thisMethodDoesNotExist" );
				}
				finally {
					releaseComObject( dict );
				}
			});

			it( "an unknown ProgID throws an exception with the ProgID", function() {
				expect( function() {
					createComObject( "This.ProgId.DoesNotExist" );
				}).toThrow( "application", "This.ProgId.DoesNotExist" );
			});

			it( "many objects can be created and released", function() {
				loop times=300 {
					var fso = createComObject( "Scripting.FileSystemObject" );
					expect( fso.FolderExists( getTempDirectory() ) ).toBeTrue();
					releaseComObject( fso );
				}
			});

			it( "a COM object can be dumped", function() {
				var dict = createComObject( "Scripting.Dictionary" );
				try {
					savecontent variable="local.out" {
						dump( dict );
					}
					expect( out ).toInclude( "COM Object" );
				}
				finally {
					releaseComObject( dict );
				}
			});
		});

		describe( title="createComObject on other platforms than Windows", skip=!isNotWindows(), body=function() {

			it( "gives a clear error", function() {
				expect( function() {
					createComObject( "Scripting.FileSystemObject" );
				}).toThrow( "application", "only supported on Windows" );
			});
		});

		describe( "createComObject and releaseComObject", function() {

			it( "createComObject requires a class name", function() {
				expect( function() {
					createComObject( "" );
				}).toThrow();
			});

			it( "releaseComObject ignores everything that is not a COM object", function() {
				releaseComObject( "nothing" );
				releaseComObject( {} );
			});
		});
	}

	private function isNotWindows() {
		return !findNoCase( "windows", server.os.name );
	}
}
