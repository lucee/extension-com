# Lucee COM Extension

COM/ActiveX support for Lucee (**Windows only**), based on [Jacob](https://github.com/freemansoft/jacob-project) 1.21.

```cfml
fso = createComObject( "Scripting.FileSystemObject" );
echo( fso.FolderExists( "C:\temp" ) );
releaseComObject( fso );
```

Functions:

* `createComObject( className )` creates a COM object, `className` is the name (ProgID) of the COM object
* `releaseComObject( comObject )` releases a COM object and frees up the resources it used

## Lucee 9: this was part of the core before

Until Lucee 8 COM objects were created with `createObject( "com", className )` or `<cfobject type="com" ...>`. This is **not supported anymore in Lucee 9**, the COM support moved to this extension, which is **not required** by Lucee: install it only if you need COM.

To migrate replace `createObject( "com", className )` / `<cfobject type="com" class="className" name="obj">` with `createComObject( className )`. The returned object behaves like the COM object of the core did.

See [LDEV-6763](https://luceeserver.atlassian.net/browse/LDEV-6763).

## Build

```
mvn clean install -Dgoal=install
```

The official Jacob release is not on Maven Central, the build installs `source/java/lib/jacob-1.21.jar` into the local Maven repository first (target `install-jacob` in `build.xml`). The native libraries `jacob-1.21-x64.dll` and `jacob-1.21-x86.dll` of the same release are in `source/java/src/resources/jacob`, on first use they are copied to `lucee-server/bin`.

Jacob is licensed under the LGPL 2.1 (`source/java/src/resources/jacob/LICENSE.TXT`).

## Tests

The tests in `tests/` need Windows for the COM parts (they are skipped on other platforms), run them with the Lucee test suite (`testAdditional`).
