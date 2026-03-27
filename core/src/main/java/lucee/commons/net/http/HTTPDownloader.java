package lucee.commons.net.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.GeneralSecurityException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;

import lucee.commons.io.IOUtil;
import lucee.commons.io.SystemUtil;
import lucee.commons.io.log.Log;
import lucee.commons.io.log.LogUtil;
import lucee.commons.io.res.Resource;
import lucee.commons.io.res.util.ResourceUtil;
import lucee.commons.lang.Pair;
import lucee.commons.lang.StringUtil;
import lucee.commons.net.http.httpclient.HTTPEngine4Impl;
import lucee.runtime.config.Config;
import lucee.runtime.engine.ThreadLocalPageContext;
import lucee.runtime.net.proxy.ProxyData;
import lucee.runtime.net.proxy.ProxyDataImpl;

/**
 * Unified HTTP downloader utility that wraps HTTPEngine4Impl. Provides consistent error handling,
 * logging, and connection pooling for all internal downloads.
 * 
 * @see HTTPEngine4Impl
 * @see <a href="https://luceeserver.atlassian.net/browse/LDEV-5122">LDEV-5122</a>
 */
public final class HTTPDownloader {

	public static final long DEFAULT_CONNECT_TIMEOUT = 10000; // 10 seconds
	public static final long DEFAULT_READ_TIMEOUT = 60000; // 60 seconds
	private static final String DEFAULT_USER_AGENT = "Lucee";

	private HTTPDownloader() {
		// Utility class, prevent instantiation
	}

	// private static volatile CloseableHttpClient SHARED_CLIENT;
	private static volatile CloseableHttpClient SHARED_CLIENT_POOLING;
	private static HttpClientConnectionManager SHARED_CLIENT_POOLING_CM;
	private static final Object CLIENT_LOCK = new Object();

	public static void releaseSharedClient() {
		synchronized (CLIENT_LOCK) {
			if (SHARED_CLIENT_POOLING != null) {
				IOUtil.closeEL(SHARED_CLIENT_POOLING);
				SHARED_CLIENT_POOLING = null;
			}
		}
	}

	public static CloseableHttpClient getHttpClient(boolean pooling) throws GeneralSecurityException, IOException {
		if (pooling) {
			if (SHARED_CLIENT_POOLING == null) {
				synchronized (CLIENT_LOCK) {
					if (SHARED_CLIENT_POOLING == null) {
						Pair<HttpClientBuilder, HttpClientConnectionManager> pair = HTTPEngine4Impl.getHttpClientBuilder(true, null, null, "true");
						SHARED_CLIENT_POOLING_CM = pair.getValue();
						SHARED_CLIENT_POOLING = pair.getName().build();
					}
				}
			}

			// Stats are useful for debugging pool exhaustion, but noisy for Info level
			if (SHARED_CLIENT_POOLING_CM instanceof PoolingHttpClientConnectionManager) {
				PoolingHttpClientConnectionManager cm = (PoolingHttpClientConnectionManager) SHARED_CLIENT_POOLING_CM;
				org.apache.http.pool.PoolStats stats = cm.getTotalStats();

				LogUtil.log(Log.LEVEL_DEBUG, "http", "Pool Stats -> " + "Max: " + stats.getMax() + ", Leased: " + stats.getLeased() + ", Pending: " + stats.getPending()
						+ ", Available: " + stats.getAvailable());

			}
			return SHARED_CLIENT_POOLING;
		}

		// Always return a fresh, unshared client for non-pooling requests to ensure thread safety
		HttpClientBuilder builder = HTTPEngine4Impl.getHttpClientBuilder(false, null, null, "true").getName();
		return builder.build();
	}

