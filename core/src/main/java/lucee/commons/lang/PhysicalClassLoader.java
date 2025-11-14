/**
 * Copyright (c) 2014, the Railo Company Ltd.
 * Copyright (c) 2015, Lucee Association Switzerland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either 
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package lucee.commons.lang;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.UnmodifiableClassException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lucee.commons.io.IOUtil;
import lucee.commons.io.SystemUtil;
import lucee.commons.io.log.LogUtil;
import lucee.commons.io.res.Resource;
import lucee.commons.lang.ClassUtil.ClassLoading;
import lucee.runtime.PageSourcePool;
import lucee.runtime.config.Config;
import lucee.runtime.config.ConfigPro;
import lucee.runtime.osgi.OSGiUtil;
import lucee.transformer.bytecode.util.ASMUtil;
import lucee.transformer.bytecode.util.ClassRenamer;

/**
 * Directory ClassLoader
 */
public final class PhysicalClassLoader extends URLClassLoader implements ExtendableClassLoader, ClassLoaderDefault, ClassLoading, DirectoryProvider {

	static {
		boolean res = registerAsParallelCapable();
	}

	private final Resource directory;
	private ConfigPro config;
	private final ClassLoader addionalClassLoader;
	private final Collection<Resource> resources;

	private Map<String, byte[]> loadedClasses = new ConcurrentHashMap<String, byte[]>();
	private Map<String, byte[]> allLoadedClasses = new ConcurrentHashMap<String, byte[]>(); // this includes all renames
	private Map<String, String> unavaiClasses = new ConcurrentHashMap<String, String>();

	private PageSourcePool pageSourcePool;

	private boolean rpc;

	private String birthplace;

	public final String id;

	PhysicalClassLoader(String key, Config c, List<Resource> resources, Resource directory, ClassLoader parentClassLoader, ClassLoader addionalClassLoader,
			PageSourcePool pageSourcePool, boolean rpc) throws IOException {
		super(PhysicalClassLoaderFactory.doURLs(resources), parentClassLoader == null ? (parentClassLoader = SystemUtil.getCoreClassLoader()) : parentClassLoader);
		this.resources = resources;

		config = (ConfigPro) c;
		this.addionalClassLoader = addionalClassLoader;
		this.birthplace = ExceptionUtil.getStacktrace(new Throwable(), false);
		this.pageSourcePool = pageSourcePool;

		// check directory
		if (!directory.exists()) directory.mkdirs();
		if (!directory.isDirectory()) throw new IOException("Resource [" + directory + "] is not a directory");
		if (!directory.canRead()) throw new IOException("Access denied to [" + directory + "] directory");
		this.directory = directory;
		this.rpc = rpc;
		id = key;
	}

	public String getBirthplace() {
		return birthplace;
	}

	public boolean isRPC() {
		return rpc;
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		return loadClass(name, false, true);
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		return loadClass(name, resolve, true);
	}

	public Boolean isClassAvailable(ClassLoader loader, String className) {
		if (allLoadedClasses.containsKey(className)) return true;
		if (unavaiClasses.containsKey(className)) return false;

		return null;
		// String resourcePath = className.replace('.', '/').concat(".class");
		// return loader.getResource(resourcePath) != null;
	}

	@Override
	public Class<?> loadClass(String name, boolean resolve, Class<?> defaultValue) {
		try {
			return loadClass(name, resolve, true);
		}
		catch (ClassNotFoundException e) {
			return defaultValue;
		}
	}

	@Override
	public Class<?> loadClass(String className, Class defaultValue) {
		try {
			return loadClass(className, false, true);
		}
		catch (ClassNotFoundException e) {
			return defaultValue;
		}
	}

	@Override
	public Class<?> loadClass(String className, Class defaultValue, Set<Throwable> exceptions) {
		try {
			return loadClass(className);
		}
		catch (Exception e) {
			if (exceptions != null) {
				exceptions.add(e);
			}
			return defaultValue;
		}
	}

