package lucee.runtime.config.maven;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.osgi.framework.Version;
import org.xml.sax.SAXException;

import lucee.commons.digest.HashUtil;
import lucee.commons.io.CharsetUtil;
import lucee.commons.io.IOUtil;
import lucee.commons.io.SystemUtil;
import lucee.commons.io.log.Log;
import lucee.commons.io.log.LogUtil;
import lucee.commons.io.res.Resource;
import lucee.commons.lang.ExceptionUtil;
import lucee.commons.lang.StringUtil;
import lucee.commons.net.http.HTTPResponse;
import lucee.commons.net.http.Header;
import lucee.commons.net.http.httpclient.HTTPEngine4Impl;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.converter.ConverterException;
import lucee.runtime.converter.JSONConverter;
import lucee.runtime.converter.JSONDateFormat;
import lucee.runtime.exp.PageException;
import lucee.runtime.listener.SerializationSettings;
import lucee.runtime.op.CastImpl;
import lucee.runtime.op.Caster;
import lucee.runtime.op.date.DateCaster;
import lucee.runtime.osgi.OSGiUtil;
import lucee.runtime.thread.ThreadUtil;
import lucee.runtime.type.util.ListUtil;

public final class MavenUpdateProvider {

	public static final int CONNECTION_TIMEOUT = 10000;

	// new last 90 days
	private static final Repository DEFAULT_REPOSITORY_SONATYPE_LAST90 = new Repository("Sonatype Repositry for Snapshots (last 90 days)",
			"https://central.sonatype.com/repository/maven-snapshots/", Repository.TIMEOUT_15MINUTES, Repository.TIMEOUT_NEVER);

	// old up to version 7.0.0.275-SNAPSHOT
	private static final Repository DEFAULT_REPOSITORY_SONATYPE_LEGACY = new Repository("Old Sonatype Repositry for Snapshots",
			"https://oss.sonatype.org/content/repositories/snapshots/", Repository.TIMEOUT_NEVER, Repository.TIMEOUT_NEVER);

	private static final Repository[] DEFAULT_REPOSITORY_SNAPSHOTS_CORE = new Repository[] { DEFAULT_REPOSITORY_SONATYPE_LAST90, DEFAULT_REPOSITORY_SONATYPE_LEGACY };
	private static final Repository[] DEFAULT_REPOSITORY_SNAPSHOTS_EXTENSIONS = new Repository[] { DEFAULT_REPOSITORY_SONATYPE_LAST90 };

	private static final Repository[] DEFAULT_REPOSITORY_RELEASES = new Repository[] {
			new Repository("Maven Release Repository", "https://repo1.maven.org/maven2/", Repository.TIMEOUT_1HOUR, Repository.TIMEOUT_NEVER) };

	private static final Repository[] DEFAULT_REPOSITORY_MIXED = new Repository[] {
			// versions provided by Lucee
			new Repository("Lucee Maven repository", "https://cdn.lucee.org/", Repository.TIMEOUT_1HOUR, Repository.TIMEOUT_NEVER) };

	public static final String DEFAULT_GROUP = "org.lucee";
	public static final String DEFAULT_ARTIFACT = "lucee";

	private static Repository[] defaultRepositoryReleases;
	private static Repository[] defaultRepositorySnapshotsCore;
	private static Repository[] defaultRepositorySnapshotsExtensions;
	private static Repository[] defaultRepositoryMixed;

	private final String group;
	private final String artifact;
	private final Repository[] repoSnapshots;
	private final Repository[] repoReleases;
	private final Repository[] repoMixed;
	private final List<Repository> repos;

	public static Repository[] getDefaultRepositoryReleases() {
		if (defaultRepositoryReleases == null) {
			defaultRepositoryReleases = readReposFromEnvVar("lucee.mvn.repo.releases", DEFAULT_REPOSITORY_RELEASES);
		}
		return defaultRepositoryReleases;
	}

	public static Repository[] getDefaultRepositorySnapshotsCore() {
		if (defaultRepositorySnapshotsCore == null) {
			defaultRepositorySnapshotsCore = readReposFromEnvVar("lucee.mvn.repo.snapshots", DEFAULT_REPOSITORY_SNAPSHOTS_CORE);
		}
		return defaultRepositorySnapshotsCore;
	}

