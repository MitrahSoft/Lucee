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
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import lucee.commons.digest.HashUtil;
import lucee.commons.io.IOUtil;
import lucee.commons.io.SystemUtil;
import lucee.commons.io.log.LogUtil;
import lucee.commons.io.res.Resource;
import lucee.runtime.PageSourcePool;
import lucee.runtime.config.Config;
import lucee.runtime.config.ConfigPro;
import lucee.transformer.bytecode.util.ASMUtil;
import lucee.transformer.bytecode.util.ClassRenamer;

/**
 * Directory ClassLoader
 */
public class PhysicalClassLoader extends URLClassLoader implements ExtendableClassLoader, ClassLoaderDefault {

	static {
		boolean res = registerAsParallelCapable();
	}

	private final Resource directory;
	private final ConfigPro config;
	private final ClassLoader parentClassLoader;
	private final ClassLoader addionalClassLoader;
	private final Collection<Resource> resources;

	private PageSourcePool pageSourcePool;

	private boolean rpc;

	private String birthplace;

	public final String id;
	private Core core;

	PhysicalClassLoader(Config c, List<Resource> resources, Resource directory, ClassLoader parentClassLoader, ClassLoader addionalClassLoader, PageSourcePool pageSourcePool,
			boolean rpc) throws IOException {
		super(new URL[0]); // this classloader is just a shell
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

		StringBuilder sb = new StringBuilder();
		sb.append(directory);
		if (resources != null) {
			for (Resource r: resources) {
				sb.append(';').append(r);
			}
		}
		id = HashUtil.create64BitHashAsString(sb.toString());
		this.parentClassLoader = parentClassLoader == null ? (parentClassLoader = SystemUtil.getCombinedClassLoader()) : parentClassLoader;
		this.core = new Core();
	}

	public String getBirthplace() {
		return birthplace;
	}

	public boolean isRPC() {
		return rpc;
	}

	@Override
	public void close() throws IOException {
		core.close();
	}

	@Override
	protected void addURL(URL url) {
		core.addURL(url);
	}

	@Override
	public URL[] getURLs() {
		return core.getURLs();
	}

	@Override
	protected Package definePackage(String name, Manifest man, URL url) {
		return core.definePackage(name, man, url);
	}

	@Override
	public URL findResource(String name) {
		return core.findResource(name);
	}

	@Override
	public Enumeration<URL> findResources(String name) throws IOException {
		return core.findResources(name);
	}

	@Override
	protected PermissionCollection getPermissions(CodeSource codesource) {
		return core.getPermissions(codesource);
	}

	@Override
	protected Object getClassLoadingLock(String className) {
		return core.getClassLoadingLock(className);
	}

	@Override
	protected Class<?> findClass(String moduleName, String name) {
		return core.findClass(moduleName, name);
	}

	@Override
	protected URL findResource(String moduleName, String name) throws IOException {
		return core.findResource(moduleName, name);
	}

	@Override
	public URL getResource(String name) {
		return core.getResource(name);
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		return core.getResources(name);
	}

	@Override
	public Stream<URL> resources(String name) {
		return core.resources(name);
	}

	@Override
	protected Package definePackage(String name, String specTitle, String specVersion, String specVendor, String implTitle, String implVersion, String implVendor, URL sealBase) {
		return core.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
	}

	@Override
	protected Package getPackage(String name) {
		return core.getPackage(name);
	}

	@Override
	protected Package[] getPackages() {
		return core.getPackages();
	}

	@Override
	protected String findLibrary(String libname) {
		return core.findLibrary(libname);
	}

	@Override
	public void setDefaultAssertionStatus(boolean enabled) {
		core.setDefaultAssertionStatus(enabled);
	}

	@Override
	public void setPackageAssertionStatus(String packageName, boolean enabled) {
		core.setPackageAssertionStatus(packageName, enabled);
	}

	@Override
	public void setClassAssertionStatus(String className, boolean enabled) {
		core.setClassAssertionStatus(className, enabled);
	}

	@Override
	public void clearAssertionStatus() {
		core.clearAssertionStatus();
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		return core.loadClass(name, false, true);
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		return core.loadClass(name, resolve, true);
	}

	public Boolean isClassAvailable(ClassLoader loader, String className) {
		return core.isClassAvailable(loader, className);
	}

	@Override
	public Class<?> loadClass(String name, boolean resolve, Class<?> defaultValue) {
		return core.loadClass(name, resolve, defaultValue);
	}

	@Override
	public Class<?> loadClass(String name, byte[] barr) throws UnmodifiableClassException {
		return core.loadClass(name, barr);
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		return core.findClass(name);
	}

	public Resource[] getJarResources() {
		return resources.toArray(new Resource[resources.size()]);
	}

	public boolean hasJarResources() {
		return resources.isEmpty();
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		return core.getResourceAsStream(name);
	}

	public boolean hasResource(String name) {
		return _getResource(name) != null;
	}

	private Resource _getResource(String name) {
		Resource f = directory.getRealResource(name);
		if (f != null && f.isFile()) return f;
		return null;
	}

	public Resource getDirectory() {
		return directory;
	}

	public boolean hasClass(String className) {
		return core.hasClass(className);
	}

