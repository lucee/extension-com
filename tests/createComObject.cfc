component extends="org.lucee.cfml.test.LuceeTestCase" labels="com" {

	function run( testResults, testBox ) {
		describe( "createComObject", function() {

			it( title="creates a COM object and calls methods and properties", skip=isNotWindows(), body=function( currentSpec ) {
				var fso = createComObject( "Scripting.FileSystemObject" );
				expect( isObject( fso ) ).toBeTrue();
				var tmp = getTempDirectory();
				expect( fso.FolderExists( tmp ) ).toBeTrue();
				expect( fso.FolderExists( tmp & "this-folder-does-not-exist-" & createUUID() ) ).toBeFalse();
				releaseComObject( fso );
			});

			it( title="works with a sub object (a property that returns a COM object)", skip=isNotWindows(), body=function( currentSpec ) {
				var shell = createComObject( "WScript.Shell" );
				var env = shell.Environment( "PROCESS" );
				expect( isObject( env ) ).toBeTrue();
				releaseComObject( env );
				releaseComObject( shell );
			});

			it( title="WScript.Shell can be instantiated (LDEV-3314)", skip=isNotWindows(), body=function( currentSpec ) {
				var comObj = createComObject( "WScript.Shell" );
				expect( isObject( comObj ) ).toBeTrue();
				releaseComObject( comObj );
			});

			it( title="gives a clear error on other platforms than Windows", skip=!isNotWindows(), body=function( currentSpec ) {
				expect( function() {
					createComObject( "Scripting.FileSystemObject" );
				}).toThrow( "application", "only supported on Windows" );
			});

			it( title="requires a class name", body=function( currentSpec ) {
				expect( function() {
					createComObject( "" );
				}).toThrow();
			});

			it( title="releaseComObject ignores everything that is not a COM object", body=function( currentSpec ) {
				releaseComObject( "nothing" );
				releaseComObject( {} );
			});

		});
	}

	private function isNotWindows() {
		return !findNoCase( "windows", server.os.name );
	}
}