	private Class<?> loadClass(String name, boolean resolve, boolean loadFromFS) throws ClassNotFoundException {
		// First, check if the class has already been loaded
		Class<?> c = findLoadedClass(name);
		if (c != null) {
			if (resolve) resolveClass(c);
			return c;
		}

		synchronized (getClassLoadingLock(name)) {
			// Check again after acquiring lock
			c = findLoadedClass(name);
			if (c != null) {
				if (resolve) resolveClass(c);
				return c;
			}

			// Check additional classloader first (if any)
			if (addionalClassLoader != null) {
				try {
					c = addionalClassLoader.loadClass(name);
					if (resolve) resolveClass(c);
					return c;
				}
				catch (ClassNotFoundException e) {
					// Continue to next strategy
				}
			}

			boolean isBootDelegated = OSGiUtil.isClassInBootelegation(name);

			// For classes in boot delegation list, always delegate to parent first
			if (isBootDelegated) {
				ClassLoader parent = getParent();
				if (parent != null) {
					try {
						c = parent.loadClass(name);
						if (resolve) resolveClass(c);
						return c;
					}
					catch (ClassNotFoundException e) {
						// Fall through to resources
					}
				}
			}

			// Try resources (Maven libraries override core, but respect boot delegation)
			try {
				c = super.findClass(name);
				if (resolve) resolveClass(c);
				return c;
			}
			catch (ClassNotFoundException e1) {
				// Resources didn't have it, try parent if not already tried
				if (!isBootDelegated) {
					ClassLoader parent = getParent();
					if (parent != null) {
						try {
							c = parent.loadClass(name);
							if (resolve) resolveClass(c);
							return c;
						}
						catch (ClassNotFoundException e2) {
							// Continue to filesystem
						}
					}
				}

				// Finally try filesystem directory
				if (loadFromFS) {
					synchronized (getClassLoadingLock(name)) {
						Resource res = directory.getRealResource(name.replace('.', '/').concat(".class"));
						if (res.isFile()) {
							c = _loadClass(name, read(name), false);
							if (resolve) resolveClass(c);
							return c;
						}
					}
				}

				throw new ClassNotFoundException(name);
			}
		}
	}

	@Override
	public Class<?> loadClass(String name, byte[] barr) throws UnmodifiableClassException {
		synchronized (getClassLoadingLock(name)) {
			Class<?> clazz = findLoadedClass(name);
			if (clazz == null) return _loadClass(name, barr, false);
			return rename(clazz, barr);
		}
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		if (addionalClassLoader != null) {
			// boolean true in case it returns TRUE or null
			if (!Boolean.FALSE.equals(isClassAvailable(addionalClassLoader, name))) {
				try {
					return addionalClassLoader.loadClass(name);
				}
				catch (ClassNotFoundException e) {
					LogUtil.trace("physical-classloader", e);
				}
			}
		}

		if (super.findResource(name.replace('.', '/').concat(".class")) != null) {
			return super.findClass(name);
		}

		synchronized (getClassLoadingLock(name)) {
			Resource res = directory.getRealResource(name.replace('.', '/').concat(".class"));
			if (!res.isFile()) {
				// if (cnfe != null) throw cnfe;
				throw new ClassNotFoundException("Class [" + name + "] is invalid or doesn't exist");
			}
			return _loadClass(name, read(name), false);
		}
	}

