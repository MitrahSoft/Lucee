package lucee.commons.lang;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.felix.framework.BundleWiringImpl.BundleClassLoader;

import lucee.commons.digest.HashUtil;
import lucee.commons.io.CharsetUtil;
import lucee.commons.io.IOUtil;
import lucee.commons.io.SystemUtil;
import lucee.commons.io.log.Log;
import lucee.commons.io.log.LogUtil;
import lucee.commons.io.res.Resource;
import lucee.commons.io.res.type.file.FileResource;
import lucee.commons.io.res.util.ResourceUtil;
import lucee.runtime.config.Config;
import lucee.runtime.converter.ConverterException;
import lucee.runtime.converter.JSONConverter;
import lucee.runtime.converter.JSONDateFormat;
import lucee.runtime.exp.ApplicationException;
import lucee.runtime.listener.JavaSettings;
import lucee.runtime.listener.JavaSettingsImpl;
import lucee.runtime.listener.SerializationSettings;
import lucee.runtime.op.Caster;
import lucee.runtime.type.Struct;
import lucee.runtime.type.StructImpl;
import lucee.runtime.type.util.KeyConstants;

public class PhysicalClassLoaderFactory {

	private static final AtomicLong counter = new AtomicLong(0);
	private static long _start = 0L;
	private static String start = Long.toString(_start, Character.MAX_RADIX);
	private static Object countToken = new Object();

	private static final long IDLE_TIMEOUT_MS = Math.max(60000, Caster.toLongValue(SystemUtil.getSystemPropOrEnvVar("lucee.classloader.idle.timeout", null), 300000L));
	private static final int IDLE_MINSIZE = Caster.toIntValue(SystemUtil.getSystemPropOrEnvVar("lucee.classloader.idle.minsize", null), 75);

	private static Map<String, CachedLoader> classLoaders = new ConcurrentHashMap<>();
	private static RC rc = new RC();

	public static PhysicalClassLoader getPhysicalClassLoader(Config c, Resource directory, boolean reload) throws IOException {
		String key = HashUtil.create64BitHashAsString(directory.getAbsolutePath());

		CachedLoader cached = reload ? null : classLoaders.get(key);
		if (cached == null) {
			synchronized (SystemUtil.createToken("PhysicalClassLoader", key)) {
				cached = reload ? null : classLoaders.get(key);
				if (cached == null) {
					// if we have a reload, clear the existing before set a new one
					if (reload) {
						CachedLoader existing = classLoaders.get(key);
						if (existing != null) PhysicalClassLoader.flush(existing.get(), c, false);
					}
					LogUtil.log(Log.LEVEL_INFO, "physical-classloader",
							"set new PhysicalClassLoader with key [" + key + "], there are now [" + classLoaders.size() + "] PhysicalClassLoaders loaded.");
					PhysicalClassLoader pcl = new PhysicalClassLoader(c, new ArrayList<Resource>(), directory, SystemUtil.getCombinedClassLoader(), null, false);
					classLoaders.put(key, new CachedLoader(pcl));
					return pcl;
				}
			}
		}

		// at this point we know we had an existing one
		PhysicalClassLoader flushed = PhysicalClassLoader.flushIfNecessary(cached.get(), c);
		if (flushed != null) {
			LogUtil.log(Log.LEVEL_INFO, "physical-classloader",
					"set new PhysicalClassLoader with key [" + key + "], there are now [" + classLoaders.size() + "] PhysicalClassLoaders loaded.");
			classLoaders.put(key, new CachedLoader(flushed));
		}
		return cached.get();
	}

