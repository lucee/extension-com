package org.lucee.extension.com;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Arrays;

import com.jacob.com.LibraryLoader;

import lucee.commons.io.res.Resource;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.PageContext;
import lucee.runtime.config.Config;
import lucee.runtime.config.ConfigWeb;
import lucee.runtime.exp.PageException;
import lucee.runtime.util.SystemUtil;

/**
 * Jacob needs a native library (jacob-&lt;version&gt;-x64.dll or jacob-&lt;version&gt;-x86.dll). It is part of this extension, on
 * first use it is copied to the "bin" folder of the Lucee server context and Jacob is told where it is.
 * <p>
 * A JNI library can only be loaded by one classloader per JVM. When the extension is updated or reloaded without a restart of the
 * server, the new classloader cannot load the library again, restart the server in that case.
 */
final class DllSetup {

	static final String JACOB_VERSION = "1.21";

	/**
	 * Jacob keeps a table of every object created by a thread until the thread ends (ComThread.Release()), with this property the
	 * objects are released by the garbage collector instead. It has to be set before the first Jacob class is loaded.
	 */
	static final String PROP_AUTOGC = "com.jacob.autogc";

	/**
	 * optional: full path of a native library of Jacob 1.21 to use instead of the bundled one
	 */
	static final String PROP_DLL_PATH = "lucee.extension.com.dll.path";

	private static final String[] INITIALIZE = { "com.jacob.com.JacobObject", "com.jacob.com.ComThread", "com.jacob.com.ROT", "com.jacob.com.STA" };

	private static volatile boolean done;

	static void setup(PageContext pc) throws PageException {
		if (done) return;
		synchronized (DllSetup.class) {
			if (done) return;
			CFMLEngine eng = CFMLEngineFactory.getInstance();
			SystemUtil su = eng.getSystemUtil();
			if (!su.isWindows()) {
				throw eng.getExceptionUtil().createApplicationException("COM objects are only supported on Windows");
			}
			String arch = System.getProperty("os.arch", "");
			if (arch.toLowerCase().contains("aarch64") || arch.toLowerCase().contains("arm")) {
				throw eng.getExceptionUtil().createApplicationException("COM objects are not supported on Windows ARM (os.arch is [" + arch + "]), the Jacob library only supports x86 and x64");
			}

			if (System.getProperty(PROP_AUTOGC) == null) System.setProperty(PROP_AUTOGC, "true");

			// an explicitly configured library (own property, the properties of Jacob are shared with the COM support of the core of
			// Lucee 7 and 8, which has its own, different native library)
			String configured = System.getProperty(PROP_DLL_PATH);
			if (!isEmpty(configured)) {
				loadLibrary(eng, new File(configured.trim()), new File(configured.trim()).getName());
				done = true;
				return;
			}

			boolean is64 = su.getJREArch() == SystemUtil.ARCH_64;
			String name = "jacob-" + JACOB_VERSION + "-" + (is64 ? "x64" : "x86") + ".dll";
			File dll = null;
			File tmp = null;
			try {
				dll = new File(getBinDirectory(pc), name);
				byte[] bundled = readBundled(name);
				// a stale or truncated file with the right name is replaced
				if (!dll.isFile() || !Arrays.equals(sha256(Files.readAllBytes(dll.toPath())), sha256(bundled))) {
					dll.getParentFile().mkdirs();
					// copy to a temp file first, so a second server starting at the same time never sees a partial file
					tmp = new File(dll.getParentFile(), name + "." + System.nanoTime() + ".tmp");
					Files.write(tmp.toPath(), bundled);
					try {
						Files.move(tmp.toPath(), dll.toPath(), StandardCopyOption.REPLACE_EXISTING);
					}
					catch (IOException e) {
						// the file is in use by another instance, which is fine as long as it is there
						if (!dll.isFile()) throw e;
					}
				}
				loadLibrary(eng, dll, name);
			}
			catch (IOException e) {
				throw eng.getExceptionUtil().createApplicationException("Failed to deploy the native library [" + name + "] of Jacob to [" + dll + "]: " + e.getMessage()
						+ ", make sure the folder is writable or copy the file there manually and set the system property [" + LibraryLoader.JACOB_DLL_PATH + "]");
			}
			finally {
				if (tmp != null && tmp.exists()) tmp.delete();
			}
			done = true;
		}
	}

	/**
	 * Jacob finds its native library with the system properties jacob.dll.path and jacob.dll.name. They are global, and the COM support
	 * of the core of Lucee 7 and 8 uses the same properties for a different library, so they are only set while the library is loaded
	 * and then set back.
	 */
	private static void loadLibrary(CFMLEngine eng, File dll, String name) throws PageException {
		String oldPath = System.getProperty(LibraryLoader.JACOB_DLL_PATH);
		String oldName = System.getProperty(LibraryLoader.JACOB_DLL_NAME);
		try {
			System.setProperty(LibraryLoader.JACOB_DLL_PATH, dll.getAbsolutePath());
			System.setProperty(LibraryLoader.JACOB_DLL_NAME, name);
			LibraryLoader.loadJacobLibrary();
			// these classes load the library again in their static initializers, initialize them while the properties are set
			for (String className: INITIALIZE) {
				Class.forName(className, true, DllSetup.class.getClassLoader());
			}
		}
		catch (ClassNotFoundException e) {
			throw eng.getExceptionUtil().createApplicationException("Jacob class not found: " + e.getMessage());
		}
		catch (UnsatisfiedLinkError | ExceptionInInitializerError e) {
			throw eng.getExceptionUtil().createApplicationException("Failed to load the native library [" + dll + "] of Jacob: " + e.getMessage(),
					"A native library can only be loaded once per JVM and classloader, restart the server if the extension was updated or reloaded.");
		}
		finally {
			restore(LibraryLoader.JACOB_DLL_PATH, oldPath);
			restore(LibraryLoader.JACOB_DLL_NAME, oldName);
		}
	}

	private static void restore(String key, String value) {
		if (value == null) System.clearProperty(key);
		else System.setProperty(key, value);
	}

	private static byte[] readBundled(String name) throws IOException {
		try (InputStream is = DllSetup.class.getResourceAsStream("/resources/jacob/" + name)) {
			if (is == null) throw new IOException("the native library [" + name + "] is not part of the extension");
			return is.readAllBytes();
		}
	}

	private static byte[] sha256(byte[] data) throws IOException {
		try {
			return MessageDigest.getInstance("SHA-256").digest(data);
		}
		catch (java.security.NoSuchAlgorithmException e) {
			throw new IOException(e);
		}
	}

	private static boolean isEmpty(String str) {
		return str == null || str.trim().isEmpty();
	}

	private static File getBinDirectory(PageContext pc) {
		Config config = pc.getConfig();
		Resource dir = (config instanceof ConfigWeb) ? ((ConfigWeb) config).getConfigServerDir() : config.getConfigDir();
		return new File(dir.getAbsolutePath(), "bin");
	}
}