	private byte[] read(String name) throws ClassNotFoundException {
		Resource res = directory.getRealResource(name.replace('.', '/').concat(".class"));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			IOUtil.copy(res, baos, false);
		}
		catch (IOException e) {
			this.unavaiClasses.put(name, "");
			throw new ClassNotFoundException("Class [" + name + "] is invalid or doesn't exist", e);
		}
		finally {
			IOUtil.closeEL(baos);
		}
		return baos.toByteArray();
	}

	private byte[] read(String name, byte[] defaultValue) {
		Resource res = directory.getRealResource(name.replace('.', '/').concat(".class"));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			IOUtil.copy(res, baos, false);
		}
		catch (IOException e) {
			this.unavaiClasses.put(name, "");
			return defaultValue;
		}
		finally {
			IOUtil.closeEL(baos);
		}
		return baos.toByteArray();
	}

	private Class<?> rename(Class<?> clazz, byte[] barr) {
		String newName = clazz.getName() + "$" + PhysicalClassLoaderFactory.uid();
		return _loadClass(newName, ClassRenamer.rename(barr, newName), true);
	}

	private Class<?> _loadClass(String name, byte[] barr, boolean rename) {
		try {
			Class<?> clazz = defineClass(name, barr, 0, barr.length);

			if (clazz != null) {
				if (!rename) loadedClasses.put(name, barr);
				allLoadedClasses.put(name, barr);

				resolveClass(clazz);
			}
			return clazz;
		}
		catch (ClassFormatError cfe) {
			if (!ASMUtil.isValidBytecode(barr)) throw new RuntimeException("given bytcode for [" + name + "] is not valid");
			throw cfe;
		}
	}

	public Resource[] getJarResources() {
		return resources.toArray(new Resource[resources.size()]);
	}

	public boolean hasJarResources() {
		return resources.isEmpty();
	}

	public int getSize(boolean includeAllRenames) {
		return includeAllRenames ? allLoadedClasses.size() : loadedClasses.size();
	}

	/*
	 * @Override public URL getResource(String name) { URL r = super.getResource(name); if (r != null)
	 * return r; print.e("xx ====>" + name);
	 * 
	 * Resource f = _getResource(name);
	 * 
	 * if (f != null) { return ResourceUtil.toURL(f, null); } return null; }
	 */

	@Override
	public InputStream getResourceAsStream(String name) {
		InputStream is = super.getResourceAsStream(name);
		if (is != null) return is;

		if (name.endsWith(".class")) {
			// MUST store the barr in a less memory intensive way
			String className = name.substring(0, name.length() - 6).replace('/', '.').replace('\\', '.');
			byte[] barr = allLoadedClasses.get(className);
			if (barr != null) return new ByteArrayInputStream(barr);
		}

		URL url = super.getResource(name);
		if (url != null) {
			try {
				return IOUtil.toBufferedInputStream(url.openStream());
			}
			catch (IOException e) {
				LogUtil.trace("physical-classloader", e);
			}
		}

		Resource f = _getResource(name);
		if (f != null) {
			try {
				return IOUtil.toBufferedInputStream(f.getInputStream());
			}
			catch (IOException e) {
				LogUtil.trace("physical-classloader", e);
			}
		}
		return null;
	}

	/**
	 * returns matching File Object or null if file not exust
	 * 
	 * @param name
	 * @return matching file
	 */
	public Resource _getResource(String name) {
		Resource f = directory.getRealResource(name);
		if (f != null && f.isFile()) return f;
		return null;
	}

	public boolean hasClass(String className) {
		return hasResource(className.replace('.', '/').concat(".class"));
	}

	public boolean isClassLoaded(String className) {
		return findLoadedClass(className) != null;
	}

	public boolean hasResource(String name) {
		return _getResource(name) != null;
	}

	/**
	 * @return the directory
	 */
	@Override
	public Resource getDirectory() {
		return directory;
	}

	public void clear() {
		clear(true);
	}

	public void clear(boolean clearPagePool) {
		if (clearPagePool && pageSourcePool != null) pageSourcePool.clearPages(this);
		this.loadedClasses.clear();
		this.allLoadedClasses.clear();
		this.unavaiClasses.clear();
	}

	@Override
	public void finalize() throws Throwable {
		try {
			clear();
		}
		catch (Exception e) {
			LogUtil.log(config, "classloader", e);
		}
		super.finalize();
	}

}