	public static PhysicalClassLoader getRPCClassLoader(Config c, JavaSettings js, boolean reload, ClassLoader parent) throws IOException {
		String key = js == null ? "orphan" : ((JavaSettingsImpl) js).id();
		if (parent != null) {
			if (parent instanceof PhysicalClassLoader) key += "_" + ((PhysicalClassLoader) parent).id;
			else key += "_" + parent.hashCode();
		}

		CachedLoader cached = reload ? null : classLoaders.get(key);
		if (cached == null) {
			synchronized (SystemUtil.createToken("PhysicalClassLoader", key)) {
				cached = reload ? null : classLoaders.get(key);
				if (cached == null) {
					// if we have a reload, clear the existing before set a new one
					if (reload) {
						CachedLoader existing = classLoaders.get(key);
						if (existing != null) PhysicalClassLoader.flush(existing.get(), c, false);
					}
					List<Resource> resources;
					if (js == null) {
						resources = new ArrayList<Resource>();
					}
					else {
						resources = toSortedList(((JavaSettingsImpl) js).getAllResources());
					}
					Resource dir = storeResourceMeta(c, key, js, resources);
					LogUtil.log(Log.LEVEL_INFO, "physical-classloader",
							"set new PhysicalClassLoader with key [" + key + "], there are now [" + classLoaders.size() + "] PhysicalClassLoaders loaded.");
					PhysicalClassLoader pcl = new PhysicalClassLoader(c, resources, dir, parent != null ? parent : SystemUtil.getCombinedClassLoader(), null, true);
					classLoaders.put(key, new CachedLoader(pcl));
					return pcl;
				}
			}
		}

		// at this point we know we had an existing one
		PhysicalClassLoader flushed = PhysicalClassLoader.flushIfNecessary(cached.get(), c);
		if (flushed != null) {
			LogUtil.log(Log.LEVEL_INFO, "physical-classloader",
					"set new PhysicalClassLoader with key [" + key + "], there are now [" + classLoaders.size() + "] PhysicalClassLoaders loaded.");
			classLoaders.put(key, new CachedLoader(flushed));
		}
		return cached.get();
	}

	public static PhysicalClassLoader getRPCClassLoader(Config c, BundleClassLoader bcl, boolean reload) throws IOException {
		String key = HashUtil.create64BitHashAsString(bcl + "");

		CachedLoader cached = reload ? null : classLoaders.get(key);
		if (cached == null) {
			synchronized (SystemUtil.createToken("PhysicalClassLoader", key)) {
				cached = reload ? null : classLoaders.get(key);
				if (cached == null) {
					// if we have a reload, clear the existing before set a new one
					if (reload) {
						CachedLoader existing = classLoaders.get(key);
						if (existing != null) PhysicalClassLoader.flush(existing.get(), c, false);
					}
					Resource dir = c.getClassDirectory().getRealResource("RPC/" + key);
					if (!dir.exists()) ResourceUtil.createDirectoryEL(dir, true);
					LogUtil.log(Log.LEVEL_INFO, "physical-classloader",
							"set new PhysicalClassLoader with key [" + key + "], there are now [" + classLoaders.size() + "] PhysicalClassLoaders loaded.");
					PhysicalClassLoader pcl = new PhysicalClassLoader(c, new ArrayList<Resource>(), dir, SystemUtil.getCombinedClassLoader(), bcl, true);
					classLoaders.put(key, new CachedLoader(pcl));
					return pcl;
				}
			}
		}

		// at this point we know we had an existing one
		PhysicalClassLoader flushed = PhysicalClassLoader.flushIfNecessary(cached.get(), c);
		if (flushed != null) {
			LogUtil.log(Log.LEVEL_INFO, "physical-classloader",
					"set new PhysicalClassLoader with key [" + key + "], there are now [" + classLoaders.size() + "] PhysicalClassLoaders loaded.");
			classLoaders.put(key, new CachedLoader(flushed));
		}
		return cached.get();
	}

	/**
	 * Evicts classloaders that have not been accessed within the idle timeout window. Intended to be
	 * called periodically by the Lucee Controller thread.
	 */
	public static void clean(Config config) {
		int sizeBefore = classLoaders.size();
		LogUtil.log(Log.LEVEL_DEBUG, "physical-classloader",
				"clean called, checking " + sizeBefore + " PhysicalClassLoaders for idle timeout (>" + (IDLE_TIMEOUT_MS / 1000) + "s), min size threshold: " + IDLE_MINSIZE);

		if (sizeBefore <= IDLE_MINSIZE) {
			LogUtil.log(Log.LEVEL_DEBUG, "physical-classloader", "clean skipped, size " + sizeBefore + " is within min size threshold " + IDLE_MINSIZE);
			return;
		}

		int evicted = 0;
		for (Map.Entry<String, CachedLoader> entry: classLoaders.entrySet()) {
			CachedLoader cached = entry.getValue();
			if (cached.isIdle()) {
				if (classLoaders.remove(entry.getKey(), cached)) {
					PhysicalClassLoader.flush(cached.loader, config, false);
					evicted++;
					LogUtil.log(Log.LEVEL_INFO, "physical-classloader", "evicted idle PhysicalClassLoader with key [" + entry.getKey() + "], idle for >" + (IDLE_TIMEOUT_MS / 1000)
							+ "s" + ", remaining: " + classLoaders.size());
				}
			}
		}

		if (evicted > 0) {
			System.gc();
		}

		LogUtil.log(Log.LEVEL_DEBUG, "physical-classloader",
				"clean finished, evicted " + evicted + " of " + sizeBefore + " PhysicalClassLoaders, remaining: " + classLoaders.size());
	}