	public static Repository[] getDefaultRepositorySnapshotsExtension() {
		if (defaultRepositorySnapshotsExtensions == null) {
			defaultRepositorySnapshotsExtensions = readReposFromEnvVar("lucee.mvn.repo.snapshots", DEFAULT_REPOSITORY_SNAPSHOTS_EXTENSIONS);
		}
		return defaultRepositorySnapshotsExtensions;
	}

	public static Repository[] getDefaultRepositoryMixed() {
		if (defaultRepositoryMixed == null) {
			defaultRepositoryMixed = readReposFromEnvVar("lucee.mvn.repo.snapshots", DEFAULT_REPOSITORY_MIXED);
		}
		return defaultRepositoryMixed;
	}

	private static Repository[] readReposFromEnvVar(String envVarName, Repository[] defaultValue) {
		String str = SystemUtil.getSystemPropOrEnvVar(envVarName, null);
		if (!StringUtil.isEmpty(str, true)) {

			List<String> raw = ListUtil.listToList(str.trim(), ',', true);
			List<Repository> repos = new ArrayList<>();
			for (String s: raw) {
				try {
					repos.add(new Repository(null, new URL(s).toExternalForm(), Repository.TIMEOUT_5MINUTES, Repository.TIMEOUT_NEVER));
				}
				catch (Exception e) {
				}
			}
			if (repos.size() > 0) {
				return repos.toArray(new Repository[repos.size()]);
			}

		}

		return defaultValue;
	}

	public MavenUpdateProvider() {
		this.repoSnapshots = getDefaultRepositorySnapshotsCore();
		this.repoReleases = getDefaultRepositoryReleases();
		this.repoMixed = getDefaultRepositoryMixed();
		this.repos = merge(repoSnapshots, repoReleases, repoMixed);
		this.group = DEFAULT_GROUP;
		this.artifact = DEFAULT_ARTIFACT;
	}

	public MavenUpdateProvider(String group, String artifact) {
		this.repoSnapshots = getDefaultRepositorySnapshotsCore();
		this.repoReleases = getDefaultRepositoryReleases();
		this.repoMixed = getDefaultRepositoryMixed();
		this.repos = merge(repoSnapshots, repoReleases, repoMixed);
		this.group = group;
		this.artifact = artifact;
	}

	public MavenUpdateProvider(Repository[] repoSnapshots, Repository[] repoReleases, Repository[] repoMixed, String group, String artifact) {
		this.repoSnapshots = repoSnapshots;
		this.repoReleases = repoReleases;
		this.repoMixed = repoMixed;
		this.repos = merge(repoSnapshots, repoReleases, repoMixed);
		this.group = group;
		this.artifact = artifact;
	}

	static List<Repository> merge(Repository[] left, Repository[] right) {
		List<Repository> list = new ArrayList<>();
		for (Repository repo: left) {
			list.add(repo);
		}
		for (Repository repo: right) {
			list.add(repo);
		}

		return list;
	}

	static List<Repository> merge(Repository[] left, Repository[] middle, Repository[] right) {
		List<Repository> list = new ArrayList<>();
		for (Repository repo: left) {
			list.add(repo);
		}
		for (Repository repo: middle) {
			list.add(repo);
		}
		for (Repository repo: right) {
			list.add(repo);
		}

		return list;
	}

	public List<Version> listOld() throws IOException, GeneralSecurityException, SAXException {
		try {
			MetadataReader mr;

			Set<Version> versions = new HashSet<>();
			for (Repository repo: repos) {
				mr = new MetadataReader(repo, group, artifact);
				for (Version v: mr.read()) {
					versions.add(v);
				}
			}

			if (versions.size() > 0) {
				List<Version> sortedList = new ArrayList<>(versions);
				Collections.sort(sortedList, OSGiUtil::compare);
				return sortedList;

			}

			return new ArrayList<>();
		}
		catch (UnknownHostException uhe) {
			throw new IOException("cannot reach maven server", uhe);
		}
	}

