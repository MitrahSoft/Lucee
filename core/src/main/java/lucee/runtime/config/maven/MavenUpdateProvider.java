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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.osgi.framework.Version;
import org.xml.sax.SAXException;

import lucee.commons.digest.HashUtil;
import lucee.commons.io.SystemUtil;
import lucee.commons.io.res.Resource;
import lucee.commons.lang.StringUtil;
import lucee.commons.net.http.HTTPResponse;
import lucee.commons.net.http.Header;
import lucee.commons.net.http.httpclient.HTTPEngine4Impl;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.exp.PageException;
import lucee.runtime.op.Caster;
import lucee.runtime.op.date.DateCaster;
import lucee.runtime.osgi.OSGiUtil;
import lucee.runtime.type.util.ListUtil;

public class MavenUpdateProvider {

	public static final int CONNECTION_TIMEOUT = 10000;

	private static final Repository[] DEFAULT_REPOSITORY_SNAPSHOTS = new Repository[] {
			// new last 90 days
			new Repository("Sonatype Repositry for Snapshots (last 90 days)", "https://central.sonatype.com/repository/maven-snapshots/", Repository.TIMEOUT_5MINUTES)
			// versions provided by Lucee
			, new Repository("Lucee Maven repository", "https://cdn.lucee.org/", Repository.TIMEOUT_1HOUR)

	};

	private static final Repository[] DEFAULT_REPOSITORY_RELEASES = new Repository[] {
			new Repository("Maven Release Repository", "https://repo1.maven.org/maven2/", Repository.TIMEOUT_1HOUR) };

	public static final String DEFAULT_GROUP = "org.lucee";
	public static final String DEFAULT_ARTIFACT = "lucee";

	private static Repository[] defaultRepositoryReleases;
	private static Repository[] defaultRepositorySnapshots;

	private String group;
	private String artifact;
	private Repository[] repoSnapshots;
	private Repository[] repoReleases;
	private List<Repository> repos;

	public static Repository[] getDefaultRepositoryReleases() {
		if (defaultRepositoryReleases == null) {
			defaultRepositoryReleases = readReposFromEnvVar("lucee.mvn.repo.releases", DEFAULT_REPOSITORY_RELEASES);
		}
		return defaultRepositoryReleases;
	}

	public static Repository[] getDefaultRepositorySnapshots() {
		if (defaultRepositorySnapshots == null) {
			defaultRepositorySnapshots = readReposFromEnvVar("lucee.mvn.repo.snapshots", DEFAULT_REPOSITORY_SNAPSHOTS);
		}
		return defaultRepositorySnapshots;
	}

	private static Repository[] readReposFromEnvVar(String envVarName, Repository[] defaultValue) {
		String str = SystemUtil.getSystemPropOrEnvVar(envVarName, null);
		if (!StringUtil.isEmpty(str, true)) {

			List<String> raw = ListUtil.listToList(str.trim(), ',', true);
			List<Repository> repos = new ArrayList<>();
			for (String s: raw) {
				try {
					repos.add(new Repository(null, new URL(s).toExternalForm(), Repository.TIMEOUT_5MINUTES));
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
		this.repoSnapshots = getDefaultRepositorySnapshots();
		this.repoReleases = getDefaultRepositoryReleases();
		this.repos = merge(repoSnapshots, repoReleases);
		this.group = DEFAULT_GROUP;
		this.artifact = DEFAULT_ARTIFACT;
	}

	private List<Repository> merge(Repository[] left, Repository[] right) {
		List<Repository> list = new ArrayList<>();
		for (Repository repo: left) {
			list.add(repo);
		}
		for (Repository repo: right) {
			list.add(repo);
		}

		return list;
	}

	public MavenUpdateProvider(Repository[] repoSnapshots, Repository[] repoReleases, String group, String artifact) {
		this.repoSnapshots = repoSnapshots;
		this.repoReleases = repoReleases;
		this.group = group;
		this.artifact = artifact;
	}

	public List<Version> list() throws IOException, GeneralSecurityException, SAXException {
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

	public InputStream getCore(Version version) throws IOException, GeneralSecurityException, SAXException, PageException {

		Map<String, Object> data = detail(version);
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
		Map<String, Object> data = detail(version);
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

	public Map<String, Object> detail(Version version) throws IOException, GeneralSecurityException, SAXException, PageException {
		// SNAPSHOT - snapshot have a more complicated structure, ebcause there can be udaptes/multiple
		// versions
		boolean isSnap = version.getQualifier().endsWith("-SNAPSHOT");
		Repository[] repos = isSnap ? repoSnapshots : repoReleases;

		try {
			// direct access
			{

				String g = group.replace('.', '/');
				String a = artifact.replace('.', '/');
				String v = version.toString();

				for (Repository repo: repos) {

					// read from maven-metadata.xml, snapshots mostly use that pattern
					if (isSnap) {
						RepoReader repoReader = new RepoReader(repo.url, group, artifact, version);
						Map<String, Object> result = repoReader.read();
						if (result != null) {
							return result;
						}
					}

					// read jar
					{
						URL urlJar = new URL(repo.url + g + "/" + a + "/" + v + "/" + a + "-" + v + ".jar");
						HTTPResponse rsp = HTTPEngine4Impl.head(urlJar, null, null, MavenUpdateProvider.CONNECTION_TIMEOUT, true, null, null, null, null);

						if (validSatusCode(rsp)) {
							Map<String, Object> result = new LinkedHashMap<>();
							Header[] headers = rsp.getAllHeaders();
							for (Header h: headers) {
								if ("Last-Modified".equals(h.getName())) result.put("lastModified", DateCaster.toDateAdvanced(h.getValue(), null));
							}

							result.put("jar", urlJar.toExternalForm());

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

							return result;
						}
					}
				}
			}

		}
		catch (UnknownHostException uhe) {
			throw new IOException("cannot reach maven server", uhe);
		}
		throw new IOException("could not find detail info for version [" + version + "] in the following repositories [" + toList(repos) + "]");
	}

	private String toList(Repository[] repos) {
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

	public final static class Repository {

		public static final long TIMEOUT_1HOUR = 60 * 60 * 1000;
		public static final long TIMEOUT_NEVER = Long.MAX_VALUE;
		public static final long TIMEOUT_5MINUTES = 60 * 5 * 1000;

		private static Resource cacheRootDirectory;

		public final String label;
		public final String url;
		public final long timeout;
		public final Resource cacheDirectory;

		static {
			try {
				cacheRootDirectory = CFMLEngineFactory.getInstance().getThreadConfig().getConfigDir();
			}
			catch (Exception e) {
				cacheRootDirectory = SystemUtil.getTempDirectory();
			}
		}

		public Repository(String label, String url, long timeout) {
			if (!url.endsWith("/")) url += "/";
			this.label = label;
			this.url = url;
			this.timeout = timeout;

			cacheDirectory = cacheRootDirectory.getRealResource("mvn/cache/" + HashUtil.create64BitHashAsString(url, Character.MAX_RADIX) + "/");
			cacheDirectory.mkdirs();
		}
	}

}