	static String uid() {
		long currentCounter = counter.incrementAndGet(); // Increment and get atomically
		if (currentCounter < 0) {
			synchronized (countToken) {
				currentCounter = counter.incrementAndGet();
				if (currentCounter < 0) {
					counter.set(0L);
					currentCounter = 0L;
					start = Long.toString(++_start, Character.MAX_RADIX);
				}
			}
		}
		if (_start == 0L) return Long.toString(currentCounter, Character.MAX_RADIX);
		return start + "_" + Long.toString(currentCounter, Character.MAX_RADIX);
	}

	static URL[] doURLs(Collection<Resource> reses) throws IOException {
		List<URL> list = new ArrayList<URL>();
		for (Resource r: reses) {
			if ("jar".equalsIgnoreCase(ResourceUtil.getExtension(r, null)) || r.isDirectory()) list.add(doURL(r));
		}
		return list.toArray(new URL[list.size()]);
	}

	static URL doURL(Resource res) throws IOException {
		if (!(res instanceof FileResource)) {
			return ResourceUtil.toFile(res).toURL();
		}
		return ((FileResource) res).toURL();
	}

	static List<Resource> toSortedList(Collection<Resource> resources) {
		List<Resource> list = new ArrayList<Resource>();
		if (resources != null) {
			for (Resource r: resources) {
				if (r != null) list.add(r);
			}
		}
		java.util.Collections.sort(list, rc);
		return list;
	}

	static List<Resource> toSortedList(Resource[] resources) {
		List<Resource> list = new ArrayList<Resource>();
		if (resources != null) {
			for (Resource r: resources) {
				if (r != null) list.add(r);
			}
		}
		java.util.Collections.sort(list, rc);
		return list;
	}

	static Resource storeResourceMeta(Config config, String key, JavaSettings js, Collection<Resource> _resources) throws IOException {
		Resource dir = config.getClassDirectory().getRealResource("RPC/" + key);
		if (!dir.exists()) {
			ResourceUtil.createDirectoryEL(dir, true);
			Resource file = dir.getRealResource("classloader-resources.json");
			Struct root = new StructImpl();
			root.setEL(KeyConstants._resources, _resources);
			JSONConverter json = new JSONConverter(true, CharsetUtil.UTF8, JSONDateFormat.PATTERN_CF, false);
			try {
				String str = json.serialize(null, root, SerializationSettings.SERIALIZE_AS_COLUMN, null);
				IOUtil.write(file, str, CharsetUtil.UTF8, false);
			}
			catch (ConverterException e) {
				throw ExceptionUtil.toIOException(e);
			}

		}
		return dir;
	}

	/**
	 * removes memory based appendix from class name, for example it translates
	 * [test.test_cfc$sub2$cf$5] to [test.test_cfc$sub2$cf]
	 * 
	 * @param name
	 * @return
	 * @throws ApplicationException
	 */
	public static String substractAppendix(String name) throws ApplicationException {
		if (name.endsWith("$cf")) return name;
		int index = name.lastIndexOf('$');
		if (index != -1) {
			name = name.substring(0, index);
		}
		if (name.endsWith("$cf")) return name;
		throw new ApplicationException("could not remove appendix from [" + name + "]");
	}

	private static class RC implements Comparator<Resource> {

		@Override
		public int compare(Resource l, Resource r) {
			return l.getAbsolutePath().compareTo(r.getAbsolutePath());
		}
	}

	private static class CachedLoader {
		final PhysicalClassLoader loader;
		volatile long lastAccess;

		CachedLoader(PhysicalClassLoader loader) {
			this.loader = loader;
			this.lastAccess = System.currentTimeMillis();
		}

		PhysicalClassLoader get() {
			this.lastAccess = System.currentTimeMillis();
			return this.loader;
		}

		boolean isIdle() {
			return (System.currentTimeMillis() - lastAccess) > IDLE_TIMEOUT_MS;
		}
	}

}