	public List<Version> list() throws IOException, GeneralSecurityException, SAXException, InterruptedException {
		try {
			Set<Version> versions = Collections.synchronizedSet(new HashSet<>());
			List<Thread> threads = new ArrayList<>();
			Stack<Exception> exceptions = new Stack<>();
			for (Repository repo: repos) {
				Thread thread = ThreadUtil.getThread(() -> {
					try {
						MetadataReader mr = new MetadataReader(repo, group, artifact);
						for (Version v: mr.read()) {
							// print.e(repo.label + "(" + repo.url + "):" + v);
							versions.add(v);
						}
					}
					catch (Exception e) {
						exceptions.add(e);
					}
				}, true);
				thread.start();
				threads.add(thread);
			}

			// handle exceptions
			if (exceptions.size() > 0) {
				Exception e = exceptions.pop();
				if (e instanceof GeneralSecurityException) throw (GeneralSecurityException) e;
				else if (e instanceof SAXException) throw (SAXException) e;
				throw ExceptionUtil.toIOException(e);
			}

			// Join all threads
			for (Thread thread: threads) {
				thread.join();
			}

			if (versions.size() > 0) {
				List<Version> sortedList = new ArrayList<>(versions);
				Collections.sort(sortedList, OSGiUtil::compare);
				return sortedList;
			}

			return new ArrayList<>();
		}
		catch (UnknownHostException uhe) {
			throw new IOException("cannot reach maven server", uhe);
		}
	}

	public InputStream getCore(Version version) throws IOException, GeneralSecurityException, SAXException, PageException {

		Map<String, Object> data = detail(version, "jar", true);
		String strURL = Caster.toString(data.get("lco"), null);
		if (!StringUtil.isEmpty(strURL)) {
			// JAR
			HTTPResponse rsp = HTTPEngine4Impl.get(new URL(strURL), null, null, MavenUpdateProvider.CONNECTION_TIMEOUT, true, null, null, null, null);
			if (rsp != null) {
				int sc = rsp.getStatusCode();
				if (sc >= 200 && sc < 300) return rsp.getContentAsStream();
			}

		}
		return getFileStreamFromZipStream(getLoader(version));
	}

	public InputStream getLoader(Version version) throws IOException, GeneralSecurityException, SAXException, PageException {
		Map<String, Object> data = detail(version, "jar", true);
		String strURL = Caster.toString(data.get("jar"), null);
		if (StringUtil.isEmpty(strURL)) throw new IOException("no jar for [" + version + "] found.");

		// JAR
		HTTPResponse rsp = HTTPEngine4Impl.get(new URL(strURL), null, null, MavenUpdateProvider.CONNECTION_TIMEOUT, true, null, null, null, null);
		if (rsp != null) {
			int sc = rsp.getStatusCode();
			if (sc < 200 || sc >= 300) throw new IOException("unable to invoke [" + strURL + "], status code [" + sc + "]");
		}
		else {
			throw new IOException("unable to invoke [" + strURL + "], no response.");
		}

		return rsp.getContentAsStream();
	}

	/*
	 * public static void main(String[] args) throws PageException, IOException,
	 * GeneralSecurityException, SAXException, BundleException {
	 * 
	 * MavenUpdateProvider mup = new MavenUpdateProvider(); Map<String, Object> map =
	 * mup.detail(OSGiUtil.toVersion("6.1.0.719-SNAPSHOT")); print.e(map); }
	 */

