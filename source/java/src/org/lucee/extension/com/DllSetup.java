package org.lucee.extension.com;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

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
 */
final class DllSetup {

	static final String JACOB_VERSION = "1.21";

	private static boolean done;

	static void setup(PageContext pc) throws PageException {
		if (done) return;
		synchronized (DllSetup.class) {
			if (done) return;
			CFMLEngine eng = CFMLEngineFactory.getInstance();
			SystemUtil su = eng.getSystemUtil();
			if (!su.isWindows()) {
				throw eng.getExceptionUtil().createApplicationException("COM objects are only supported on Windows");
			}
			boolean is64 = su.getJREArch() == SystemUtil.ARCH_64;
			String name = "jacob-" + JACOB_VERSION + "-" + (is64 ? "x64" : "x86") + ".dll";
			try {
				File dll = new File(getBinDirectory(pc), name);
				if (!dll.isFile() || dll.length() == 0) {
					dll.getParentFile().mkdirs();
					try (InputStream is = DllSetup.class.getResourceAsStream("/resources/jacob/" + name)) {
						if (is == null) throw new IOException("the native library [" + name + "] is not part of the extension");
						// copy to a temp file first, so a second server starting at the same time never sees a partial file
						File tmp = new File(dll.getParentFile(), name + "." + System.nanoTime() + ".tmp");
						Files.copy(is, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
						try {
							Files.move(tmp.toPath(), dll.toPath(), StandardCopyOption.REPLACE_EXISTING);
						}
						catch (IOException e) {
							// the file is in use by another instance, which is fine as long as it is there
							tmp.delete();
							if (!dll.isFile()) throw e;
						}
					}
				}
				System.setProperty(LibraryLoader.JACOB_DLL_PATH, dll.getAbsolutePath());
				System.setProperty(LibraryLoader.JACOB_DLL_NAME, name);
			}
			catch (IOException e) {
				throw eng.getCastUtil().toPageException(e);
			}
			done = true;
		}
	}

	private static File getBinDirectory(PageContext pc) {
		Config config = pc.getConfig();
		Resource dir = (config instanceof ConfigWeb) ? ((ConfigWeb) config).getConfigServerDir() : config.getConfigDir();
		return new File(dir.getAbsolutePath(), "bin");
	}
}