	/**
	 * Build RequestConfig with separate connection and socket timeouts (following Http.java pattern)
	 */
	private static RequestConfig buildRequestConfig(long connectTimeout, long socketTimeout) {
		return RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).setConnectionRequestTimeout(2000).setConnectTimeout((int) connectTimeout)
				.setSocketTimeout((int) socketTimeout).build();
	}

	/**
	 * Get ProxyData from ThreadLocalPageContext Config (following Http.java pattern line 1099)
	 */
	private static ProxyData getProxyData(String host) {
		Config config = ThreadLocalPageContext.getConfig();
		if (config != null) {
			ProxyData proxy = config.getProxyData();
			return ProxyDataImpl.validate(proxy, host);
		}
		return null;
	}

	/**
	 * Simple container for client and context
	 */
	private static class ClientContext {
		final CloseableHttpClient client;
		final HttpContext context;

		ClientContext(CloseableHttpClient client, HttpContext context) {
			this.client = client;
			this.context = context;
		}
	}

	/**
	 * Setup HttpClientBuilder with proxy, credentials, and request config. Shared by get(), head(), and
	 * exists() methods to avoid code duplication.
	 */
	private static ClientContext buildHttpClient(URL url, String username, String password, long connectTimeout, long readTimeout, HttpClientBuilder builder,
			org.apache.http.client.methods.HttpUriRequest request) {

		// Get proxy from Config (already validated in getProxyData)
		ProxyData proxy = getProxyData(url.getHost());

		// Build RequestConfig with separate connection and socket timeouts
		RequestConfig requestConfig = buildRequestConfig(connectTimeout, readTimeout);
		builder.setDefaultRequestConfig(requestConfig);

		// Set credentials if provided
		HttpHost httpHost = new HttpHost(url.getHost(), url.getPort());
		HttpContext context = HTTPEngine4Impl.setCredentials(builder, httpHost, username, password, false);

		// Set proxy on builder and request
		HTTPEngine4Impl.setProxy(url.getHost(), builder, request, proxy);

		// Build client
		CloseableHttpClient client = builder.build();

		// Return both client and context
		if (context == null) context = new HttpClientContext();
		return new ClientContext(client, context);
	}

	/**
	 * Simple GET request with default timeouts (10s connect, 60s read)
	 *
	 * @param url URL to download from
	 * @return InputStream of the response content
	 * @throws IOException if download fails
	 * @throws GeneralSecurityException if SSL/TLS fails
	 */
	public static InputStream get(URL url) throws IOException, GeneralSecurityException {
		return get(url, null, null, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT, null, true, Log.LEVEL_DEBUG);
	}

	/**
	 * GET request with custom timeouts
	 *
	 * @param url URL to download from
	 * @param connectTimeout Connection timeout in milliseconds
	 * @param readTimeout Read timeout in milliseconds
	 * @return InputStream of the response content
	 * @throws IOException if download fails
	 * @throws GeneralSecurityException if SSL/TLS fails
	 */
	public static InputStream get(URL url, long connectTimeout, long readTimeout) throws IOException, GeneralSecurityException {
		return get(url, null, null, connectTimeout, readTimeout, null, true, Log.LEVEL_DEBUG);
	}

	/**
	 * GET request with custom User-Agent
	 *
	 * @param url URL to download from
	 * @param connectTimeout Connection timeout in milliseconds
	 * @param readTimeout Read timeout in milliseconds
	 * @param userAgent User-Agent header (can be null, defaults to "Lucee")
	 * @return InputStream of the response content
	 * @throws IOException if download fails
	 * @throws GeneralSecurityException if SSL/TLS fails
	 */
	public static InputStream get(URL url, long connectTimeout, long readTimeout, String userAgent) throws IOException, GeneralSecurityException {
		return get(url, null, null, connectTimeout, readTimeout, userAgent, true, Log.LEVEL_DEBUG);
	}

	/**
	 * GET request with full options
	 *
	 * @param url URL to download from
	 * @param username HTTP Basic Auth username (can be null)
	 * @param password HTTP Basic Auth password (can be null)
	 * @param connectTimeout Connection timeout in milliseconds
	 * @param readTimeout Read timeout in milliseconds
	 * @param userAgent User-Agent header (can be null, defaults to "Lucee")
	 * @return InputStream of the response content
	 * @throws IOException if download fails
	 * @throws GeneralSecurityException if SSL/TLS fails
	 */
	public static InputStream get(URL url, String username, String password, long connectTimeout, long readTimeout, String userAgent) throws IOException, GeneralSecurityException {
		return get(url, username, password, connectTimeout, readTimeout, userAgent, true, Log.LEVEL_DEBUG);
	}

	public static InputStream get(URL url, String username, String password, long connectTimeout, long readTimeout, String userAgent, boolean pooling) throws IOException {
		return get(url, username, password, connectTimeout, readTimeout, userAgent, pooling, Log.LEVEL_DEBUG);
	}

	/**
	 * GET request with full options including log level control
	 *
	 * @param url URL to download from
	 * @param username HTTP Basic Auth username (can be null)
	 * @param password HTTP Basic Auth password (can be null)
	 * @param connectTimeout Connection timeout in milliseconds
	 * @param readTimeout Read timeout in milliseconds
	 * @param userAgent User-Agent header (can be null, defaults to "Lucee")
	 * @param logLevel Log level for success messages (Log.LEVEL_TRACE for minimal logging,
	 *            Log.LEVEL_DEBUG for visibility)
	 * @return InputStream of the response content
	 * @throws IOException if download fails
	 * @throws GeneralSecurityException if SSL/TLS fails
	 */
	public static InputStream get(URL url, String username, String password, long connectTimeout, long readTimeout, String userAgent, boolean pooling, int logLevel)
			throws IOException {

		CloseableHttpResponse response = null;
		try {
			HttpGet request = new HttpGet(url.toString());
			request.setHeader("User-Agent", userAgent != null ? userAgent : DEFAULT_USER_AGENT);

			// 1. DYNAMIC TIMEOUTS: Set on the request, not the builder/client
			request.setConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).setConnectionRequestTimeout(2000).setConnectTimeout((int) connectTimeout)
					.setSocketTimeout((int) readTimeout).build());

			// 2. DYNAMIC AUTH/PROXY: Use a local Context
			HttpClientContext context = HttpClientContext.create();

			// Handle Credentials locally in the context (no builder needed!)
			if (!StringUtil.isEmpty(username, true)) {
				CredentialsProvider credsProvider = new BasicCredentialsProvider();
				credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
				context.setCredentialsProvider(credsProvider);
			}

			// Handle Proxy locally in the context (no builder needed!)
			ProxyData proxy = getProxyData(url.getHost());
			if (ProxyDataImpl.isValid(proxy, url.getHost())) {
				HttpHost proxyHost = new HttpHost(proxy.getServer(), proxy.getPort());
				request.setConfig(RequestConfig.copy(request.getConfig()).setProxy(proxyHost).build());
			}

			// 3. EXECUTE: Use the immutable Shared Client
			// This is where the 3s speed comes from.
			response = getHttpClient(pooling).execute(request, context);

			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode < 200 || statusCode >= 300) {
				// Must close response if we throw an exception here!
				IOUtil.closeEL(response);
				throw new IOException("Failed download: " + statusCode);
			}

			HttpEntity e = response.getEntity();
			if (e == null) {
				IOUtil.closeEL(response);
				return null;
			}

			// 4. WRAP: The connection is now "Leased" until the user closes this stream
			return new HTTPDownloaderInputStream(response, e.getContent());
		}
		catch (Exception e) {
			IOUtil.closeEL(response); // Safety first
			throw new IOException("Download failed [" + url + "]: " + e.getMessage(), e);
		}
	}

	/**
	 * HEAD request returning a decoupled response object. This ensures the connection is returned to
	 * the pool immediately.
	 */
	public static HTTPDownloaderHeadResponse head(URL url, long connectTimeout, long readTimeout, boolean pooling, int logLevel) {
		long start = System.currentTimeMillis();
		HttpHead request = new HttpHead(url.toString());
		request.setHeader("User-Agent", DEFAULT_USER_AGENT);
		request.setConfig(buildRequestConfig(connectTimeout, readTimeout));

		CloseableHttpResponse response = null;
		try {
			response = getHttpClient(pooling).execute(request);

			long duration = System.currentTimeMillis() - start;
			int code = response.getStatusLine().getStatusCode();
			LogUtil.log(logLevel, "download", "HEAD [" + url + "] status: " + code + " in " + duration + "ms");
			return new HTTPDownloaderHeadResponse(response);
		}
		catch (Exception e) {
			LogUtil.log(Log.LEVEL_ERROR, "download", e);
			return null;
		}
		finally {
			IOUtil.closeEL(response);
		}
	}

	/**
	 * HEAD request returning full HTTPResponse
	 *
	 * @param url URL to check
	 * @param connectTimeout Connection timeout in milliseconds
	 * @param readTimeout Read timeout in milliseconds
	 * @param logLevel Log level for messages (Log.LEVEL_TRACE for minimal logging, Log.LEVEL_DEBUG for
	 *            visibility)
	 * @return HTTPResponse object, or null if request fails
	 */
	public static StatusLine head(URL url, long connectTimeout, long readTimeout, int logLevel) {
		long start = System.currentTimeMillis();
		try {
			CloseableHttpClient client = getHttpClient(true);

			HttpHead request = new HttpHead(url.toString());
			request.setHeader("User-Agent", DEFAULT_USER_AGENT);

			// 2. Set timeouts directly on the request
			request.setConfig(RequestConfig.custom().setConnectTimeout((int) connectTimeout).setSocketTimeout((int) readTimeout).setConnectionRequestTimeout(2000).build());

			// 3. Use try-with-resources (or try-finally) to ensure IMMEDIATE release
			try (CloseableHttpResponse response = client.execute(request)) {
				long duration = System.currentTimeMillis() - start;
				LogUtil.log(logLevel, "download", "HEAD [" + url + "] " + response.getStatusLine().getStatusCode() + " in " + duration + "ms");

				// Return only the data, NOT the response object
				return response.getStatusLine();
			}
			catch (Exception e) {
				LogUtil.log(Log.LEVEL_ERROR, "download", e);
				return null;
			}
		}
		catch (Exception e) {
			return null;
		}
		// Connection is automatically returned to pool here!
	}

	/**
	 * HEAD request to check if URL exists (with default timeouts)
	 *
	 * @param url URL to check
	 * @return true if URL exists (200-299 status code), false otherwise
	 */
	public static boolean exists(URL url) {
		return exists(url, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT, true);
	}

	/**
	 * HEAD request to check if URL exists
	 *
	 * @param url URL to check
	 * @param connectTimeout Connection timeout in milliseconds
	 * @param readTimeout Read timeout in milliseconds
	 * @return true if URL exists (200-299 status code), false otherwise
	 */
	public static boolean exists(URL url, long connectTimeout, long readTimeout, boolean pooling) {
		HTTPDownloaderHeadResponse response = head(url, connectTimeout, readTimeout, pooling, Log.LEVEL_DEBUG);
		if (response == null) return false;

		int statusCode = response.getStatusLine().getStatusCode();
		return statusCode >= 200 && statusCode < 300;
	}

	/**
	 * Download to file with atomic write (via temp file) using default timeouts
	 *
	 * @param url URL to download from
	 * @param target Target file
	 * @throws IOException if download or file operations fail
	 * @throws GeneralSecurityException if SSL/TLS fails
	 */
	public static void downloadToFile(URL url, File target) throws IOException, GeneralSecurityException {
		downloadToFile(url, target, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT, null);
	}

	/**
	 * Download to file with atomic write (via temp file) with custom timeouts
	 *
	 * @param url URL to download from
	 * @param target Target file
	 * @param connectTimeout Connection timeout in milliseconds
	 * @param readTimeout Read timeout in milliseconds
	 * @throws IOException if download or file operations fail
	 * @throws GeneralSecurityException if SSL/TLS fails
	 */
	public static void downloadToFile(URL url, File target, long connectTimeout, long readTimeout) throws IOException, GeneralSecurityException {
		downloadToFile(url, target, connectTimeout, readTimeout, null);
	}

	/**
	 * Download to file with atomic write (via temp file)
	 *
	 * @param url URL to download from
	 * @param target Target file
	 * @param connectTimeout Connection timeout in milliseconds
	 * @param readTimeout Read timeout in milliseconds
	 * @param userAgent User-Agent header (can be null)
	 * @throws IOException if download or file operations fail
	 * @throws GeneralSecurityException if SSL/TLS fails
	 */
	public static void downloadToFile(URL url, File target, long connectTimeout, long readTimeout, String userAgent) throws IOException, GeneralSecurityException {

		InputStream is = null;
		Resource temp = null;

		try {
			is = get(url, null, null, connectTimeout, readTimeout, userAgent, true, Log.LEVEL_DEBUG);

			// Download to temp file first (atomic write)
			temp = SystemUtil.getTempFile("download", false);
			IOUtil.copy(is, temp.getOutputStream(), true, true);

			// Atomic move to target
			File tempFile = ResourceUtil.toFile(temp);
			if (!tempFile.renameTo(target)) {
				// renameTo failed, try copying instead
				Resource targetResource = ResourceUtil.toResource(target);
				IOUtil.copy(temp, targetResource.getOutputStream(), true);
				tempFile.delete();
			}

		}
		finally {
			IOUtil.closeEL(is);
			if (temp != null && temp.exists()) {
				temp.delete();
			}
		}
	}

	public static class HTTPDownloaderHeadResponse {

		private StatusLine sl;
		private Header[] headers;

		public HTTPDownloaderHeadResponse(CloseableHttpResponse response) {
			headers = response.getAllHeaders();
			sl = response.getStatusLine();

		}

		public StatusLine getStatusLine() {
			return sl;
		}

		public Header[] getAllHeaders() {
			return headers;
		}

	}

	private static class HTTPDownloaderInputStream extends InputStream {

		private CloseableHttpResponse rsp;
		private InputStream is;

		public HTTPDownloaderInputStream(CloseableHttpResponse rsp, InputStream is) {
			this.rsp = rsp;
			this.is = is;
		}

		@Override
		public int read(byte[] b) throws IOException {
			return is.read(b);
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			return is.read(b, off, len);
		}

		@Override
		public byte[] readAllBytes() throws IOException {
			return is.readAllBytes();
		}

		@Override
		public byte[] readNBytes(int len) throws IOException {
			return is.readNBytes(len);
		}

		@Override
		public int readNBytes(byte[] b, int off, int len) throws IOException {
			return is.readNBytes(b, off, len);
		}

		@Override
		public long skip(long n) throws IOException {
			return is.skip(n);
		}

		@Override
		public int available() throws IOException {
			return is.available();
		}

		@Override
		public void close() throws IOException {
			try {
				is.close();
			}
			finally {
				rsp.close();
			}
		}

		@Override
		public synchronized void mark(int readlimit) {
			is.mark(readlimit);
		}

		@Override
		public synchronized void reset() throws IOException {
			is.reset();
		}

		@Override
		public boolean markSupported() {
			return is.markSupported();
		}

		@Override
		public long transferTo(OutputStream out) throws IOException {
			return is.transferTo(out);
		}

		@Override
		public int read() throws IOException {
			return is.read();
		}
	}
}