	public Map<String, Object> detail(Version version, String requiredArtifactExtension, boolean throwException)
			throws IOException, GeneralSecurityException, SAXException, PageException {
		// SNAPSHOT - snapshot have a more complicated structure, ebcause there can be udaptes/multiple
		// versions

		boolean isSnap = version.getQualifier().endsWith("-SNAPSHOT");
		List<Repository> repos = isSnap ? merge(repoSnapshots, repoMixed) : merge(repoReleases, repoMixed);

		if (requiredArtifactExtension == null) requiredArtifactExtension = "jar";
		else requiredArtifactExtension = requiredArtifactExtension.toLowerCase();
		try {
			// direct access
			{

				String g = group.replace('.', '/');
				String a = artifact.replace('.', '/');
				String v = version.toString();

				// check caches
				{
					Map<String, Object> result;
					for (Repository repo: repos) {
						result = readFromCache(repo, artifact, v);
						if (result != null) {
							return result;
						}
					}
				}

				for (Repository repo: repos) {
					// read from maven-metadata.xml, snapshots mostly use that pattern
					if (isSnap) {
						RepoReader repoReader = new RepoReader(repo.url, group, artifact, version);
						Map<String, Object> result = repoReader.read(requiredArtifactExtension);
						if (result != null) {
							storeToCache(repo, artifact, v, result);
							return result;
						}
					}
					// read main
					{
						URL urlMain = new URL(repo.url + g + "/" + a + "/" + v + "/" + a + "-" + v + "." + requiredArtifactExtension);
						HTTPResponse rsp = HTTPEngine4Impl.head(urlMain, null, null, MavenUpdateProvider.CONNECTION_TIMEOUT, true, null, null, null, null);
						if (validSatusCode(rsp)) {
							Map<String, Object> result = new LinkedHashMap<>();
							Header[] headers = rsp.getAllHeaders();
							for (Header h: headers) {
								if ("Last-Modified".equals(h.getName())) result.put("lastModified", DateCaster.toDateAdvanced(h.getValue(), null));
							}

							result.put(requiredArtifactExtension, urlMain.toExternalForm());

							// optional
							// pom
							{
								URL url = new URL(repo.url + g + "/" + a + "/" + v + "/" + a + "-" + v + ".pom");
								rsp = HTTPEngine4Impl.head(url, null, null, MavenUpdateProvider.CONNECTION_TIMEOUT, true, null, null, null, null);
								if (validSatusCode(rsp)) {
									result.put("pom", url.toExternalForm());
								}
							}
							// lco
							{
								URL url = new URL(repo.url + g + "/" + a + "/" + v + "/" + a + "-" + v + ".lco");
								rsp = HTTPEngine4Impl.head(url, null, null, MavenUpdateProvider.CONNECTION_TIMEOUT, true, null, null, null, null);
								if (validSatusCode(rsp)) {
									result.put("lco", url.toExternalForm());
								}
							}
							storeToCache(repo, artifact, v, result);
							return result;
						}
					}
				}
			}

		}
		catch (UnknownHostException uhe) {
			throw new IOException("cannot reach maven server", uhe);
		}
		if (throwException) throw new IOException("could not find the artifact [" + requiredArtifactExtension + "] for [" + group + ":" + artifact + ":" + version
				+ "] in the following repositories [" + toList(repos) + "]");
		return null;
	}

	private void storeToCache(Repository repository, String artifact, String version, Map<String, Object> detail) {
		try {
			Resource resLastmod = repository.cacheDirectory
					.getRealResource("detail_" + HashUtil.create64BitHashAsString(group + "_" + artifact + "_" + version + "_lastmod", Character.MAX_RADIX));
			Resource resVersions = repository.cacheDirectory
					.getRealResource("detail_" + HashUtil.create64BitHashAsString(group + "_" + artifact + "_" + version + "_versions", Character.MAX_RADIX));
			String content = fromMapToJsonString(detail, true);

			IOUtil.write(resVersions, StringUtil.isEmpty(content, true) ? "" : content.trim(), CharsetUtil.UTF8, false);
			IOUtil.write(resLastmod, Caster.toString(System.currentTimeMillis()), CharsetUtil.UTF8, false);
		}
		catch (Exception e) {
			LogUtil.log("MetadataReader", e);
		}
	}

	private Map<String, Object> readFromCache(Repository repository, String artifact, String version) {
		try {
			Resource resLastmod = repository.cacheDirectory
					.getRealResource("detail_" + HashUtil.create64BitHashAsString(group + "_" + artifact + "_" + version + "_lastmod", Character.MAX_RADIX));

			if (resLastmod.isFile()) {
				long lastmod = repository.timeoutDetail == Repository.TIMEOUT_NEVER ? Repository.TIMEOUT_NEVER
						: Caster.toLongValue(IOUtil.toString(resLastmod, CharsetUtil.UTF8), 0L);

				if (repository.timeoutDetail == Repository.TIMEOUT_NEVER || lastmod + repository.timeoutDetail > System.currentTimeMillis()) {

					Resource resVersions = repository.cacheDirectory
							.getRealResource("detail_" + HashUtil.create64BitHashAsString(group + "_" + artifact + "_" + version + "_versions", Character.MAX_RADIX));
					String content = IOUtil.toString(resVersions, CharsetUtil.UTF8);
					if (content.length() > 0) {
						return new CastImpl().fromJsonStringToMap(content);
					}
					return null;
				}
			}
		}
		catch (Exception e) {
			LogUtil.log(Log.LEVEL_WARN, "MetadataReader", e);
		}
		return null;
	}

