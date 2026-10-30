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

The Lucee core PR that removed COM from the core: [lucee/Lucee#3402](https://github.com/lucee/Lucee/pull/3402) (9.0). Lucee 9.0 builds have no COM at all without this extension.

Exceptions: the exception classes of Jacob are `com.jacob.com.*` (they were `lucee.commons.jacob.com.*` in the core), which matters only if you catch by Java type. The extension itself throws Lucee `application` exceptions (unknown ProgID, failed property or method call), with the ProgID and the message of Windows.

See [LDEV-6763](https://luceeserver.atlassian.net/browse/LDEV-6763).

## Which Lucee versions

The extension works with Lucee 7.0.5.41 and newer (`luceeCoreVersion`, the loader API is all it needs; the first 7.0 builds could not load the extension class reliably in CI), so code can be migrated to `createComObject()` before the upgrade to Lucee 9. Lucee 7.x and 8.0 still have COM in the core, there the extension is redundant. From Lucee 9.0 on it is the only way to use COM.

## Threads and objects

Jacob initialises every thread that touches a COM object as a multi threaded apartment (MTA). COM components that only work in a single threaded apartment (STA) may not work, or need more than this extension.

Jacob keeps a table of all the objects created by a thread until the thread ends (`ComThread.Release()`), a request thread that creates COM objects in a loop would hold on to all of them. The extension sets the system property `com.jacob.autogc=true` before the first object is created (unless you set it yourself), then the objects are released by the garbage collector. Call `releaseComObject()` when you are done with an object anyway.

## Native library

The extension copies `jacob-1.21-x64.dll` (or `-x86`) to `lucee-server/bin` on first use, and replaces the file when it differs from the bundled one. If you set the system property `lucee.extension.com.dll.path` to the full path of a native library of Jacob 1.21, that library is used instead (see LGPL below, you can replace the library). The properties of Jacob itself (`jacob.dll.path`) are not used, because the COM support in the core of Lucee 7 and 8 uses the same ones for a different library; the extension only sets them while it loads its library and sets them back.

A JNI library can be loaded only once per JVM and classloader: if the extension is updated or reloaded without a restart of the server, the new version cannot load the library, `createComObject` throws an exception that tells you so, restart the server. Windows on ARM is not supported by Jacob 1.21 (only x86 and x64).

## Third party: Jacob

[Jacob](https://github.com/freemansoft/jacob-project) (Java COM Bridge), version 1.21. Original licence: GNU LGPL 2.1 (`source/java/src/resources/jacob/LICENSE.TXT`). The extension uses the build published on Maven Central as `io.github.osobolev:jacob:1.21` (source: https://github.com/osobolev/jacob-project, its POM states Apache 2.0), including the native libraries of the same release, which are downloaded from Central at build time and are not committed here.

| File (Maven Central, `io/github/osobolev/jacob/1.21/`) | SHA-256 |
|---|---|
| `jacob-1.21.jar` | `4b41764013b264ea46b9f659de7914559677b80c5918dae0beb6dcd42f7634ef` |
| `jacob-1.21-x64.dll` | `fbb3d16d2c3ee947fa3f9fb53e98b2fb4f0f4517abebc7005dfd082e928659af` |
| `jacob-1.21-x86.dll` | `ca5ebd5e58d9d6bd8489d5ccf3b4f25ff2b11b2fc9eaa31ad47a8a43bfa913d5` |

LGPL: the Jacob library is a separate jar (`maven/io/github/osobolev/jacob/` in the extension) and a separate native library, both can be replaced by your own build of Jacob (same API), put your jar in the `mvn` folder of the server and set `lucee.extension.com.dll.path`.

## Build

```
mvn clean install -Dgoal=install
```

Jacob (jar and native libraries) comes from Maven Central, see above. The version is the property `jacob.version` in `source/java/pom.xml`.

## Tests

The tests in `tests/` run with the Lucee test suite (`testAdditional`, label `com`). The COM tests need Windows, the CI workflow has a `windows-latest` job for them, they are skipped on other platforms.

## Releases

Version: `extension.version.base` + build number + `extension.version.qualifier` in `pom.xml`. Every push to `main` publishes a new build (highest of the git tags and the versions on Maven Central and the snapshot repository + 1) as `-SNAPSHOT`, a manual run of the workflow can publish an ALPHA, BETA, RC or final release.