	public boolean isClassLoaded(String className) {
		return core.isClassLoaded(className);
	}

	public void clear() {
		clear(true);
	}

	public void clear(boolean clearPagePool) {
		if (clearPagePool && pageSourcePool != null) pageSourcePool.clearPages(this);
		core.clear();
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

	private class Core extends URLClassLoader implements ExtendableClassLoader, ClassLoaderDefault {

		private Map<String, byte[]> loadedClasses = new ConcurrentHashMap<String, byte[]>();
		private Map<String, byte[]> allLoadedClasses = new ConcurrentHashMap<String, byte[]>(); // this includes all renames
		private Map<String, String> unavaiClasses = new ConcurrentHashMap<String, String>();

		private Core() throws IOException {
			super(PhysicalClassLoaderFactory.doURLs(resources), parentClassLoader);

		}

		@Override
		protected void addURL(URL url) {
			super.addURL(url);
		}

		@Override
		protected PermissionCollection getPermissions(CodeSource codesource) {
			return super.getPermissions(codesource);
		}

		@Override
		protected Package definePackage(String name, Manifest man, URL url) {
			return super.definePackage(name, man, url);
		}

		@Override
		protected Object getClassLoadingLock(String className) {
			return super.getClassLoadingLock(className);
		}

		@Override
		protected Class<?> findClass(String moduleName, String name) {
			return super.findClass(moduleName, name);
		}

		@Override
		protected URL findResource(String moduleName, String name) throws IOException {
			return super.findResource(moduleName, name);
		}

		@Override
		protected Package definePackage(String name, String specTitle, String specVersion, String specVendor, String implTitle, String implVersion, String implVendor,
				URL sealBase) {
			return super.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
		}

		@Override
		protected Package getPackage(String name) {
			return super.getPackage(name);
		}

		@Override
		protected Package[] getPackages() {
			return super.getPackages();
		}

		@Override
		protected String findLibrary(String libname) {
			return super.findLibrary(libname);
		}

		public Boolean isClassAvailable(ClassLoader loader, String className) {
			if (allLoadedClasses.containsKey(className)) return true;
			if (unavaiClasses.containsKey(className)) return false;

			return null;
		}

		public int getSize(boolean includeAllRenames) {
			return includeAllRenames ? allLoadedClasses.size() : loadedClasses.size();
		}

		public boolean hasClass(String className) {
			return hasResource(className.replace('.', '/').concat(".class"));
		}

		public boolean isClassLoaded(String className) {
			return findLoadedClass(className) != null;
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			return loadClass(name, false, true);
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			return loadClass(name, resolve, true);
		}

		@Override
		public Class<?> loadClass(String name, boolean resolve, Class<?> defaultValue) {
			return loadClass(name, resolve, true, defaultValue);
		}

		@Override
		public Class<?> loadClass(String name, byte[] barr) throws UnmodifiableClassException {
			synchronized (getClassLoadingLock(name)) {
				Class<?> clazz = findLoadedClass(name);
				if (clazz == null) return _loadClass(name, barr, false);
				return rename(clazz, barr);
			}
		}

		private Class<?> loadClass(String name, boolean resolve, boolean loadFromFS, Class<?> defaultValue) {
			// First, check if the class has already been loaded
			Class<?> c = findLoadedClass(name);

			if (c == null) {
				synchronized (getClassLoadingLock(name)) {
					c = findLoadedClass(name);
					if (c == null) {
						ClassLoader pcl = getParent();
						if (pcl instanceof ClassLoaderDefault) {
							c = ((ClassLoaderDefault) pcl).loadClass(name, resolve, null);
						}
						else {
							try {
								c = super.loadClass(name, resolve);
							}
							catch (Exception e) {
							}
						}

						if (c == null && addionalClassLoader != null) {
							try {
								c = addionalClassLoader.loadClass(name);
							}
							catch (Exception e) {
							}
						}

						if (c == null) {
							if (loadFromFS) {
								try {
									c = findClass(name);
								}
								catch (ClassNotFoundException e) {
									return defaultValue;
								}
							}
							else return defaultValue;
						}
					}
				}
			}
			if (resolve) resolveClass(c);
			return c;
		}

		private Class<?> loadClass(String name, boolean resolve, boolean loadFromFS) throws ClassNotFoundException {
			Class<?> c = loadClass(name, resolve, loadFromFS, null);
			if (c == null) {
				throw new ClassNotFoundException(name);
			}
			return c;
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			/*
			 * ClassNotFoundException cnfe = null; try { return super.findClass(name); } catch
			 * (ClassNotFoundException e) { cnfe = e; }
			 */

			if (super.findResource(name.replace('.', '/').concat(".class")) != null) {
				return super.findClass(name);
			}

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

			synchronized (getClassLoadingLock(name)) {
				Resource res = directory.getRealResource(name.replace('.', '/').concat(".class"));
				if (!res.isFile()) {
					// if (cnfe != null) throw cnfe;
					throw new ClassNotFoundException("Class [" + name + "] is invalid or doesn't exist");
				}

				return _loadClass(name, read(name), false);
			}
		}

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

		private void clear() {
			this.loadedClasses.clear();
			this.allLoadedClasses.clear();
			this.unavaiClasses.clear();
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
	}
}