	private String toList(Repository[] repos) {
		StringBuilder sb = new StringBuilder();
		for (Repository r: repos) {
			if (sb.length() > 0) sb.append(", ");
			sb.append(r.url);
		}
		return sb.toString();
	}

	private String toList(List<Repository> repos) {
		StringBuilder sb = new StringBuilder();
		for (Repository r: repos) {
			if (sb.length() > 0) sb.append(", ");
			sb.append(r.url);
		}
		return sb.toString();
	}

	private boolean validSatusCode(HTTPResponse rsp) {
		if (rsp == null) return false;
		return rsp.getStatusCode() >= 200 && rsp.getStatusCode() < 300;
	}

	public static InputStream getFileStreamFromZipStream(InputStream zipStream) throws IOException {
		ZipInputStream zis = new ZipInputStream(zipStream);
		ZipEntry entry;
		while ((entry = zis.getNextEntry()) != null) {
			if (entry.getName().equals("core/core.lco")) {
				// Return an InputStream which is limited to the current zip entry's data
				Enumeration<InputStream> singleStreamEnum = Collections.enumeration(Collections.singletonList(zis));
				return new SequenceInputStream(singleStreamEnum);
			}
		}
		throw new FileNotFoundException("core/core.lco not found in zip");
	}

	private static String fromMapToJsonString(Map<String, Object> detail, boolean compact) throws PageException {
		JSONConverter json = new JSONConverter(true, CharsetUtil.UTF8, JSONDateFormat.PATTERN_CF, compact);
		try {
			return json.serialize(null, detail, SerializationSettings.SERIALIZE_AS_COLUMN, null);
		}
		catch (ConverterException e) {
			throw Caster.toPageException(e);
		}
	}

	public final static class Repository implements Cloneable {

		public static final long TIMEOUT_1HOUR = 60 * 60 * 1000;
		public static final long TIMEOUT_NEVER = Long.MAX_VALUE;
		public static final long TIMEOUT_5MINUTES = 60 * 5 * 1000;
		public static final long TIMEOUT_10MINUTES = 60 * 10 * 1000;
		public static final long TIMEOUT_15MINUTES = 60 * 15 * 1000;
		public static final long TIMEOUT_5SECONDS = 5 * 1000;
		public static final long TIMEOUT_ZERO = 0;

		private static Resource cacheRootDirectory;

		public final String label;
		public final String url;
		public final long timeoutList;
		public final long timeoutDetail;
		public final Resource cacheDirectory;

		static {
			try {
				cacheRootDirectory = CFMLEngineFactory.getInstance().getThreadConfig().getConfigDir();
			}
			catch (Exception e) {
				cacheRootDirectory = SystemUtil.getTempDirectory();
			}
		}

		public Repository(String label, String url, long timeoutList, long timeoutDetail) {
			this(label, url, timeoutList, timeoutDetail, getCacheDirectory(url));
		}

		public Repository(String label, String url, long timeoutList, long timeoutDetail, Resource cacheDirectory) {
			if (!url.endsWith("/")) url += "/";
			this.label = label;
			this.url = url;
			this.timeoutList = timeoutList;
			this.timeoutDetail = timeoutDetail;
			this.cacheDirectory = cacheDirectory;
		}

		private static Resource getCacheDirectory(String url) {
			Resource cacheDirectory = cacheRootDirectory.getRealResource("mvn/cache/" + HashUtil.create64BitHashAsString(url, Character.MAX_RADIX) + "/");
			cacheDirectory.mkdirs();
			return cacheDirectory;
		}

		@Override
		public Object clone() {
			return duplicate();
		}

		public Repository duplicate() {
			return new Repository(label, url, timeoutList, timeoutDetail, cacheDirectory);
		}

		@Override
		public String toString() {
			return "label:" + label + ";url:" + url + ";timeoutList:" + timeoutList + ";timeoutDetail:" + timeoutDetail;
		}
	}
}
