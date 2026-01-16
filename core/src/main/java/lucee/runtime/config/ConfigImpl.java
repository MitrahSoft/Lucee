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
package lucee.runtime.config;

import static lucee.runtime.db.DatasourceManagerImpl.QOQ_DATASOURCE_NAME;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.ref.SoftReference;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

import lucee.print;
import lucee.commons.date.TimeZoneConstants;
import lucee.commons.io.CharsetUtil;
import lucee.commons.io.FileUtil;
import lucee.commons.io.SystemUtil;
import lucee.commons.io.cache.Cache;
import lucee.commons.io.log.Log;
import lucee.commons.io.log.LogEngine;
import lucee.commons.io.log.LogFactory;
import lucee.commons.io.log.LogUtil;
import lucee.commons.io.log.LoggerAndSourceData;
import lucee.commons.io.res.Resource;
import lucee.commons.io.res.ResourceProvider;
import lucee.commons.io.res.Resources;
import lucee.commons.io.res.ResourcesImpl;
import lucee.commons.io.res.ResourcesImpl.ResourceProviderFactory;
import lucee.commons.io.res.filter.ExtensionResourceFilter;
import lucee.commons.io.res.util.ResourceUtil;
import lucee.commons.lang.ByteSizeParser;
import lucee.commons.lang.CharSet;
import lucee.commons.lang.ClassException;
import lucee.commons.lang.ClassUtil;
import lucee.commons.lang.ExceptionUtil;
import lucee.commons.lang.Md5;
import lucee.commons.lang.PhysicalClassLoader;
import lucee.commons.lang.PhysicalClassLoaderFactory;
import lucee.commons.lang.SerializableObject;
import lucee.commons.lang.StringUtil;
import lucee.commons.lang.types.RefBoolean;
import lucee.commons.net.IPRange;
import lucee.loader.TP;
import lucee.runtime.CIPage;
import lucee.runtime.Component;
import lucee.runtime.Mapping;
import lucee.runtime.MappingFactory;
import lucee.runtime.MappingImpl;
import lucee.runtime.Page;
import lucee.runtime.PageContext;
import lucee.runtime.PageSource;
import lucee.runtime.ai.AIEngine;
import lucee.runtime.ai.AIEngineFactory;
import lucee.runtime.cache.CacheConnection;
import lucee.runtime.cache.CacheConnectionFactory;
import lucee.runtime.cache.ram.RamCache;
import lucee.runtime.cache.tag.CacheHandler;
import lucee.runtime.cfx.CFXTagPool;
import lucee.runtime.cfx.customtag.CFXTagClass;
import lucee.runtime.cfx.customtag.CFXTagPoolImpl;
import lucee.runtime.cfx.customtag.JavaCFXTagClassFactory;
import lucee.runtime.component.ImportDefintion;
import lucee.runtime.component.ImportDefintionImpl;
import lucee.runtime.config.ConfigFactoryImpl.Path;
import lucee.runtime.config.ConfigUtil.CacheElement;
import lucee.runtime.config.Prop.Choice;
import lucee.runtime.config.gateway.GatewayMap;
import lucee.runtime.customtag.InitFile;
import lucee.runtime.db.ClassDefinition;
import lucee.runtime.db.DataSource;
import lucee.runtime.db.DataSourceFactory;
import lucee.runtime.db.DataSourcePro;
import lucee.runtime.db.DatasourceConnectionFactory;
import lucee.runtime.db.JDBCDriver;
import lucee.runtime.dump.DumpWriter;
import lucee.runtime.dump.DumpWriterEntry;
import lucee.runtime.dump.HTMLDumpWriter;
import lucee.runtime.engine.ExecutionLogFactory;
import lucee.runtime.exp.ApplicationException;
import lucee.runtime.exp.DatabaseException;
import lucee.runtime.exp.DeprecatedException;
import lucee.runtime.exp.ExpressionException;
import lucee.runtime.exp.PageException;
import lucee.runtime.exp.PageRuntimeException;
import lucee.runtime.exp.SecurityException;
import lucee.runtime.extension.Extension;
import lucee.runtime.extension.ExtensionDefintion;
import lucee.runtime.extension.ExtensionProvider;
import lucee.runtime.extension.RHExtension;
import lucee.runtime.extension.RHExtensionProvider;
import lucee.runtime.functions.other.CreateUniqueId;
import lucee.runtime.gateway.GatewayEntry;
import lucee.runtime.gateway.GatewayEntryFactory;
import lucee.runtime.listener.AppListenerUtil;
import lucee.runtime.listener.ApplicationContext;
import lucee.runtime.listener.ApplicationListener;
import lucee.runtime.listener.JavaSettings;
import lucee.runtime.listener.JavaSettingsImpl;
import lucee.runtime.listener.ModernAppListener;
import lucee.runtime.net.mail.Server;
import lucee.runtime.net.mail.ServerFactory;
import lucee.runtime.net.proxy.ProxyData;
import lucee.runtime.net.proxy.ProxyDataImpl;
import lucee.runtime.op.Caster;
import lucee.runtime.op.Decision;
import lucee.runtime.op.Duplicator;
import lucee.runtime.orm.ORMConfiguration;
import lucee.runtime.orm.ORMEngine;
import lucee.runtime.osgi.BundleInfo;
import lucee.runtime.osgi.EnvClassLoader;
import lucee.runtime.osgi.OSGiUtil.BundleDefinition;
import lucee.runtime.reflection.Reflector;
import lucee.runtime.regex.Regex;
import lucee.runtime.regex.RegexFactory;
import lucee.runtime.rest.RestSettingImpl;
import lucee.runtime.rest.RestSettings;
import lucee.runtime.schedule.Scheduler;
import lucee.runtime.schedule.SchedulerImpl;
import lucee.runtime.search.SearchEngine;
import lucee.runtime.security.SecretProvider;
import lucee.runtime.security.SecretProviderFactory;
import lucee.runtime.security.SecurityManager;
import lucee.runtime.security.SecurityManagerImpl;
import lucee.runtime.spooler.SpoolerEngine;
import lucee.runtime.spooler.SpoolerEngineImpl;
import lucee.runtime.type.Array;
import lucee.runtime.type.ArrayImpl;
import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.Struct;
import lucee.runtime.type.StructImpl;
import lucee.runtime.type.UDF;
import lucee.runtime.type.dt.TimeSpan;
import lucee.runtime.type.dt.TimeSpanImpl;
import lucee.runtime.type.scope.ClusterNotSupported;
import lucee.runtime.type.scope.Undefined;
import lucee.runtime.type.util.ArrayUtil;
import lucee.runtime.type.util.KeyConstants;
import lucee.runtime.type.util.ListUtil;
import lucee.runtime.video.VideoExecuterNotSupported;
import lucee.transformer.dynamic.meta.Method;
import lucee.transformer.library.ClassDefinitionFactory;
import lucee.transformer.library.function.FunctionLib;
import lucee.transformer.library.function.FunctionLibException;
import lucee.transformer.library.function.FunctionLibFactory;
import lucee.transformer.library.function.FunctionLibFunction;
import lucee.transformer.library.function.FunctionLibFunctionArg;
import lucee.transformer.library.tag.TagLib;
import lucee.transformer.library.tag.TagLibException;
import lucee.transformer.library.tag.TagLibFactory;
import lucee.transformer.library.tag.TagLibTag;
import lucee.transformer.library.tag.TagLibTagAttr;
import lucee.transformer.library.tag.TagLibTagScript;

/**
 * Hold the definitions of the Lucee configuration.
 */
public abstract class ConfigImpl extends ConfigBase implements ConfigPro {

	private static final RHExtension[] RHEXTENSIONS_EMPTY = new RHExtension[0];

	//////////////////////////
	// no need to expose // TODO still use Prop
	//////////////////////////
	private TagLib[] cfmlTlds;
	private Resource tldFile;
	private FunctionLib cfmlFlds;
	private Resource fldFile;
	private RHExtensionProvider[] rhextensionProviders;

	//////////////////////////
	// not read from config //
	//////////////////////////
	private Resource configFile;
	private Resource configDir;
	protected Struct root;
	private Integer mode;
	private static final double DEFAULT_VERSION = 5.0d;
	private long loadTime;
	private final Map<String, PhysicalClassLoader> rpcClassLoaders = new ConcurrentHashMap<String, PhysicalClassLoader>();
	private PhysicalClassLoader directClassLoader;
	private boolean suppresswhitespace = false;
	private long timeOffset;
	private final String baseComponentTemplate = "Component.cfc";
	private PageSource baseComponentPageSource;
	private long sessionScopeDirSize = 1024 * 1024 * 100;
	private Resource sessionScopeDir;
	private Resource deployDir;
	private boolean newVersion;
	private AtomicBoolean insideLoggers = new AtomicBoolean(false);
	private boolean componentRootSearch = true;
	private long configFileLastModified;
	private ComponentPathCache componentPathCache = new ComponentPathCache();
	private final Map<String, DatasourceConnPool> pools = new ConcurrentHashMap<>();
	protected MappingImpl scriptMapping;
	private Class clusterClass = ClusterNotSupported.class;
	private Class videoExecuterClass = VideoExecuterNotSupported.class;
	private AdminSync adminSync;
	private Map<Integer, CacheConnection> cacheDefaultConnection = null;
	private ClassLoader envClassLoader;
	private static Object token = new Object();
	private SpoolerEngine remoteClientSpoolerEngine;
	private String extensionsMD5;
	//////////////////////////
	//////////////////////////

	private static Prop<CacheConnection> metaCacheConnection = Prop.custom(CacheConnectionFactory.getInstance(), Prop.TYPE_MAP).keys("caches")
			.access(SecurityManager.TYPE_DATASOURCE).description("Defines cache connections for data storage, sessions, and distributed locks."
					+ " Supports OSGi/Maven driver loading and specific role assignments like 'storage' or 'default' query caching.");
	private Map<String, CacheConnection> cacheConnection;

	private static String depText = "set instead 'default' with the cache connection itself";
	private static Prop<String> metaCacheDefaultConnectionNamesResource = Prop.str().keys("defaultResource", "cacheDefaultResource").parent("cache").deprecated()
			.description(depText);
	private static Prop<String> metaCacheDefaultConnectionNamesFunction = Prop.str().keys("defaultFunction", "cacheDefaultFunction").parent("cache").deprecated()
			.description(depText);
	private static Prop<String> metaCacheDefaultConnectionNamesInclude = Prop.str().keys("defaultInclude", "cacheDefaultInclude").parent("cache").deprecated().description(depText);
	private static Prop<String> metaCacheDefaultConnectionNamesQuery = Prop.str().keys("defaultQuery", "cacheDefaultQuery").parent("cache").deprecated().description(depText);
	private static Prop<String> metaCacheDefaultConnectionNamesTemplate = Prop.str().keys("defaultTemplate", "cacheDefaultTemplate").parent("cache").deprecated()
			.description(depText);
	private static Prop<String> metaCacheDefaultConnectionNamesObject = Prop.str().keys("defaultObject", "cacheDefaultObject").parent("cache").deprecated().description(depText);
	private static Prop<String> metaCacheDefaultConnectionNamesFile = Prop.str().keys("defaultFile", "cacheDefaultFile").parent("cache").deprecated().description(depText);
	private static Prop<String> metaCacheDefaultConnectionNamesHTTP = Prop.str().keys("defaultHTTP", "cacheDefaultHTTP").parent("cache").deprecated().description(depText);
	private static Prop<String> metaCacheDefaultConnectionNamesWebservice = Prop.str().keys("defaultWebservice", "cacheDefaultWebservice").parent("cache").deprecated()
			.description(depText);
	private Map<Integer, String> cacheDefaultConnectionNames = null;

	private static Prop<DataSource> metaDatasourcesAll = Prop.custom(DataSourceFactory.getInstance(), Prop.TYPE_MAP).keys("dataSources").access(SecurityManager.TYPE_DATASOURCE)
			.description("Defines database connections. Supports loading drivers via OSGi bundles or Maven coordinates,"
					+ " cloud-native credential resolution, and advanced pooling controls like connection limits and live timeouts.");
	private Map<String, DataSource> datasourcesAll;
	private Map<String, DataSource> datasourcesNoQoQ;

	@SuppressWarnings("unchecked")
	private static Prop<Short> metaScopeType = Prop.shor().keys("scopeCascading").defaultValue(SCOPE_STANDARD)
			.choices(
					new Choice<Short>(SCOPE_STRICT, "strict").description(
							"High Performance: Scans 'Arguments', 'Local' (within functions), and 'Variables' scopes only. External scopes like URL/Form are ignored."),

					new Choice<Short>(SCOPE_SMALL, "small")
							.description("Balanced: Scans 'Arguments', 'Local', 'Variables', 'URL', and 'Form'. Excludes more expensive scopes like CGI and Cookie."),

					new Choice<Short>(SCOPE_STANDARD, "standard")
							.description("Standard CFML: Scans 'Arguments', 'Local', 'Variables', 'CGI', 'URL', 'Form', and 'Cookie'. Matches traditional CFML engine behavior."))
			.description("Defines the search strategy for unscoped variables. 'Strict' is recommended for modern, secure applications to prevent unintended scope injection.");
	private Short scopeType;

	private static Prop<Boolean> metaAllowImplicidQueryCall = Prop.bool().keys("cascadeToResultset", "searchResults").systemPropEnvVar("lucee.cascade.to.resultset")
			.defaultValue(true).description(
					"When a variable has no scope defined (Example: #myVar# instead of #variables.myVar#), Lucee will also search available resultsets (CFML Standard) or not");
	private Boolean allowImplicidQueryCall;

	private static Prop<Boolean> metaLimitEvaluation = Prop.bool().keys("limitEvaluation")
			.systemPropEnvVar("lucee.security.limitEvaluation", "lucee.security.isdefined", "lucee.isdefined.limit").defaultValue(false).parent("security").description(
					"If enable you cannot use expression within \"[ ]\" like this susi[getVariableName()] . This affects the following functions [IsDefined, structGet, empty] and the following tags [savecontent attribute \"variable\"].");
	private Boolean limitEvaluation;

	private static Prop<Boolean> metaMergeFormAndURL = Prop.bool().keys("mergeUrlForm").defaultValue(false).description(
			"This setting defines if the scopes URL and Form will be merged together (CFML Default is false). If a key already exists in Form and URL Scopes, the value from the Form Scope is used.");
	private Boolean mergeFormAndURL;

	private static Prop<LoggerAndSourceData> metaLoggers = Prop.custom(LogFactory.getInstance(), Prop.TYPE_MAP).keys("loggers").logGlobal()
			.description("definition of all logs for Lucee");
	private Map<String, LoggerAndSourceData> loggers;

	private static Prop<Boolean> metaDebugLogOutput = Prop.bool().keys("debuggingLogOutput").defaultValue(false);
	private Boolean debugLogOutput;

	// debug options
	private static Prop<Boolean> metaDebugOptionsDatabase = Prop.bool().keys("debuggingDatabase", "debuggingShowDatabase").systemPropEnvVar("lucee.monitoring.debuggingDatabase")
			.defaultValue(false).access(SecurityManager.TYPE_DEBUGGING)
			.description("Select this option to log the database activity for the SQL Query events and Stored Procedure events.");

	private static Prop<Boolean> metaDebugOptionsException = Prop.bool().keys("debuggingException", "debuggingShowException")
			.systemPropEnvVar("lucee.monitoring.debuggingException").defaultValue(false).access(SecurityManager.TYPE_DEBUGGING)
			.description("Select this option to log all exceptions raised for the request.");

	private static Prop<Boolean> metaDebugOptionsTemplate = Prop.bool().keys("debuggingTemplate", "debuggingShowTemplate").systemPropEnvVar("lucee.monitoring.debuggingTemplate")
			.defaultValue(false).access(SecurityManager.TYPE_DEBUGGING).description("Select this option log template activity for all cfm and cfc templates.");

	private static Prop<Boolean> metaDebugOptionsDump = Prop.bool().keys("debuggingDump", "debuggingShowDump").systemPropEnvVar("lucee.monitoring.debuggingDump")
			.defaultValue(false).access(SecurityManager.TYPE_DEBUGGING)
			.description("Select this option to enable output produced with help of the tag cfdump and send to debugging.");

	private static Prop<Boolean> metaDebugOptionsTracing = Prop.bool().keys("debuggingTracing", "debuggingShowTracing", "debuggingShowTrace")
			.systemPropEnvVar("lucee.monitoring.debuggingTracing").defaultValue(false).access(SecurityManager.TYPE_DEBUGGING)
			.description("Select this option to log trace event information. Tracing lets a developer track program flow and efficiency through the use of the CFTRACE tag.");

	private static Prop<Boolean> metaDebugOptionsTimer = Prop.bool().keys("debuggingTimer", "debuggingShowTimer").systemPropEnvVar("lucee.monitoring.debuggingTimer")
			.defaultValue(false).access(SecurityManager.TYPE_DEBUGGING).description(
					"Select this option to show timer event information. Timers let a developer track the execution time of the code between the start and end tags of the CFTIMER tag.");

	private static Prop<Boolean> metaDebugOptionsImplicitAccess = Prop.bool().keys("debuggingImplicitAccess", "debuggingImplicitVariableAccess", "debuggingShowImplicitAccess")
			.systemPropEnvVar("lucee.monitoring.debuggingImplicitAccess").defaultValue(false).access(SecurityManager.TYPE_DEBUGGING)
			.description("Select this option to log all accesses to scopes, queries and threads that happens implicit (cascaded).");

	private static Prop<Boolean> metaDebugOptionsQueryUsage = Prop.bool().keys("debuggingQueryUsage", "debuggingShowQueryUsage")
			.systemPropEnvVar("lucee.monitoring.debuggingQueryUsage").defaultValue(false).access(SecurityManager.TYPE_DEBUGGING)
			.description("Select this option to also log query usage.");

	private static Prop<Boolean> metaDebugOptionsThread = Prop.bool().keys("debuggingThread", "debuggingShowThread").systemPropEnvVar("lucee.monitoring.debuggingThread")
			.defaultValue(false).access(SecurityManager.TYPE_DEBUGGING).description("Select this option to also log threads.");
	private Integer debugOptions;

	private static Prop<Boolean> metaSuppressContent = Prop.bool().keys("suppressContent").defaultValue(false)
			.description("Suppress content written to response stream when a Component is invoked remotely. Only works if the content was not flushed before.");
	private Boolean suppressContent;

	private static Prop<Boolean> metaShowVersion = Prop.bool().keys("showVersion").defaultValue(false)
			.description("deprected: expose Lucee version information in response header.");
	private Boolean showVersion;

	private static Prop<String> metaTempDirectory = Prop.str().keys("tempDirectory");
	private Resource tempDirectory;
	private boolean tempDirectoryReload;

	private static Prop<TimeSpan> metaClientTimeout = Prop.timespan().keys("clientTimeout").systemPropEnvVar("lucee.clienttimeout").defaultValue(new TimeSpanImpl(0, 0, 90, 0))
			.description("Sets the amount of time Lucee will keep the client scope alive.");
	private TimeSpan clientTimeout;

	private static Prop<TimeSpan> metaSessionTimeout = Prop.timespan().keys("sessionTimeout").systemPropEnvVar("lucee.sessiontimeout").defaultValue(new TimeSpanImpl(0, 0, 30, 0))
			.description("Sets the amount of time Lucee will keep the session scope alive. This behaviour can be overridden by the tag cfapplication.");
	private TimeSpan sessionTimeout;

	private static Prop<TimeSpan> metaApplicationTimeout = Prop.timespan().keys("applicationTimeout").systemPropEnvVar("lucee.applicationtimeout")
			.defaultValue(new TimeSpanImpl(1, 0, 0, 0))
			.description("Sets the amount of time Lucee will keep the application scope alive. This behaviour can be overridden by the tag cfapplication.");
	private TimeSpan applicationTimeout;

	private static Prop<TimeSpan> metaRequestTimeout = Prop.timespan().keys("requestTimeout").systemPropEnvVar("lucee.requesttimeout").defaultValue(new TimeSpanImpl(0, 0, 0, 50))
			.description("Defines how Lucee handles long running requests.");
	private TimeSpan requestTimeout;

	private static Prop<Boolean> metaSessionManagement = Prop.bool().keys("sessionManagement").systemPropEnvVar("lucee.sessionmanagement").defaultValue(true)
			.description("By default session management can be enabled. This behaviour can be overridden by the tag cfapplication.");
	private Boolean sessionManagement;

	private static Prop<Boolean> metaClientManagement = Prop.bool().keys("clientManagement").systemPropEnvVar("lucee.clientmanagement").defaultValue(false)
			.description("By default client management can be enabled. This behaviour can be overridden by the tag cfapplication.");
	private Boolean clientManagement;

	private static Prop<Boolean> metaClientCookies = Prop.bool().keys("clientCookies").defaultValue(true)
			.description("Enable or disable client cookies. This behaviour can be overridden by the tag cfapplication.");
	private Boolean clientCookies;

	private static Prop<Boolean> metaDevelopMode = Prop.bool().keys("developMode").defaultValue(DEFAULT_DEVELOP_MODE);
	private Boolean developMode;

	private static Prop<Boolean> metaDomainCookies = Prop.bool().keys("domainCookies").defaultValue(false)
			.description("Enable or disable domain cookies. This behaviour can be overridden by the tag cfapplication.");
	private Boolean domainCookies;

	private static Prop<String> metaSessionStorage = Prop.str().keys("sessionStorage").defaultValue(DEFAULT_STORAGE_SESSION)
			.description("The default storage for sessions can be set to \"memory\" for non-persistent in-memory data, \"file\" to store data on the local filesystem, "
					+ "or the specific name of a cache or datasource instance provided that \"Storage\" has been enabled for that instance.");
	private String sessionStorage;

	private static Prop<String> metaClientStorage = Prop.str().keys("clientStorage").defaultValue(DEFAULT_STORAGE_CLIENT)
			.description("The default storage for client can be set to \"memory\" for non-persistent in-memory data, \"file\" to store data on the local filesystem, "
					+ "or the specific name of a cache or datasource instance provided that \"Storage\" has been enabled for that instance.");
	private String clientStorage;

	private static Prop<Integer> metaSpoolInterval = Prop.integer().keys("mailSpoolInterval").defaultValue(30).access(SecurityManager.TYPE_MAIL)
			.description("interval in seconds Lucee checks for new mails to send");
	private int spoolInterval = -1;

	private static Prop<Boolean> metaSpoolEnable = Prop.bool().keys("mailSpoolEnable").defaultValue(true).access(SecurityManager.TYPE_MAIL)
			.description("if true, the mails are sent in a background thread and the main request does not have to wait until the mails are sent.");
	private Boolean spoolEnable;

	private static Prop<Boolean> metaSendPartial = Prop.bool().keys("mailSendPartial").defaultValue(false).access(SecurityManager.TYPE_MAIL).description(
			"This setting determines whether the SMTP protocol should deliver a message to all valid recipients when some addresses are invalid, rather than failing the entire delivery attempt if a single recipient is rejected.");
	private Boolean sendPartial;//

	private static Prop<Boolean> metaUserSet = Prop.bool().keys("mailUserSet").defaultValue(true).access(SecurityManager.TYPE_MAIL).description(
			"This setting determines whether the SMTP protocol should explicitly use the sender's identity for the \"From\" address during the mail handshake, rather than relying on the default server identity or an automatically generated system address.");
	private Boolean userSet;//

	private static Prop<CharSet> metaMailDefaultCharset = Prop.charSet().keys("mailDefaultEncoding", "mailDefaultCharset").access(SecurityManager.TYPE_MAIL)
			.defaultValue(CharSet.UTF8).description("default charset used for sending mails");
	private CharSet mailDefaultCharset;

	private static Prop<Integer> metaMailTimeout = Prop.integer().keys("mailConnectionTimeout", "mailTimeout").access(SecurityManager.TYPE_MAIL).defaultValue(30)
			.description("default mail connection timeout in seconds");
	private int mailTimeout = -1;

	private static Prop<Server> metaMailServers = Prop.custom(ServerFactory.getInstance(), Prop.TYPE_LIST).keys("mailServers").access(SecurityManager.TYPE_MAIL)
			.description("mailserver to use for sending mails.");
	private Server[] mailServers;

	@SuppressWarnings("unchecked")
	private static Prop<Integer> metaReturnFormat = Prop.integer().keys("returnFormat").defaultValue(UDF.RETURN_FORMAT_WDDX).choices(
			new Choice<Integer>(UDF.RETURN_FORMAT_WDDX, "wddx").description("Web Distributed Data eXchange (WDDX) format. This is the legacy default for CFML remote calls."),
			new Choice<Integer>(UDF.RETURN_FORMAT_JSON, "json").description("Javascript Object Notation (JSON). The modern standard for web APIs and AJAX requests."),
			new Choice<Integer>(UDF.RETURN_FORMAT_PLAIN, "text", "plain").description("Returns the raw string output of the function without any additional serialization."),
			new Choice<Integer>(UDF.RETURN_FORMAT_JAVA, "java").description("Binary-encoded Java objects. Used for high-performance communication between Java-based systems."),
			new Choice<Integer>(UDF.RETURN_FORMAT_SERIALIZE, "cfm", "cfml", "serialize")
					.description("Lucee's internal object serialization format, ideal for passing complex objects between Lucee instances."),
			new Choice<Integer>(UDF.RETURN_FORMAT_XML, "xml").description("Structured eXtensible Markup Language (XML). Used for legacy SOAP services or XML-based data exchange."))
			.description(
					"Defines the serialization format for remote function calls. While 'wddx' is the default for backward compatibility, 'json' is recommended for modern web applications.");
	private Integer returnFormat;

	private static Prop<TimeZone> metaTimeZone = Prop.timezone().keys("timezone", "thisTimezone")
			.defaultValue(TimeZone.getDefault() != null ? TimeZone.getDefault() : TimeZoneConstants.UTC)
			.description("Define the desired time zone for Lucee. This will also change the time for the context of the web.");
	private TimeZone timeZone;

	private ClassDefinition<SearchEngine> searchEngineClassDef;

	private static Prop<String> metaSearchEngineDirectory = Prop.str().keys("directory").parent("search").defaultValue("{lucee-web}/search/")
			.description("search engine directory");
	private String searchEngineDirectory;

	private static Prop<Locale> metaLocale = Prop.locale().keys("locale", "thisLocale").defaultValue(Locale.US)
			.description("Define the desired time locale for Lucee, this will change the default locale for the context of the web.");
	private Locale locale;

	private static Prop<Boolean> metaPsq = Prop.bool().keys("preserveSingleQuote", "datasourcePreserveSingleQuotes").defaultValue(false)
			.description("Preserve single quotes (\") in the SQL defined with the tag cfquery");
	private Boolean psq;

	private static Prop<String> metaErrorTemplate500 = Prop.str().keys("errorGeneralTemplate", "generalErrorTemplate").access(SecurityManager.TYPE_DEBUGGING)
			.defaultValue("/lucee/templates/error/error." + (Constants.getCFMLTemplateExtensions()[0])).description(
					"This setting specifies the custom file path for the template rendered during all uncaught internal server exceptions, providing a tailored response for unexpected application failures.");
	private String errorTemplate500;

	private static Prop<String> metaErrorTemplate404 = Prop.str().keys("errorMissingTemplate", "missingErrorTemplate").access(SecurityManager.TYPE_DEBUGGING)
			.defaultValue("/lucee/templates/error/error." + (Constants.getCFMLTemplateExtensions()[0])).description(
					"This setting specifies the custom file path for the template rendered whenever a requested resource is not found on the server, ensuring a controlled and helpful response for status 404 missing page exceptions.");
	private String errorTemplate404;

	private static Prop<Password> metaPassword = Prop.custom(PasswordFactory.getInstance()).keys("hspw", "adminhspw", "adminpw", "pw", "adminpassword", "password")
			.systemPropEnvVar("lucee.admin.password");
	protected Password password;
	private boolean initPassword = true;

	private static Prop<String> metaSalt = Prop.str().keys("salt", "adminSalt").systemPropEnvVar("lucee.admin.salt");
	private String salt;

	private static Prop<Mapping> metaMappings = Prop.custom(MappingFactory.getInstance(MappingFactory.TYPE_REGULAR), Prop.TYPE_MAP).keys("mappings", "CFMappings")
			.description("Maps logical paths to storage locations, supporting local filesystems and virtual providers like S3. "
					+ "Includes controls for 'physical' vs 'archive' priority, 'toplevel' browser visibility, 'inspectTemplate' change-detection rules, "
					+ "and 'listener' configurations for Application.cfc discovery.");
	private Mapping[] uncheckedMappings;
	private Mapping[] mappings;

	private static Prop<Mapping> metaCustomTagMappings = Prop.custom(MappingFactory.getInstance(MappingFactory.TYPE_CUSTOM_TAG), Prop.TYPE_LIST)
			.keys("customTagMappings", "customTagPaths")
			.description("Acts as a component classpath. Maps virtual handles to local or cloud-based directories (S3, etc.) and .lar archives. "
					+ "Controls 'primary' source priority, 'inspectTemplate' caching, and 'toplevel' remote access for component resolution.");
	private Mapping[] uncheckedCustomTagMappings;
	private Mapping[] customTagMappings;

	private static Prop<Mapping> metaComponentMappings = Prop.custom(MappingFactory.getInstance(MappingFactory.TYPE_COMPONENT), Prop.TYPE_LIST)
			.keys("componentMappings", "componentPaths")
			.description("Acts as a component classpath. Maps virtual handles to local or cloud-based directories (S3, etc.) and .lar archives. "
					+ "Controls 'primary' source priority, 'inspectTemplate' caching, and 'toplevel' remote access for component resolution.");
	private Mapping[] uncheckedComponentMappings;
	private Mapping[] componentMappings;

	private static Prop<CFXTagClass> metaCfxTagPool = Prop.custom(JavaCFXTagClassFactory.getInstance(), Prop.TYPE_MAP).keys("cfx").access(SecurityManager.TYPE_CFX_SETTING)
			.deprecated();
	private CFXTagPool cfxTagPool;

	private static Prop<Boolean> metaRestList = Prop.bool().keys("list").parent("rest").defaultValue(false).description("List Services when \"/rest/\" is called");
	private Boolean restList;

	@SuppressWarnings("unchecked")
	private static Prop<Short> metaClientType = Prop.shor().keys("clientType").defaultValue(Config.CLIENT_SCOPE_TYPE_COOKIE)
			.choices(new Choice<Short>(Config.CLIENT_SCOPE_TYPE_FILE, "file"), new Choice<Short>(Config.CLIENT_SCOPE_TYPE_DB, "db", "database"),
					new Choice<Short>(Config.CLIENT_SCOPE_TYPE_COOKIE, "cookie"))
			.deprecated();
	private Short clientType;

	private static Prop<String> metaComponentDumpTemplate = Prop.str().keys("componentDumpTemplate").defaultValue("/lucee/component-dump.cfm")

			.description("If you call a component directly this template will be invoked to dump the component. (Example: http://localhost:8888/lucee/Admin.cfc)");
	private String componentDumpTemplate;

	@SuppressWarnings("unchecked")
	private static Prop<Integer> metaComponentDataMemberDefaultAccess = Prop.integer().keys("componentDataMemberAccess").defaultValue(Component.ACCESS_PUBLIC).choices(
			new Choice<Integer>(Component.ACCESS_REMOTE, "remote").description("External/API: Allows data members to be accessed via remote protocols."),

			new Choice<Integer>(Component.ACCESS_PUBLIC, "public").description("Open: Data members are accessible from any other component or script (Default)."),

			new Choice<Integer>(Component.ACCESS_PACKAGE, "package").description("Restricted: Data members are only accessible by components within the same directory/package."),

			new Choice<Integer>(Component.ACCESS_PRIVATE, "private")
					.description("Strict: Data members are only accessible within the component itself or by components that extend it."))
			.description(
					"Determines the default visibility for data members in the 'this' scope of a component. This allows you to control how internal state is exposed to the rest of the application or external consumers.");
	private Integer componentDataMemberDefaultAccess;

	private static Prop<Boolean> metaTriggerComponentDataMember = Prop.bool().keys("componentImplicitNotation", "triggerComponentDataMember").defaultValue(false).description(
			"If there is no accessible data member (property, element of the this scope) inside a component, Lucee searches for available matching \"getters\" or \"setters\" for the requested property. The following example should clarify this behaviour. \"somevar = myComponent.properyName\". If \"myComponent\" has no accessible data member named \"propertyName\", Lucee searches for a function member (method) named \"getPropertyName\".");
	private Boolean triggerComponentDataMember;

	@SuppressWarnings("unchecked")
	private static Prop<Short> metaSessionType = Prop.shor().keys("sessionType").defaultValue(SESSION_TYPE_APPLICATION).choices(
			new Choice<Short>(Config.SESSION_TYPE_APPLICATION, "cfml", "cfm", "c", "application")
					.description("Lucee Native: Managed entirely by the engine. Uses 'CFID' and 'CFTOKEN' cookies. Does not require a restart to modify settings."),

			new Choice<Short>(Config.SESSION_TYPE_JEE, "j2ee", "jee", "j").description(
					"Servlet Container: Managed by the underlying server (e.g., Tomcat/Jetty). Uses the 'JSESSIONID' cookie. Better for integration with external Java filters or load balancers."))
			.description(
					"Specifies the session management engine. 'cfml' is the native Lucee implementation, while 'j2ee' delegates session tracking to the underlying servlet container.");
	private Short sessionType;

	private static Prop<String> metaDeployDirectory = Prop.str().keys("deployDirectory").parent("fileSystem").description("folder where Lucee stores template classes");
	private Resource deployDirectory;

	private static Prop<CharSet> metaResourceCharset = Prop.charSet().keys("resourceCharset").systemPropEnvVar("lucee.resource.charset").defaultValue(SystemUtil.getCharSet())
			.description("Default character set for reading from/writing to various resources");
	private CharSet resourceCharset;

	public static void main(String[] args) {
		print.e(Prop.createConfigSchema(true));
	}

	private static Prop<CharSet> metaTemplateCharset = Prop.charSet().keys("templateCharset").systemPropEnvVar("lucee.template.charset").defaultValue(SystemUtil.getCharSet())
			.description("Default character used to read templates (*.cfm and *.cfc files)");
	private CharSet templateCharset;

	private static Prop<CharSet> metaWebCharset = Prop.charSet().keys("webCharset").systemPropEnvVar("lucee.web.charset").defaultValue(CharSet.UTF8)
			.description("Default character set for output streams, form-, url-, and cgi scope variables and reading/writing the header");
	private CharSet webCharset;

	@SuppressWarnings("unchecked")
	private static Prop<Integer> metaApplicationListenerType = Prop.integer().keys("listenerType", "applicationListener").systemPropEnvVar("lucee.listener.type")
			.defaultValue(ApplicationListener.TYPE_MIXED)
			.choices(
					new Choice<Integer>(ApplicationListener.TYPE_NONE, "none")
							.description("Disabled: No application files are processed. Useful for high-performance microservices with no global state."),

					new Choice<Integer>(ApplicationListener.TYPE_CLASSIC, "classic").description("Legacy: Searches only for Application.cfm/OnRequestEnd.cfm files."),

					new Choice<Integer>(ApplicationListener.TYPE_MODERN, "modern").description("Modern: Searches only for Application.cfc components."),

					new Choice<Integer>(ApplicationListener.TYPE_MIXED, "mixed").description("Compatibility: Searches for both Application.cfc and Application.cfm. (Default)"))
			.description("Determines which types of application initialization files Lucee should detect and execute.");

	@SuppressWarnings("unchecked")
	private static Prop<Integer> metaApplicationListenerMode = Prop.integer().keys("listenerMode", "applicationMode").systemPropEnvVar("lucee.listener.mode")
			.defaultValue(ApplicationListener.MODE_CURRENT2ROOT)
			.choices(
					new Choice<Integer>(ApplicationListener.MODE_CURRENT, "current", "curr")
							.description("Local Only: Checks only the directory containing the requested template."),

					new Choice<Integer>(ApplicationListener.MODE_CURRENT2ROOT, "currentToRoot", "currToRoot", "current2root", "curr2root")
							.description("Full Recursive: Searches the current directory and every parent directory until it reaches the webroot. (Standard CFML behavior)"),

					new Choice<Integer>(ApplicationListener.MODE_CURRENT_OR_ROOT, "currentOrRoot", "currOrRoot")
							.description("Local & Root: Checks the current directory; if not found, it checks the webroot, skipping intermediate parent directories."),

					new Choice<Integer>(ApplicationListener.MODE_ROOT, "root").description("Root Only: Checks only the webroot for application files."))
			.description(
					"Specifies the file-system search strategy for locating application files. Recursive searching (currentToRoot) is the most flexible but can add overhead on deep directory structures.");

	private static Prop<Boolean> metaApplicationListenerSingleton = Prop.bool().keys("listenerSingleton", "applicationSingleton")
			.systemPropEnvVar("lucee.listener.singleton", "lucee.application.singleton").defaultValue(false).description(
					"When enabled, Lucee caches a single instance of Application.cfc for the life of the application, reducing overhead compared to the default behavior of reinstantiating it for every request.");
	private ApplicationListener applicationListener;

	private static Prop<String> metaScriptProtect = Prop.str().keys("scriptProtect").systemPropEnvVar("lucee.script.protect").defaultValue("all")
			.description("Determines which scopes are sanitized for XSS protection, accepting 'all', 'none', or a comma-separated list of scopes like 'url,form,cookie,cgi'.");
	private Integer scriptProtect;

	private static Prop<Boolean> metaProxyEnabled = Prop.bool().keys("enabled").systemPropEnvVar("lucee.proxy.enabled").access(SecurityManager.TYPE_SETTING).parent("proxy")
			.defaultValue(true).description("enable proxy");

	private static Prop<String> metaProxyServer = Prop.str().keys("server", "host", "updateProxyHost").systemPropEnvVar("lucee.proxy.host", "lucee.proxy.server")
			.access(SecurityManager.TYPE_SETTING).parent("proxy").description("proxy host");

	private static Prop<String> metaProxyUser = Prop.str().keys("username", "user", "updateProxyUsername").systemPropEnvVar("lucee.proxy.username")
			.access(SecurityManager.TYPE_SETTING).parent("proxy").description("proxy username");

	private static Prop<String> metaProxyPass = Prop.str().keys("password", "pass", "updateProxyPassword").systemPropEnvVar("lucee.proxy.password")
			.access(SecurityManager.TYPE_SETTING).parent("proxy").description("proxy password");

	private static Prop<Integer> metaProxyPort = Prop.integer().keys("port", "updateProxyPort").systemPropEnvVar("lucee.proxy.port").access(SecurityManager.TYPE_SETTING)
			.parent("proxy").description("proxy port");

	private static Prop<String> metaProxyIncludes = Prop.str().keys("includes").systemPropEnvVar("lucee.proxy.includes").access(SecurityManager.TYPE_SETTING).parent("proxy")
			.description("proxy includes");

	private static Prop<String> metaProxyExcludes = Prop.str().keys("excludes").systemPropEnvVar("lucee.proxy.excludes").access(SecurityManager.TYPE_SETTING).parent("proxy")
			.description("proxy excludes");
	private ProxyData proxy = null;

	private static Prop<String> metaClientScopeDir = Prop.str().keys("clientDirectory").description("client scope directory");
	private Resource clientScopeDir;

	private static Prop<String> metaClientScopeDirSize = Prop.str().keys("clientDirectoryMaxSize").systemPropEnvVar("lucee.client.directory.max.size").defaultValue("100mb")

			.description("Defines the maximum allowable disk space for the client scope storage directory, accepting values with unit suffixes like kb, mb, gb, or tb.");
	private Long clientScopeDirSize;

	private static Prop<String> metaCacheDir = Prop.str().keys("cacheDirectory").defaultValue("100mb")
			.description("Defines the maximum allowable disk space for the client scope storage directory, accepting values with unit suffixes like kb, mb, gb, or tb.");
	private Resource cacheDir;

	private static Prop<String> metaCacheDirSize = Prop.str().keys("cacheDirectoryMaxSize").systemPropEnvVar("lucee.cache.directory.max.size").defaultValue("100mb")
			.description("Defines the maximum allowable disk space for the cache directory, accepting values with unit suffixes like kb, mb, gb, or tb.");
	private Long cacheDirSize;

	private static Prop<Boolean> metaUseComponentShadow = Prop.bool().keys("componentUseVariablesScope").defaultValue(true)
			.description("Defines whether a component has an independent variables scope parallel to the \"this\" scope (CFML standard) or not.");
	private Boolean useComponentShadow;

	private static Prop<String> metaOut = Prop.str().keys("systemOut").systemPropEnvVar("lucee.system.out").defaultValue("system").access(SecurityManager.TYPE_SETTING).description(
			"Specifies the destination for standard system output, supporting 'system', 'log', 'null', file paths via 'file:', or custom PrintWriter implementations via 'class:'");
	private PrintWriter out;

	private static Prop<String> metaErr = Prop.str().keys("systemErr").systemPropEnvVar("lucee.system.err").defaultValue("system").access(SecurityManager.TYPE_SETTING).description(
			"Defines the destination for standard error output, supporting values like 'system', 'log', 'null', or specific paths using 'file:' and custom implementations via 'class:'.");
	private PrintWriter err;

	private static Prop<Boolean> metaDoCustomTagDeepSearch = Prop.bool().keys("customTagDeepSearch", "customTagSearchSubdirectories").access(SecurityManager.TYPE_CUSTOM_TAG)
			.defaultValue(false).description("Search for custom tags in subdirectories.");
	private Boolean doCustomTagDeepSearch = null;

	private static Prop<Boolean> metaDoComponentTagDeepSearch = Prop.bool().keys("componentDeepSearch", "componentSearchSubdirectories").defaultValue(false)
			.description("Search for CFCs in the subdirectories.");
	private Boolean doComponentTagDeepSearch;

	private static Prop<Double> metaVersion = Prop.dbl().keys("version").defaultValue(DEFAULT_VERSION).hidden();
	private Double version = null;

	private static Prop<Boolean> metaCloseConnection = Prop.bool().keys("closeConnection").defaultValue(false).description(
			"This setting specifies whether every HTTP response should instruct the client and any intermediate proxies to terminate the network connection immediately after the request is fulfilled, preventing the connection from being reused for additional traffic.");
	private Boolean closeConnection;

	private static Prop<Boolean> metaContentLength = Prop.bool().keys("contentLength").defaultValue(true).deprecated();
	private Boolean contentLength;

	private static Prop<Boolean> metaAllowCompression = Prop.bool().keys("allowCompression").systemPropEnvVar("lucee.allow.compression")
			.defaultValue(ConfigImpl.DEFAULT_ALLOW_COMPRESSION).description(
					"This setting determines whether the response stream is compressed using GZIP; when enabled, Lucee inspects the client's request headers and, if the client supports it, automatically compresses the output to reduce bandwidth usage and improve page load times.");
	private Boolean allowCompression;

	private static Prop<Boolean> metaDoLocalCustomTag = Prop.bool().keys("customTagLocalSearch", "customTagSearchLocal").access(SecurityManager.TYPE_CUSTOM_TAG).defaultValue(true)
			.description("look for custom tags locally.");
	private Boolean doLocalCustomTag;

	private static Prop<Struct> metaConstants = Prop.sct().keys("constants").defaultValue(new StructImpl());
	private Struct constants = null;

	private static Prop<Boolean> metaAllowURLRequestTimeout = Prop.bool().keys("requestTimeoutInURL", "allowUrlRequesttimeout").defaultValue(false)
			.description("Defines if it is possible to overwrite the request timeout with the query string [requestTimeout] in the URL.");
	private Boolean allowURLRequestTimeout;

	private static Prop<Boolean> metaErrorStatusCode = Prop.bool().keys("errorStatusCode").systemPropEnvVar("lucee.status.code").defaultValue(true)
			.access(SecurityManager.TYPE_DEBUGGING).description("In case of an exception, should individual status codes be returned? Untick to always return 200 status code.");
	private Boolean errorStatusCode;

	@SuppressWarnings("unchecked")
	private static Prop<Integer> metaLocalMode = Prop.integer().keys("localScopeMode").defaultValue(Undefined.MODE_LOCAL_OR_ARGUMENTS_ONLY_WHEN_EXISTS).choices(
			new Choice<Integer>(Undefined.MODE_LOCAL_OR_ARGUMENTS_ALWAYS, "always", "modern", Boolean.TRUE).description(
					"Modern: Unscoped assignments inside a function are automatically placed in the 'local' scope. Encourages cleaner code and prevents variable leaks to the 'variables' scope."),

			new Choice<Integer>(Undefined.MODE_LOCAL_OR_ARGUMENTS_ONLY_WHEN_EXISTS, "update", "classic", Boolean.FALSE).description(
					"Classic: Unscoped assignments only update the 'local' scope if the key already exists there; otherwise, they are created in the 'variables' scope. (Legacy default)"))
			.description(
					"Controls how unscoped variable assignments behave inside functions. The 'always' mode is recommended for modern applications to ensure better encapsulation and thread safety.");
	private Integer localMode;

	private static Prop<Boolean> metaAllowRealPath = Prop.bool().keys("allowRealpath").parent("fileSystem").defaultValue(true);
	private Boolean allowRealPath;

	private static Prop<String> metaCustomTagExtensions = Prop.str().keys("customTagExtensions").defaultValue(ListUtil.arrayToList(Constants.getExtensions(), ","))
			.access(SecurityManager.TYPE_CUSTOM_TAG).description("this are the file extensions Lucee allows for custom tags.");
	private String[] customTagExtensions = null;

	private static Prop<Boolean> metaTypeChecking = Prop.bool().keys("typeChecking", "UDFTypeChecking").systemPropEnvVar("lucee.type.checking", "lucee.udf.type.checking")
			.defaultValue(true).description("check the types defined with function arguments and return type");
	private Boolean typeChecking;

	private static Prop<Boolean> metaExecutionLogEnabled = Prop.bool().keys("enabled").parent("executionLog").defaultValue(false);
	private Boolean executionLogEnabled;

	private ExecutionLogFactory executionLogFactory;

	private static ImportDefintion DEFAULT_IMPORT_DEFINITION = new ImportDefintionImpl(Constants.DEFAULT_PACKAGE, "*");
	private static Prop<String> metaComponentDefaultImport = Prop.str().keys("componentAutoImport", "componentDefaultImport").defaultValue(DEFAULT_IMPORT_DEFINITION.toString())
			.description("Defines a package that is automatically imported for all components. Defaults to 'org.lucee.cfml.*' and requires the wildcard suffix.");
	private ImportDefintion componentDefaultImport;

	private static Prop<Boolean> metaComponentLocalSearch = Prop.bool().keys("componentLocalSearch").defaultValue(true)
			.description("If enabled, Lucee looks for component relative to the local position.");
	private Boolean componentLocalSearch;

	private static Prop<Boolean> metaUseComponentPathCache = Prop.bool().keys("componentUseCachePath").defaultValue(true)
			.description("If enabled, Lucee caches where it did find components, what speedup futher access");
	private Boolean useComponentPathCache;

	private static Prop<Boolean> metaUseCTPathCache = Prop.bool().keys("customTagUseCachePath", "customTagCachePaths").access(SecurityManager.TYPE_CUSTOM_TAG).defaultValue(true)
			.description("If enabled, Lucee caches where it did find custom tags, what speedup futher access");
	private Boolean useCTPathCache;

	@SuppressWarnings("unchecked")
	private static Prop<Integer> metaWriterType = Prop.integer().keys("cfmlWriter", "whitespaceManagement").systemPropEnvVar("lucee.cfml.writer")
			.defaultValue(ConfigPro.CFML_WRITER_REFULAR)
			.choices(
					new Choice<Integer>(ConfigPro.CFML_WRITER_REFULAR, "regular", "normal")
							.description("Standard: Outputs the generated content exactly as written in the source files, including all indentation and line breaks."),

					new Choice<Integer>(ConfigPro.CFML_WRITER_WS, "white-space", "simple").description(
							"Basic Compression: Aggressively removes most whitespace and line breaks. Fast execution, but can occasionally impact the layout of pre-formatted text."),

					new Choice<Integer>(ConfigPro.CFML_WRITER_WS_PREF, "white-space-pref", "smart").description(
							"Advanced Optimization: Uses an intelligent algorithm to remove unnecessary whitespace while preserving critical spacing (e.g., inside 'pre' or 'textarea' tags)."))
			.description(
					"Specifies the white-space management strategy for the output stream. Using 'smart' optimization can significantly reduce page weight without breaking HTML layout.");
	protected Integer writerType;

	private static Prop<Boolean> metaHandleUnquotedAttributeValueAsString = Prop.bool().keys("handleUnquotedAttributeValueAsString").defaultValue(true)
			.description("Controls if unquoted tag attributes are treated as literal strings (true) or as variable references (false) for evaluation.");
	private Boolean handleUnQuotedAttrValueAsString;

	private static Prop<Integer> metaQueueMax = Prop.integer().keys("requestQueueMax").systemPropEnvVar("lucee.queue.max").defaultValue(100)
			.description("maximal size of the request quque size");
	private int queueMax = -1;

	private static Prop<Long> metaQueueTimeout = Prop.loong().keys("requestQueueTimeout").systemPropEnvVar("lucee.queue.timeout").defaultValue(0L)
			.description("timeout for an element in the queue in milliseconds");
	private long queueTimeout = -1;

	private static Prop<Boolean> metaQueueEnable = Prop.bool().keys("requestQueueEnable").systemPropEnvVar("lucee.queue.enable").defaultValue(false);
	private Boolean queueEnable;

	@SuppressWarnings("unchecked")
	private static Prop<Integer> metaVarUsage = Prop.integer().keys("variableUsage").parent("security").defaultValue(ConfigPro.QUERY_VAR_USAGE_IGNORE).choices(
			new Choice<Integer>(ConfigPro.QUERY_VAR_USAGE_IGNORE, "ignore", Boolean.FALSE).description(
					"Permissive: Allows unscoped variables inside 'cfquery' without 'cfqueryparam'. This is the legacy default but poses a high risk of SQL injection."),

			new Choice<Integer>(ConfigPro.QUERY_VAR_USAGE_WARN, "warn", "warning").description(
					"Audit Mode: Allows the query to execute but logs a security warning. Useful for identifying vulnerable code in existing applications without breaking them."),

			new Choice<Integer>(ConfigPro.QUERY_VAR_USAGE_ERROR, "error", Boolean.TRUE)
					.description("Strict/Secure: Blocks any 'cfquery' that contains variables not wrapped in 'cfqueryparam'. Highly recommended for modern, secure environments."))
			.description(
					"Controls how Lucee handles raw variables used inside 'cfquery' tags. Enabling 'error' mode effectively prevents SQL injection by mandating parameterized queries.");
	private Integer varUsage;

	private static Prop<TimeSpan> metaCachedAfterTimeRange = Prop.timespan().keys("cachedAfter")
			.description("In case the attribute \"cacheAfter\" is set without the attribute \"cachedwithin\" in the tag \"query\" this time span is used for the element cached.");
	private TimeSpan cachedAfterTimeRange;
	private boolean initCachedAfterTimeRange = true;

	private static Prop<Regex> metaRegex = Prop.custom(RegexFactory.getInstance()).keys("regexType").systemPropEnvVar("lucee.regex.type")
			.defaultValue(RegexFactory.toRegex(RegexFactory.TYPE_PERL, null))
			.description("Which regular expression dialect should be used. Modern (Java dialect) or Classic (Perl5 dialect).");
	private Regex regex;

	private static Prop<TimeSpan> metaApplicationPathCacheTimeout = Prop.timespan().keys("applicationPathTimeout").systemPropEnvVar("lucee.application.path.cache.timeout")
			.defaultValue(TimeSpanImpl.fromMillis(20000L)) // 20 seconds
			.description("Specifies how long Lucee caches the resolved location of Application.cfc or Application.cfm files. "
					+ "Caching this path prevents repetitive, expensive file-system lookups on every request. "
					+ "A shorter timeout detects new application files faster, while a longer timeout improves performance in high-traffic environments.");
	private Long applicationPathCacheTimeout;

	private static Prop<Boolean> metaPreciseMath = Prop.bool().keys("preciseMath").systemPropEnvVar("lucee.precise.math").defaultValue(false)
			.description("If enabled, this improves the accuracy of floating point calculations but makes them slightly slower.");
	private Boolean preciseMath;

	private static Prop<String> metaMainLoggerName = Prop.str().keys("mainLogger").systemPropEnvVar("lucee.logging.main").defaultValue("application")
			.description("defines the main logger used by Lucee, this logger is used when no log name was provided or the provided name does not exist.");
	private String mainLoggerName;

	@SuppressWarnings("unchecked")
	private static Prop<Short> metaCompileType = Prop.shor().keys("compileType").defaultValue(RECOMPILE_NEVER).choices(new Choice<Short>(RECOMPILE_NEVER, "never").description(
			"Performance Optimized: Retains all existing compiled templates across restarts. Fastest startup time, but requires manual clearing if source files were changed while the engine was offline."),

			new Choice<Short>(RECOMPILE_ALWAYS, "always").description(
					"Safe/Clean: Forces a full recompile of all templates immediately upon restart. Ensures no stale code exists, but significantly increases CPU load and startup time."),

			new Choice<Short>(RECOMPILE_AFTER_STARTUP, "after-startup").description(
					"Lazy Reload: Retains existing templates initially, but background tasks gradually re-verify and recompile templates as they are accessed after the system is online."))
			.description(
					"Determines how Lucee handles previously compiled templates (bytecode) after an engine restart. Balancing startup speed against the risk of executing stale code.");
	private short compileType = -1;
	private short inspectTemplate = -1;
	private int inspectTemplateAutoIntervalSlow = ConfigPro.INSPECT_INTERVAL_UNDEFINED;
	private int inspectTemplateAutoIntervalFast = ConfigPro.INSPECT_INTERVAL_UNDEFINED;

	private static Prop<Boolean> metaFormUrlAsStruct = Prop.bool().keys("formUrlAsStruct").defaultValue(true).description(
			"This setting defines if the scopes URL and Form will be merged together (CFML Default is false). If a key already exists in Form and URL Scopes, the value from the Form Scope is used.");
	private Boolean formUrlAsStruct;

	private static Prop<Boolean> metaShowDebug = Prop.bool().keys("showDebug").systemPropEnvVar("lucee.monitoring.showDebug").defaultValue(false);
	private Boolean showDebug;

	private static Prop<Boolean> metaShowDoc = Prop.bool().keys("showDoc", "doc", "documentation", "showReference", "reference").systemPropEnvVar("lucee.monitoring.showDoc")
			.defaultValue(false);
	private Boolean showDoc;

	private static Prop<Boolean> metaShowMetric = Prop.bool().keys("showMetrics", "showMetric", "metric", "metrics").systemPropEnvVar("lucee.monitoring.showMetric")
			.defaultValue(false);
	private Boolean showMetric;

	private static Prop<Boolean> metaShowTest = Prop.bool().keys("showTests", "showTest", "test").systemPropEnvVar("lucee.monitoring.showTest").defaultValue(false);
	private Boolean showTest;

	private static Prop<Boolean> metafullNullSupport = Prop.bool().keys("nullSupport", "fullNullSupport").systemPropEnvVar("lucee.full.null.support").defaultValue(false);
	private Boolean fullNullSupport;

	private static Prop<SecretProvider> metaSecretProviders = Prop.custom(SecretProviderFactory.getInstance(), Prop.TYPE_MAP).keys("secretProvider");
	protected Map<String, SecretProvider> secretProviders;

	private static Prop<ClassDefinition> metacCacheDefinitions = Prop.custom(ClassDefinitionFactory.getInstance(), Prop.TYPE_LIST).keys("cacheClasses").deprecated();
	private Map<String, ClassDefinition> cacheDefinitions;

	private static Prop<GatewayEntry> metaGatewayEntries = Prop.custom(GatewayEntryFactory.getInstance(), Prop.TYPE_MAP).keys("gateways").access(SecurityManagerImpl.TYPE_GATEWAY)
			.description(
					"Defines Event Gateways for asynchronous communication (SMS, XMPP, File Watcher, etc.). Configures the driver class, listener CFC, and custom protocol parameters.");
	private GatewayMap gatewayEntries;

	private static Prop<Boolean> metaCgiScopeReadonly = Prop.bool().keys("cgiScopeReadOnly").defaultValue(true).description("make the cgi scope read only or not");
	private Boolean cgiScopeReadonly;

	private static Prop<Integer> metaDebugMaxRecordsLogged = Prop.integer().keys("debuggingMaxRecordsLogged", "debuggingShowMaxRecordsLogged").defaultValue(10);
	private Integer debugMaxRecordsLogged;

	private static Prop<Boolean> metaCheckForChangesInConfigFile = Prop.bool().keys("checkForChanges").systemPropEnvVar("lucee.check.for.changes").defaultValue(false)
			.description("Enables automatic 'Hot-Reload' for the Lucee configuration file. "
					+ "When set to 'true', the engine polls the 'lucee-config.json' file (approximately every 60 seconds) "
					+ "and automatically applies any detected changes to the running server without requiring a restart.");
	private Boolean checkForChangesInConfigFile;

	private static Prop<String> metaRemoteClientDirectory = Prop.str().keys("directory").parent("remoteClients").systemPropEnvVar("lucee.task.directory")
			.description("Specifies the file-system directory where Lucee stores persistent background tasks (e.g., cfmail, cfthread tasks). "
					+ "By storing tasks as physical files, Lucee prevents memory exhaustion during high-volume mail bursts and ensures that "
					+ "failed or pending tasks are preserved across server restarts.");
	private Resource remoteClientDirectory;

	private static Prop<Integer> metaRemoteClientMaxThreads = Prop.integer().keys("maxThreads").parent("remoteClients").defaultValue(20)
			.description("Sets the maximum number of concurrent worker threads for the Lucee Spooler. "
					+ "The spooler handles asynchronous tasks such as background mail delivery (cfmail) and thread tasks (cfthread type='task'). "
					+ "Increasing this value allows for faster parallel processing of background queues, but consumes more system resources.");
	private Integer remoteClientMaxThreads;

	private static Prop<RemoteClient> metaRemoteClients = Prop.custom(RemoteClientFactory.getInstance(), Prop.TYPE_LIST).keys("remoteClient").parent("remoteClients")
			.access(SecurityManagerImpl.TYPE_REMOTE).deprecated();
	private RemoteClient[] remoteClients;

	private Array scheduledTasks;

	private SchedulerImpl scheduler;

	private final Resources resources = new ResourcesImpl();
	private boolean initResource = true;
	private Map<String, Class<CacheHandler>> cacheHandlerClasses;

	private ResourceProvider defaultResourceProvider;

	private List<ExtensionDefintion> extensionsDefs;
	private RHExtension[] rhextensions = RHEXTENSIONS_EMPTY;

	private DumpWriterEntry[] dmpWriterEntries;

	private Struct remoteClientUsage;
	private Class adminSyncClass;
	protected Mapping defaultFunctionMapping;
	protected final Map<String, Mapping> functionMappings = new ConcurrentHashMap<String, Mapping>();

	protected Mapping defaultTagMapping;
	protected final Map<String, Mapping> tagMappings = new ConcurrentHashMap<String, Mapping>();

	private final Map<String, ORMEngine> ormengines = new HashMap<String, ORMEngine>();
	private ClassDefinition<? extends ORMEngine> cdORMEngine;
	private ORMConfiguration ormConfig;
	private lucee.runtime.rest.Mapping[] restMappings;

	private List<Object> consoleLayouts = new ArrayList<>();
	private List<Object> resourceLayouts = new ArrayList<>();

	private Map<Key, Map<Key, Object>> tagDefaultAttributeValues;
	private Map<Integer, Object> cachedWithins;
	private static Map<String, Startup> startups;

	private JavaSettings javaSettings;
	private final Map<String, JavaSettings> javaSettingsInstances = new ConcurrentHashMap<>();
	private Resource extInstalled;
	private Resource extAvailable;

	private static Prop<AIEngine> metaAiEngines = Prop.custom(AIEngineFactory.getInstance(), Prop.TYPE_MAP).keys("ai").description("");
	protected Map<String, AIEngine> aiEngines;

	private Resource antiSamyPolicy;

	/**
	 * @return the allowURLRequestTimeout
	 */
	@Override
	public boolean isAllowURLRequestTimeout() {
		if (allowURLRequestTimeout == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "isAllowURLRequestTimeout")) {
				if (allowURLRequestTimeout == null) {
					String allowURLReqTimeout = ConfigFactoryImpl.getAttr(this, root, new String[] { "requestTimeoutInURL", "allowUrlRequesttimeout" });
					if (!StringUtil.isEmpty(allowURLReqTimeout)) {
						allowURLRequestTimeout = Caster.toBooleanValue(allowURLReqTimeout, metaAllowURLRequestTimeout.defaultValue);
					}
					else allowURLRequestTimeout = metaAllowURLRequestTimeout.defaultValue;
				}
			}
		}
		return allowURLRequestTimeout;
	}

	public ConfigImpl resetAllowURLRequestTimeout() {
		if (allowURLRequestTimeout != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "isAllowURLRequestTimeout")) {
				if (allowURLRequestTimeout != null) {
					allowURLRequestTimeout = null;
				}
			}
		}
		return this;
	}

	@Override
	public short getCompileType() {
		if (compileType == -1) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCompileType")) {
				if (compileType == -1) {
					compileType = metaCompileType.get(this, root);
				}
			}
		}
		return compileType;
	}

	public ConfigImpl resetCompileType() {
		if (compileType != -1) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCompileType")) {
				if (compileType != -1) {
					compileType = -1;
				}
			}
		}
		return this;
	}

	@Override
	@Deprecated
	public void reloadTimeServerOffset() {
		// FUTURE remove method
	}

	/**
	 * private constructor called by factory method
	 * 
	 * @param configDir - config directory
	 * @param configFile - config file
	 */
	protected ConfigImpl(Resource configDir, Resource configFile, boolean newVersion) {
		this.configDir = configDir;
		this.configFile = configFile;
		this.newVersion = newVersion;
	}

	@Override
	public long lastModified() {
		return configFileLastModified;
	}

	@Override
	public void setLastModified() {
		this.configFileLastModified = configFile.lastModified();
	}

	@Override
	public short getScopeCascadingType() {
		if (scopeType == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getScopeCascadingType")) {
				if (scopeType == null) {
					scopeType = metaScopeType.get(this, root);
				}
			}
		}
		return scopeType;
	}

	public ConfigImpl resetScopeCascadingType() {
		if (scopeType != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getScopeCascadingType")) {
				if (scopeType != null) {
					scopeType = null;
				}
			}
		}

		return this;
	}

	/**
	 * return all Tag Library Deskriptors
	 * 
	 * @return Array of Tag Library Deskriptors
	 */
	@Override
	public TagLib[] getTLDs() {
		// TODO
		if (cfmlTlds == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getTLDs")) {
				if (cfmlTlds == null) {
					ConfigFactoryImpl.loadTag(this, root, newVersion());
				}
			}
		}
		return cfmlTlds;
	}

	public ConfigImpl resetTLDs() {
		if (cfmlTlds != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getTLDs")) {
				if (cfmlTlds != null) {
					cfmlTlds = null;
					tldFile = null;
				}
			}
		}
		return this;
	}

	@Override
	public TagLib getCoreTagLib() {
		TagLib[] tlds = getTLDs();
		for (int i = 0; i < tlds.length; i++) {
			if (tlds[i].isCore()) return tlds[i];
		}
		throw new RuntimeException("no core taglib found"); // this should never happen
	}

	protected void setTLDs(TagLib[] tlds) {
		cfmlTlds = tlds;
	}

	@Override
	public boolean allowImplicidQueryCall() {
		if (allowImplicidQueryCall == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "allowImplicidQueryCall")) {
				if (allowImplicidQueryCall == null) {
					allowImplicidQueryCall = metaAllowImplicidQueryCall.get(this, root);
				}
			}
		}
		return allowImplicidQueryCall;
	}

	public ConfigImpl resetAllowImplicidQueryCall() {
		if (allowImplicidQueryCall != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "allowImplicidQueryCall")) {
				if (allowImplicidQueryCall != null) {
					allowImplicidQueryCall = null;
				}
			}
		}
		return this;
	}

	@Override
	public boolean limitEvaluation() {
		if (limitEvaluation == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "limitEvaluation")) {
				if (limitEvaluation == null) {
					limitEvaluation = metaLimitEvaluation.get(this, root);
				}
			}
		}
		return limitEvaluation;
	}

	public ConfigImpl resetLimitEvaluation() {
		if (limitEvaluation != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "limitEvaluation")) {
				if (limitEvaluation != null) {
					limitEvaluation = null;
				}
			}
		}
		return this;
	}

	@Override
	public boolean mergeFormAndURL() {
		if (mergeFormAndURL == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "mergeFormAndURL")) {
				if (mergeFormAndURL == null) {
					mergeFormAndURL = metaMergeFormAndURL.get(this, root);
				}
			}
		}
		return mergeFormAndURL;
	}

	public ConfigImpl resetMergeFormAndURL() {
		if (mergeFormAndURL != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "mergeFormAndURL")) {
				if (mergeFormAndURL != null) {
					mergeFormAndURL = null;
				}
			}
		}
		return this;
	}

	@Override
	public TimeSpan getApplicationTimeout() {
		if (applicationTimeout == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getApplicationTimeout")) {
				if (applicationTimeout == null) {

					applicationTimeout = metaApplicationTimeout.get(this, root);

				}
			}
		}
		return applicationTimeout;
	}

	public ConfigImpl resetApplicationTimeout() {
		if (applicationTimeout != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getApplicationTimeout")) {
				if (applicationTimeout != null) {
					applicationTimeout = null;
				}
			}
		}
		return this;
	}

	@Override
	public TimeSpan getSessionTimeout() {
		if (sessionTimeout == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getSessionTimeout")) {
				if (sessionTimeout == null) {
					sessionTimeout = metaSessionTimeout.get(this, root);

				}
			}
		}
		return sessionTimeout;
	}

	public ConfigImpl resetSessionTimeout() {
		if (sessionTimeout != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getSessionTimeout")) {
				if (sessionTimeout != null) {
					sessionTimeout = null;
				}
			}
		}
		return this;
	}

	@Override
	public TimeSpan getClientTimeout() {
		if (clientTimeout == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getClientTimeout")) {
				if (clientTimeout == null) {
					clientTimeout = metaClientTimeout.get(this, root);
				}
			}
		}
		return clientTimeout;
	}

	public ConfigImpl resetClientTimeout() {
		if (clientTimeout != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getClientTimeout")) {
				if (clientTimeout != null) {
					clientTimeout = null;
				}
			}
		}
		return this;
	}

	@Override
	public TimeSpan getRequestTimeout() {
		if (requestTimeout == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getRequestTimeout")) {
				if (requestTimeout == null) {
					requestTimeout = metaRequestTimeout.get(this, root);
				}
			}
		}
		return requestTimeout;
	}

	public ConfigImpl resetRequestTimeout() {
		if (requestTimeout != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getRequestTimeout")) {
				if (requestTimeout != null) {
					requestTimeout = null;
				}
			}
		}
		return this;
	}

	@Override
	public boolean isClientCookies() {
		if (clientCookies == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "isClientCookies")) {
				if (clientCookies == null) {
					clientCookies = metaClientCookies.get(this, root);
				}
			}
		}
		return clientCookies;
	}

	public ConfigImpl resetClientCookies() {
		if (clientCookies != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "isClientCookies")) {
				if (clientCookies != null) {
					clientCookies = null;
				}
			}
		}
		return this;
	}

	@Override
	public boolean isDevelopMode() {
		if (developMode == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "isDevelopMode")) {
				if (developMode == null) {
					developMode = metaDevelopMode.get(this, root);
				}
			}
		}
		return developMode;
	}

	public ConfigImpl resetDevelopMode() {
		if (developMode != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "isDevelopMode")) {
				if (developMode != null) {
					developMode = null;
				}
			}
		}
		return this;
	}

	@Override
	public boolean isClientManagement() {
		if (clientManagement == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "isClientManagement")) {
				if (clientManagement == null) {
					clientManagement = metaClientManagement.get(this, root);
				}
			}
		}
		return clientManagement;
	}

	public ConfigImpl resetClientManagement() {
		if (clientManagement != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "isClientManagement")) {
				if (clientManagement != null) {
					clientManagement = null;
				}
			}
		}
		return this;
	}

	@Override
	public boolean isDomainCookies() {
		if (domainCookies == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "isDomainCookies")) {
				if (domainCookies == null) {
					domainCookies = metaDomainCookies.get(this, root);
				}
			}
		}
		return domainCookies;
	}

	public ConfigImpl resetDomainCookies() {
		if (domainCookies != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "isDomainCookies")) {
				if (domainCookies != null) {
					domainCookies = null;
				}
			}
		}
		return this;
	}

	@Override
	public boolean isSessionManagement() {
		if (sessionManagement == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "isSessionManagement")) {
				if (sessionManagement == null) {
					sessionManagement = metaSessionManagement.get(this, root);
				}
			}
		}
		return sessionManagement;
	}

	public ConfigImpl resetSessionManagement() {
		if (sessionManagement != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "isSessionManagement")) {
				if (sessionManagement != null) {
					sessionManagement = null;
				}
			}
		}
		return this;
	}

	@Override
	public boolean isMailSpoolEnable() {
		// TODO
		if (spoolEnable == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "mail")) {
				if (spoolEnable == null) {
					spoolEnable = metaSpoolEnable.get(this, root);
				}
			}
		}
		return spoolEnable;
	}

	public ConfigImpl resetMailSpoolEnable() {
		if (spoolEnable != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "mail")) {
				if (spoolEnable != null) {
					spoolEnable = null;
				}
			}
		}
		return this;
	}

	@Override
	public boolean isMailSendPartial() {
		// TODO
		if (sendPartial == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "mail")) {
				if (sendPartial == null) {
					sendPartial = metaSendPartial.get(this, root);
				}
			}
		}
		return sendPartial;
	}

	public ConfigImpl resetMailSendPartial() {
		if (sendPartial != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "mail")) {
				if (sendPartial != null) {
					sendPartial = null;
				}
			}
		}
		return this;
	}

	@Override
	public boolean isUserset() {
		// TODO
		if (userSet == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "mail")) {
				if (userSet == null) {
					userSet = metaUserSet.get(this, root);
				}
			}
		}
		return userSet;
	}

	public ConfigImpl resetUserset() {
		if (userSet != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "mail")) {
				if (userSet != null) {
					userSet = null;
				}
			}
		}
		return this;
	}

	@Override
	public Server[] getMailServers() {
		// TODO
		if (mailServers == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "mail")) {
				if (mailServers == null) {
					List<Server> list = metaMailServers.list(this, root);
					mailServers = list.toArray(new Server[list.size()]);
				}
			}
		}
		return mailServers;
	}

	public ConfigImpl resetMailServers() {
		if (mailServers != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "mail")) {
				if (mailServers != null) {
					mailServers = null;
				}
			}
		}
		return this;
	}

	@Override
	public int getMailTimeout() {
		// TODO
		if (mailTimeout == -1) {
			synchronized (SystemUtil.createToken("ConfigImpl", "mail")) {
				if (mailTimeout == -1) {
					mailTimeout = metaMailTimeout.get(this, root);
				}
			}
		}
		return mailTimeout;
	}

	public ConfigImpl resetMailTimeout() {
		if (mailTimeout != -1) {
			synchronized (SystemUtil.createToken("ConfigImpl", "mail")) {
				if (mailTimeout != -1) {
					mailTimeout = -1;
				}
			}
		}
		return this;
	}

	@Override
	public int getQueryVarUsage() {
		// TODO
		if (varUsage == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getQueryVarUsage")) {
				if (varUsage == null) {
					varUsage = metaVarUsage.get(this, root);
				}
			}
		}
		return varUsage;
	}

	public ConfigImpl resetQueryVarUsage() {
		if (varUsage != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getQueryVarUsage")) {
				if (varUsage != null) {
					varUsage = null;
				}
			}
		}
		return this;
	}

	@Override
	public boolean getPSQL() {
		if (psq == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getPSQL")) {
				if (psq == null) {
					psq = metaPsq.get(this, root);
				}
			}
		}
		return psq;
	}

	public ConfigImpl resetPSQL() {
		if (psq != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getPSQL")) {
				if (psq != null) {
					psq = null;
				}
			}
		}
		return this;
	}

	@Override
	public ClassLoader getClassLoader() {
		ClassLoader cl = null;
		try {
			cl = getRPCClassLoader(false);
		}
		catch (IOException e) {}
		if (cl != null) return cl;
		return SystemUtil.getCoreClassLoader();

	}

	// do not remove, ised in Hibernate extension
	@Override
	public ClassLoader getClassLoaderEnv() {
		if (envClassLoader == null) envClassLoader = new EnvClassLoader(this);
		return envClassLoader;
	}

	@Override
	public ClassLoader getClassLoaderCore() {
		return new lucee.commons.lang.ClassLoaderHelper().getClass().getClassLoader();
	}

	@Override
	public ClassLoader getClassLoaderLoader() {
		return new TP().getClass().getClassLoader();
	}

	@Override
	public Locale getLocale() {
		if (locale == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getLocale")) {
				if (locale == null) {
					locale = metaLocale.get(this, root);
				}
			}
		}
		return locale;
	}

	public ConfigImpl resetLocale() {
		if (locale != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getLocale")) {
				if (locale != null) {
					locale = null;
				}
			}
		}
		return this;
	}

	@Override
	public boolean debug() {
		return true;
	}

	@Override
	public boolean getShowDebug() {
		if (showDebug == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getShowDebug")) {
				if (showDebug == null) {
					showDebug = metaShowDebug.get(this, root);
				}
			}
		}
		return showDebug;
	}

	public ConfigImpl resetShowDebug() {
		if (showDebug != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getShowDebug")) {
				if (showDebug != null) {
					showDebug = null;
				}
			}
		}
		return this;
	}

	@Override
	public boolean getShowDoc() {
		if (showDoc == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getShowDoc")) {
				if (showDoc == null) {
					showDoc = metaShowDoc.get(this, root);
				}
			}
		}
		return showDoc;
	}

	public ConfigImpl resetShowDoc() {
		if (showDoc != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getShowDoc")) {
				if (showDoc != null) {
					showDoc = null;
				}
			}
		}
		return this;
	}

	@Override
	public boolean getShowMetric() {
		if (showMetric == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getShowMetric")) {
				if (showMetric == null) {
					showMetric = metaShowMetric.get(this, root);
				}
			}
		}
		return this.showMetric;
	}

	public ConfigImpl resetShowMetric() {
		if (showMetric != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getShowMetric")) {
				if (showMetric != null) {
					showMetric = null;
				}
			}
		}
		return this;
	}

	@Override
	public boolean getShowTest() {
		if (showTest == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getShowTest")) {
				if (showTest == null) {
					showTest = metaShowTest.get(this, root);
				}
			}
		}
		return this.showTest;
	}

	public ConfigImpl resetShowTest() {
		if (showTest != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getShowTest")) {
				if (showTest != null) {
					showTest = null;
				}
			}
		}
		return this;
	}

	@Override
	public boolean debugLogOutput() {
		if (debugLogOutput == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "debugLogOutput")) {
				if (debugLogOutput == null) {
					debugLogOutput = metaDebugLogOutput.get(this, root);
				}
			}
		}
		return debugLogOutput;
	}

	public ConfigImpl resetLogOutput() {
		if (debugLogOutput != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "debugLogOutput")) {
				if (debugLogOutput != null) {
					debugLogOutput = null;
				}
			}
		}

		return this;
	}

	// = SERVER_BOOLEAN_FALSE

	@Override
	public int getMailSpoolInterval() {
		// TODO
		if (spoolInterval == -1) {
			synchronized (SystemUtil.createToken("ConfigImpl", "mail")) {
				if (spoolInterval == -1) {
					spoolInterval = metaSpoolInterval.get(this, root);
				}
			}
		}
		return spoolInterval;
	}

	public ConfigImpl resetMailSpoolInterval() {
		if (spoolInterval != -1) {
			synchronized (SystemUtil.createToken("ConfigImpl", "mail")) {
				if (spoolInterval != -1) {
					spoolInterval = -1;
				}
			}
		}
		return this;
	}

	@Override
	public TimeZone getTimeZone() {
		if (timeZone == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getTimeZone")) {
				if (timeZone == null) {
					timeZone = metaTimeZone.get(this, root);
				}
			}
		}
		return timeZone;
	}

	public ConfigImpl resetTimeZone() {
		if (timeZone != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getTimeZone")) {
				if (timeZone != null) {
					timeZone = null;
				}
			}
		}
		return this;
	}

	@Override
	@Deprecated
	public long getTimeServerOffset() {
		return timeOffset;
	}

	/**
	 * @return return the Scheduler
	 */
	@Override
	public Scheduler getScheduler() {
		// TODO
		// MUST reset scheduler
		if (scheduler == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getScheduler")) {
				if (scheduler == null) {
					try {
						scheduler = new SchedulerImpl(ConfigUtil.getEngine(this), this, getScheduledTasks());
					}
					catch (PageException e) {
						try {
							scheduler = new SchedulerImpl(ConfigUtil.getEngine(this), this, new ArrayImpl());
						}
						catch (PageException e1) {}
					}
				}
			}
		}
		return scheduler;
	}

	public ConfigImpl resetScheduler() {
		if (scheduler != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getScheduler")) {
				if (scheduler != null) {
					try {
						scheduler.refresh(getScheduledTasks());
					}
					catch (Exception e) {
						SchedulerImpl tmp = scheduler;
						scheduler = null;
						tmp.stop();
					}

				}
			}
		}
		return this;
	}

	/**
	 * @return gets the password as hash
	 */
	protected Password getPassword() {
		// TODO
		if (initPassword) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getPassword")) {
				if (initPassword) {
					Password pw = metaPassword.get(this, root);
					initPassword = false;
				}
			}
		}
		return password;
	}

	protected ConfigImpl resetPassword() {
		if (!initPassword) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getPassword")) {
				if (!initPassword) {
					initPassword = true;
					password = null;
				}
			}
		}
		return this;
	}

	@Override
	public Password isPasswordEqual(String password) {
		if (getPassword() == null) return null;
		return ((PasswordImpl) getPassword()).isEqual(this, password);
	}

	@Override
	public boolean hasPassword() {
		return getPassword() != null;
	}

	@Override
	public boolean passwordEqual(Password password) {
		if (getPassword() == null) return false;
		return getPassword().equals(password);
	}

	@Override
	public Mapping[] getMappings() {
		// TODO
		if (mappings == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getMappings")) {
				if (mappings == null) {
					close(this.uncheckedMappings);

					// check for specific mappings to exist
					Map<String, Mapping> tmpMappings = metaMappings.map(this, root);
					boolean finished = true;
					boolean hasServerContext = false; // TODO still needed?
					boolean hasWebContext = false;
					String virtual;
					for (Entry<String, Mapping> entry: tmpMappings.entrySet()) {
						virtual = entry.getKey();
						if ("/lucee-server/".equalsIgnoreCase(virtual) || "/lucee-server-context/".equalsIgnoreCase(virtual)) {
							hasServerContext = true;
						}
						else if ("/lucee/".equalsIgnoreCase(virtual)) {
							hasWebContext = true;
						}
						else if ("/".equals(virtual)) {
							finished = true;
						}
					}

					// set default lucee-server context if needed TODO still neded?
					if (!hasServerContext) {
						ApplicationListener listener = ConfigUtil.loadListener(ApplicationListener.TYPE_MODERN, null);
						listener.setMode(ApplicationListener.MODE_CURRENT2ROOT);

						MappingImpl tmp = new MappingImpl(this, "/lucee-server", "{lucee-server}/context/", null, ConfigPro.INSPECT_AUTO, ConfigPro.INSPECT_INTERVAL_UNDEFINED,
								ConfigPro.INSPECT_INTERVAL_UNDEFINED, true, false, true, true, false, false, listener, ApplicationListener.MODE_CURRENT2ROOT,
								ApplicationListener.TYPE_MODERN);
						tmpMappings.put(tmp.getVirtualLowerCase(), tmp);
					}
					// set default lucee context if needed
					if (!hasWebContext) {
						ApplicationListener listener = ConfigUtil.loadListener(ApplicationListener.TYPE_MODERN, null);
						listener.setMode(ApplicationListener.MODE_CURRENT2ROOT);

						MappingImpl tmp = new MappingImpl(this, "/lucee", "{lucee-config}/context/", "{lucee-config}/context/lucee-context.lar", ConfigPro.INSPECT_AUTO,
								ConfigPro.INSPECT_INTERVAL_UNDEFINED, ConfigPro.INSPECT_INTERVAL_UNDEFINED, true, false, true, true, false, false, listener,
								ApplicationListener.MODE_CURRENT2ROOT, ApplicationListener.TYPE_MODERN);
						tmpMappings.put(tmp.getVirtualLowerCase(), tmp);
					}
					// seet root mapping (always needed)
					if (!finished) {
						MappingImpl tmp = new MappingImpl(this, "/", "/", null, ConfigPro.INSPECT_UNDEFINED, ConfigPro.INSPECT_INTERVAL_UNDEFINED,
								ConfigPro.INSPECT_INTERVAL_UNDEFINED, true, true, true, true, false, false, null, -1, -1);
						tmpMappings.put("/", tmp);
					}

					this.mappings = initMappings(this.uncheckedMappings = tmpMappings.values().toArray(new Mapping[tmpMappings.size()]));
				}
			}
		}
		return mappings;
	}

	@Override
	public ConfigImpl resetMappings() {
		if (mappings != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getMappings")) {
				if (mappings != null) {
					ConfigFactoryImpl.flushPageSourcePool(mappings);
					close(this.uncheckedMappings);
					this.mappings = null;
					this.uncheckedMappings = null;
				}
			}
		}
		return this;
	}

	@Override
	public Mapping[] getCustomTagMappings() {
		if (customTagMappings == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCustomTagMappings")) {
				if (customTagMappings == null) {
					close(this.uncheckedCustomTagMappings);

					List<Mapping> list = metaCustomTagMappings.list(this, root);

					boolean hasDefault = false;
					for (Mapping m: list) {
						if ("{lucee-config}/customtags/".equals(m.getStrPhysical())) {
							hasDefault = true;
							break;
						}
					}
					if (!hasDefault) {
						list.add(new MappingImpl(this, "/default", "{lucee-config}/customtags/", null, ConfigPro.INSPECT_NEVER, -1, -1, true, false, true, true, false, true, null,
								-1, -1));
					}
					this.customTagMappings = initMappings(this.uncheckedCustomTagMappings = list.toArray(new Mapping[list.size()]));
				}
			}
		}
		return customTagMappings;
	}

	public ConfigImpl resetCustomTagMappings() {
		if (customTagMappings != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCustomTagMappings")) {
				if (customTagMappings != null) {
					ConfigFactoryImpl.flushPageSourcePool(customTagMappings);

					close(this.uncheckedCustomTagMappings);
					this.customTagMappings = null;
					this.uncheckedCustomTagMappings = null;
				}
			}
		}
		return this;
	}

	@Override
	public Mapping[] getComponentMappings() {
		if (componentMappings == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getComponentMappings")) {
				if (componentMappings == null) {
					close(this.uncheckedComponentMappings);

					List<Mapping> list = metaComponentMappings.list(this, root);

					boolean hasDefault = false;
					for (Mapping m: list) {
						if ("{lucee-config}/components/".equals(m.getStrPhysical())) {
							hasDefault = true;
							break;
						}
					}

					if (!hasDefault) {
						list.add(new MappingImpl(this, "/default", "{lucee-config}/components/", null, ConfigPro.INSPECT_NEVER, -1, -1, true, false, true, true, false, true, null,
								-1, -1));
					}
					this.componentMappings = initMappings(this.uncheckedComponentMappings = list.toArray(new Mapping[list.size()]));
				}
			}
		}
		return componentMappings;
	}

	public ConfigImpl resetComponentMappings() {
		if (componentMappings != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getComponentMappings")) {
				if (componentMappings != null) {
					ConfigFactoryImpl.flushPageSourcePool(componentMappings);
					close(this.uncheckedComponentMappings);
					this.componentMappings = null;
					this.uncheckedComponentMappings = null;
				}
			}
		}
		return this;
	}

	public Array getScheduledTasks() {
		if (scheduledTasks == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getScheduledTasks")) {
				if (scheduledTasks == null) {
					this.scheduledTasks = ConfigUtil.getAsArray("scheduledTasks", root);
					if (this.scheduledTasks == null) this.scheduledTasks = new ArrayImpl();
				}
			}
		}
		return scheduledTasks;
	}

	public ConfigImpl resetScheduledTasks() {
		if (scheduledTasks != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getScheduledTasks")) {
				if (scheduledTasks != null) {
					this.scheduledTasks = null;
				}
			}
		}
		return this;
	}

	public void checkMappings() {
		mappings = initMappings(uncheckedMappings);
		customTagMappings = initMappings(uncheckedCustomTagMappings);
		componentMappings = initMappings(uncheckedComponentMappings);
	}

	private Mapping[] initMappings(Mapping[] mappings) {
		if (mappings == null) return null;
		List<Mapping> list = new ArrayList<Mapping>();
		for (Mapping m: mappings) {
			try {
				m.check();
				list.add(m);
			}
			catch (Exception e) {
				LogUtil.log(this, "mappings", e);
			}
		}
		return list.toArray(new Mapping[list.size()]);
	}

	protected void close(Mapping[] mappings) {
		if (mappings != null) {
			for (Mapping m: mappings) {
				if (m instanceof MappingImpl) ((MappingImpl) m).close();
			}
		}
	}

	@Override
	public lucee.runtime.rest.Mapping[] getRestMappings() {
		if (restMappings == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getRestMappings")) {
				if (restMappings == null) {
					restMappings = ConfigFactoryImpl.loadRestMappings(this, root);
				}
			}
		}
		return restMappings;
	}

	@Override
	public ConfigImpl resetRestMappings() {
		if (restMappings != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getRestMappings")) {
				if (restMappings != null) {
					restMappings = null;
				}
			}
		}
		return this;
	}

	@Override
	public PageSource getPageSource(Mapping[] mappings, String realPath, boolean onlyTopLevel) {
		throw new PageRuntimeException(new DeprecatedException("method not supported"));
	}

	@Override
	public PageSource getPageSourceExisting(PageContext pc, Mapping[] mappings, String realPath, boolean onlyTopLevel, boolean useSpecialMappings, boolean useDefaultMapping,
			boolean onlyPhysicalExisting) {
		return ConfigUtil.getPageSourceExisting(pc, this, mappings, realPath, onlyTopLevel, useSpecialMappings, useDefaultMapping, onlyPhysicalExisting);
	}

	@Override
	public PageSource[] getPageSources(PageContext pc, Mapping[] mappings, String realPath, boolean onlyTopLevel, boolean useSpecialMappings, boolean useDefaultMapping) {
		return getPageSources(pc, mappings, realPath, onlyTopLevel, useSpecialMappings, useDefaultMapping, false, onlyFirstMatch);
	}

	@Override
	public PageSource[] getPageSources(PageContext pc, Mapping[] mappings, String realPath, boolean onlyTopLevel, boolean useSpecialMappings, boolean useDefaultMapping,
			boolean useComponentMappings) {
		return getPageSources(pc, mappings, realPath, onlyTopLevel, useSpecialMappings, useDefaultMapping, useComponentMappings, onlyFirstMatch);
	}

	public PageSource[] getPageSources(PageContext pc, Mapping[] appMappings, String realPath, boolean onlyTopLevel, boolean useSpecialMappings, boolean useDefaultMapping,
			boolean useComponentMappings, boolean onlyFirstMatch) {
		return ConfigUtil.getPageSources(pc, this, appMappings, realPath, onlyTopLevel, useSpecialMappings, useDefaultMapping, useComponentMappings, onlyFirstMatch);
	}

	@Override
	public Resource[] getResources(PageContext pc, Mapping[] mappings, String realPath, boolean onlyTopLevel, boolean useSpecialMappings, boolean useDefaultMapping,
			boolean useComponentMappings, boolean onlyFirstMatch) {
		return ConfigUtil.getResources(pc, this, mappings, realPath, onlyTopLevel, useSpecialMappings, useDefaultMapping, useComponentMappings, onlyFirstMatch);
	}

	@Override
	public PageSource toPageSource(Mapping[] mappings, Resource res, PageSource defaultValue) {
		return ConfigUtil.toPageSource(this, mappings, res, defaultValue);
	}

	@Override
	public Resource getConfigDir() {
		return configDir;
	}

	@Override
	public Resource getConfigFile() {
		return configFile;
	}

	/**
	 * sets the password
	 * 
	 * @param password
	 */
	@Override
	public void setPassword(Password password) {
		this.password = password;
	}

	/**
	 * set the optional directory of the tag library deskriptors
	 * 
	 * @param fileTld directory of the tag libray deskriptors
	 * @throws TagLibException
	 */
	protected void setTldFile(Resource fileTld) throws TagLibException {
		TagLib[] tlds = cfmlTlds;

		if (fileTld == null) return;
		this.tldFile = fileTld;
		String key;
		Map<String, TagLib> map = new HashMap<String, TagLib>();
		// First fill existing to set
		for (int i = 0; i < tlds.length; i++) {
			key = getKey(tlds[i]);
			map.put(key, tlds[i]);
		}

		TagLib tl;

		// now overwrite with new data
		if (fileTld.isDirectory()) {
			Resource[] files = fileTld.listResources(new ExtensionResourceFilter(new String[] { "tld", "tldx" }));
			for (int i = 0; i < files.length; i++) {
				try {
					tl = TagLibFactory.loadFromFile(files[i], getIdentification());
					key = getKey(tl);
					if (!map.containsKey(key)) map.put(key, tl);
					else overwrite(map.get(key), tl);
				}
				catch (TagLibException tle) {
					LogUtil.log(this, Log.LEVEL_ERROR, "loading", "can't load tld " + files[i]);
					tle.printStackTrace(getErrWriter());
				}

			}
		}
		else if (fileTld.isFile()) {
			tl = TagLibFactory.loadFromFile(fileTld, getIdentification());
			key = getKey(tl);
			if (!map.containsKey(key)) map.put(key, tl);
			else overwrite(map.get(key), tl);
		}

		// now fill back to array
		tlds = new TagLib[map.size()];
		cfmlTlds = tlds;

		int index = 0;
		Iterator<TagLib> it = map.values().iterator();
		while (it.hasNext()) {
			tlds[index++] = it.next();
		}
	}

	protected void setTagDirectory(List<Path> listTagDirectory) {
		Iterator<Path> it = listTagDirectory.iterator();
		int index = -1;
		String mappingName;
		Path path;
		Mapping m;
		boolean isDefault;
		while (it.hasNext()) {
			path = it.next();
			index++;
			isDefault = index == 0;
			mappingName = "/mapping-tag" + (isDefault ? "" : index) + "";

			m = new MappingImpl(this, mappingName, path.isValidDirectory() ? path.res.getAbsolutePath() : path.str, null, ConfigPro.INSPECT_AUTO, 60000, 1000, true, true, true,
					true, false, true, null, -1, -1);
			if (isDefault) defaultTagMapping = m;
			tagMappings.put(mappingName, m);

			TagLib tlc = getCoreTagLib();

			// now overwrite with new data
			if (path.res.isDirectory()) {
				String[] files = path.res.list(new ExtensionResourceFilter(getMode() == ConfigPro.MODE_STRICT ? Constants.getComponentExtensions() : Constants.getExtensions()));
				for (int i = 0; i < files.length; i++) {
					if (tlc != null) createTag(tlc, files[i], mappingName);
				}
			}
		}
	}

	public void createTag(TagLib tl, String filename, String mappingName) {// Jira 1298
		String name = toName(filename);// filename.substring(0,filename.length()-(getCFCExtension().length()+1));

		TagLibTag tlt = new TagLibTag(tl);
		tlt.setName(name);
		tlt.setTagClassDefinition("lucee.runtime.tag.CFTagCore", getIdentification(), null);
		tlt.setHandleExceptions(true);
		tlt.setBodyContent("free");
		tlt.setParseBody(false);
		tlt.setDescription("");
		tlt.setAttributeType(TagLibTag.ATTRIBUTE_TYPE_MIXED);

		// read component and read setting from that component
		TagLibTagScript tlts = new TagLibTagScript(tlt);
		tlts.setType(TagLibTagScript.TYPE_MULTIPLE);
		tlt.setScript(tlts);

		TagLibTagAttr tlta = new TagLibTagAttr(tlt);
		tlta.setName("__filename");
		tlta.setRequired(true);
		tlta.setRtexpr(true);
		tlta.setType("string");
		tlta.setHidden(true);
		tlta.setDefaultValue(filename);
		tlt.setAttribute(tlta);

		tlta = new TagLibTagAttr(tlt);
		tlta.setName("__name");
		tlta.setRequired(true);
		tlta.setRtexpr(true);
		tlta.setHidden(true);
		tlta.setType("string");
		tlta.setDefaultValue(name);
		tlt.setAttribute(tlta);

		tlta = new TagLibTagAttr(tlt);
		tlta.setName("__isweb");
		tlta.setRequired(true);
		tlta.setRtexpr(true);
		tlta.setHidden(true);
		tlta.setType("boolean");
		tlta.setDefaultValue(this instanceof ConfigWeb ? "true" : "false");
		tlt.setAttribute(tlta);

		tlta = new TagLibTagAttr(tlt);
		tlta.setName("__mapping");
		tlta.setRequired(true);
		tlta.setRtexpr(true);
		tlta.setHidden(true);
		tlta.setType("string");
		tlta.setDefaultValue(mappingName);
		tlt.setAttribute(tlta);

		tl.setTag(tlt);
	}

	protected void setFunctionDirectory(List<Path> listFunctionDirectory) {
		Iterator<Path> it = listFunctionDirectory.iterator();
		int index = -1;
		String mappingName;
		Path path;
		boolean isDefault;
		while (it.hasNext()) {
			path = it.next();
			index++;
			isDefault = index == 0;
			mappingName = "/mapping-function" + (isDefault ? "" : index) + "";
			MappingImpl mapping = new MappingImpl(this, mappingName, (path.isValidDirectory() ? path.res.getAbsolutePath() : path.str), null, ConfigPro.INSPECT_AUTO, 60000, 1000,
					true, true, true, true, false, true, null, -1, -1);
			if (isDefault) defaultFunctionMapping = mapping;
			this.functionMappings.put(mappingName, mapping);

			// now overwrite with new data
			if (path.res != null && path.res.isDirectory()) {
				String[] files = path.res.list(new ExtensionResourceFilter(Constants.getTemplateExtensions()));

				for (String file: files) {
					if (cfmlFlds != null) createFunction(cfmlFlds, file, mappingName);

				}
			}
		}
	}

	public void createFunction(FunctionLib fl, String filename, String mapping) {
		String name = toName(filename);// filename.substring(0,filename.length()-(getCFMLExtensions().length()+1));
		FunctionLibFunction flf = new FunctionLibFunction(fl, true);
		flf.setArgType(FunctionLibFunction.ARG_DYNAMIC);
		flf.setFunctionClass("lucee.runtime.functions.system.CFFunction", null, null);
		flf.setName(name);
		flf.setReturn("object");

		FunctionLibFunctionArg arg = new FunctionLibFunctionArg(flf);
		arg.setName("__filename");
		arg.setRequired(true);
		arg.setType("string");
		arg.setHidden(true);
		arg.setDefaultValue(filename);
		flf.setArg(arg);

		arg = new FunctionLibFunctionArg(flf);
		arg.setName("__name");
		arg.setRequired(true);
		arg.setHidden(true);
		arg.setType("string");
		arg.setDefaultValue(name);
		flf.setArg(arg);

		arg = new FunctionLibFunctionArg(flf);
		arg.setName("__isweb");
		arg.setRequired(true);
		arg.setHidden(true);
		arg.setType("boolean");
		arg.setDefaultValue(this instanceof ConfigWeb ? "true" : "false");
		flf.setArg(arg);

		arg = new FunctionLibFunctionArg(flf);
		arg.setName("__mapping");
		arg.setRequired(true);
		arg.setHidden(true);
		arg.setType("string");
		arg.setDefaultValue(mapping);
		flf.setArg(arg);

		fl.setFunction(flf);
	}

	private static String toName(String filename) {
		int pos = filename.lastIndexOf('.');
		if (pos == -1) return filename;
		return filename.substring(0, pos);
	}

	private void overwrite(TagLib existingTL, TagLib newTL) {
		Iterator<TagLibTag> it = newTL.getTags().values().iterator();
		while (it.hasNext()) {
			existingTL.setTag(it.next());
		}
	}

	private String getKey(TagLib tl) {
		return tl.getNameSpaceAndSeparator().toLowerCase();
	}

	protected void setFLDs(FunctionLib flds) {
		cfmlFlds = flds;
	}

	/**
	 * return all Function Library Deskriptors
	 * 
	 * @return Array of Function Library Deskriptors
	 */
	@Override
	public FunctionLib getFLDs() {
		if (cfmlFlds == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getFLDs")) {
				if (cfmlFlds == null) {
					// TODO make some kind of pre state in case root is empty
					ConfigFactoryImpl.loadFunctions(this, root, newVersion());
				}
			}
		}
		return cfmlFlds;
	}

	public ConfigImpl resetFLDs() {
		if (cfmlFlds != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getFLDs")) {
				if (cfmlFlds != null) {
					cfmlFlds = null;
					fldFile = null;
				}
			}
		}
		return this;
	}

	@Override
	@Deprecated
	public FunctionLib[] getFLDs(int dialect) { // used in the image extension
		return new FunctionLib[] { getFLDs() };
	}

	protected void setFldFile(Resource fileFld) throws FunctionLibException {
		if (fileFld == null) return;
		this.fldFile = fileFld;

		// overwrite with additional functions
		FunctionLib fl;
		if (fileFld.isDirectory()) {
			Resource[] files = fileFld.listResources(new ExtensionResourceFilter(new String[] { "fld", "fldx" }));
			for (int i = 0; i < files.length; i++) {
				try {
					fl = FunctionLibFactory.loadFromFile(files[i], getIdentification());

					overwrite(cfmlFlds, fl);

				}
				catch (FunctionLibException fle) {
					LogUtil.log(this, Log.LEVEL_ERROR, "loading", "can't load fld " + files[i]);
					fle.printStackTrace(getErrWriter());
				}
			}
		}
		else {
			fl = FunctionLibFactory.loadFromFile(fileFld, getIdentification());
			overwrite(cfmlFlds, fl);
		}
	}

	@Override
	public Resource getFldFile() {
		getFLDs(); // will trigger the load of this variable

		return fldFile;
	}

	private void overwrite(FunctionLib existingFL, FunctionLib newFL) {
		Iterator<FunctionLibFunction> it = newFL.getFunctions().values().iterator();
		while (it.hasNext()) {
			existingFL.setFunction(it.next());
		}
	}

	@Override
	public Resource getTempDirectory() {
		if (tempDirectory == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getTempDirectory")) {
				if (tempDirectory == null) {
					try {

						String strTempDirectory = ConfigUtil.translateOldPath(metaTempDirectory.get(this, root));

						Resource configDir = getConfigDir();

						Resource cst = null;
						// Temp Dir
						if (!StringUtil.isEmpty(strTempDirectory)) {
							cst = ConfigUtil.getFile(configDir, strTempDirectory, null, configDir, FileUtil.TYPE_DIR, ResourceUtil.LEVEL_GRAND_PARENT_FILE, this);
						}
						if (cst == null) {
							cst = ConfigUtil.getFile(configDir, "temp", null, configDir, FileUtil.TYPE_DIR, ResourceUtil.LEVEL_GRAND_PARENT_FILE, this);
						}

						if (!isDirectory(cst) || !cst.isWriteable()) {
							LogUtil.log(this, Log.LEVEL_ERROR, "loading",
									"temp directory [" + cst + "] is not writable or can not be created, using directory [" + SystemUtil.getTempDirectory() + "] instead");

							cst = SystemUtil.getTempDirectory();
							if (!cst.isWriteable()) {
								LogUtil.log(this, Log.LEVEL_ERROR, "loading", "temp directory [" + cst + "] is not writable");
							}
							if (!cst.exists()) cst.mkdirs();

						}
						if (!tempDirectoryReload) ResourceUtil.removeChildrenEL(cst, false);// start with an empty temp directory
						this.tempDirectory = cst;

					}
					catch (Throwable t) {
						ExceptionUtil.rethrowIfNecessary(t);
						ConfigFactoryImpl.log(this, t);
					}
				}
			}
		}
		return tempDirectory;
	}

	public ConfigImpl resetTempDirectory() {
		if (tempDirectory != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getTempDirectory")) {
				if (tempDirectory != null) {
					tempDirectory = null;
					tempDirectoryReload = true;
				}
			}
		}
		return this;
	}

	/**
	 * sets the Schedule Directory
	 * 
	 * @param scheduleDirectory sets the schedule Directory
	 * @param logger
	 * @throws PageException
	 */

	@Override
	public Collection<String> getAIEngineNames() {
		return getAIEngines().keySet();
	}

	@Override
	public AIEngine getAIEngine(String name) {
		return getAIEngines().get(name);
	}

	private Map<String, AIEngine> getAIEngines() {
		if (aiEngines == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getAIEngineFactories")) {
				if (aiEngines == null) {
					aiEngines = metaAiEngines.map(this, root);
				}
			}
		}
		return aiEngines;
	}

	public ConfigImpl resetAIEngineFactories() {
		if (aiEngines != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getAIEngineFactories")) {
				if (aiEngines != null) {
					aiEngines = null;
				}
			}
		}
		return this;
	}

	@Override
	public SecretProvider getSecretProvider(String name) throws ApplicationException {
		SecretProvider sp = getSecretProviders().get(name.toLowerCase().trim());
		if (sp != null) return sp;
		throw new ApplicationException("there is no secret provider for the name [" + name + "]");
	}

	@Override
	public Map<String, SecretProvider> getSecretProviders() {
		if (secretProviders == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getSecretProviders")) {
				if (secretProviders == null) {
					secretProviders = metaSecretProviders.map(this, root);
					// secretProviders = ConfigFactoryImpl.loadSecretProviders(this, root, null);
				}
			}
		}
		return secretProviders;
	}

	public ConfigImpl resetSecretProviders() {
		if (secretProviders != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getSecretProviders")) {
				if (secretProviders != null) {
					secretProviders = null;
				}
			}
		}
		return this;
	}

	/**
	 * is file a directory or not, touch if not exist
	 * 
	 * @param directory
	 * @return true if existing directory or has created new one
	 */
	protected boolean isDirectory(Resource directory) {
		if (directory.exists()) return directory.isDirectory();
		try {
			directory.createDirectory(true);
			return true;
		}
		catch (IOException e) {
			e.printStackTrace(getErrWriter());
		}
		return false;
	}

	@Override
	public long getLoadTime() {
		return loadTime;
	}

	/**
	 * @param loadTime The loadTime to set.
	 */
	protected void setLoadTime(long loadTime) {
		this.loadTime = loadTime;
	}

	/**
	 * @return Returns the configLogger. / public Log getConfigLogger() { return configLogger; }
	 */

	@Override
	public CFXTagPool getCFXTagPool() throws SecurityException {
		if (cfxTagPool == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCFXTagPool")) {
				if (cfxTagPool == null) {
					cfxTagPool = new CFXTagPoolImpl(metaCfxTagPool.map(this, root));
				}
			}
		}
		return cfxTagPool;
	}

	public ConfigImpl resetCFXTagPool() {
		if (cfxTagPool != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCFXTagPool")) {
				if (cfxTagPool != null) {
					cfxTagPool = null;
				}
			}
		}
		return this;
	}

	@Override
	public PageSource getBaseComponentPageSource(PageContext pc, boolean force) {
		PageSource base = force ? null : baseComponentPageSource;

		if (base == null) {
			synchronized (SystemUtil.createToken("dialect", "")) {
				base = force ? null : baseComponentPageSource;
				if (base == null) {

					// package
					ImportDefintion di = getComponentDefaultImport();
					String pack = di == null ? null : di.getPackageAsPath();
					if (StringUtil.isEmpty(pack, true)) pack = "";
					else if (!pack.endsWith("/")) pack += "";
					// name
					String componentName = getBaseComponentTemplate();

					Mapping[] mappigs = getComponentMappings();
					if (!ArrayUtil.isEmpty(mappigs)) {
						PageSource ps;
						outer: do {
							for (Mapping m: mappigs) {
								ps = m.getPageSource(pack + componentName);
								if (ps.exists()) {
									base = ps;
									break outer;
								}
							}
							for (Mapping m: mappigs) {
								ps = m.getPageSource(componentName);
								if (ps.exists()) {
									base = ps;
									break outer;
								}
							}
							for (Mapping m: mappigs) {
								ps = m.getPageSource("org/lucee/cfml/" + componentName);
								if (ps.exists()) {
									base = ps;
									break outer;
								}
							}
						}
						while (false);
					}
					if (base == null) {
						StringBuilder detail;
						if (ArrayUtil.isEmpty(mappigs)) {
							detail = new StringBuilder("There are no components mappings available!");
						}
						else {
							detail = new StringBuilder();
							for (Mapping m: mappigs) {
								if (detail.length() > 0) detail.append(", ");
								else detail.append("The following component mappings are available [");

								Resource p = m.getPhysical();
								String physical = m.getStrPhysical();
								if (p != null) {
									try {
										physical = p.getCanonicalPath() + " (" + m.getStrPhysical() + ")";
									}
									catch (IOException e) {}
								}

								Resource a = m.getArchive();
								String archive = m.getStrArchive();
								if (p != null) {
									try {
										archive = a.getCanonicalPath() + " (" + m.getStrArchive() + ")";
									}
									catch (IOException e) {}
								}

								detail.append(physical).append(':').append(archive);
							}
							detail.append("]");
						}
						LogUtil.log(Log.LEVEL_ERROR, "component",
								"could not load the base component Component, it was not found in any of the component mappings." + detail.toString());

					}
					else {
						this.baseComponentPageSource = base;
					}
				}
			}
		}
		return base;
	}

	@Override
	public String getBaseComponentTemplate() {
		return baseComponentTemplate;
	}

	@Override
	public boolean getRestList() {
		if (restList == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getRestList")) {
				if (restList == null) {
					restList = metaRestList.get(this, root);
				}
			}
		}
		return restList;
	}

	public ConfigImpl resetRestList() {
		if (restList != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getRestList")) {
				if (restList != null) {
					restList = null;
				}
			}
		}
		return this;
	}

	/**
	 * @param clientType
	 */
	protected void setClientType(short clientType) {
		this.clientType = clientType;
	}

	/**
	 * @param strClientType
	 */
	protected void setClientType(String strClientType) {
		strClientType = strClientType.trim().toLowerCase();
		if (strClientType.equals("file")) clientType = Config.CLIENT_SCOPE_TYPE_FILE;
		else if (strClientType.equals("db")) clientType = Config.CLIENT_SCOPE_TYPE_DB;
		else if (strClientType.equals("database")) clientType = Config.CLIENT_SCOPE_TYPE_DB;
		else clientType = Config.CLIENT_SCOPE_TYPE_COOKIE;
	}

	@Override
	public short getClientType() {
		if (clientType == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getClientType")) {
				if (clientType == null) {
					clientType = metaClientType.get(this, root);
				}
			}
		}
		return this.clientType;
	}

	public ConfigImpl resetClientType() {
		if (clientType != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getClientType")) {
				if (clientType != null) {
					clientType = null;
				}

			}
		}
		return this;
	}

	@Override
	public ClassDefinition<SearchEngine> getSearchEngineClassDefinition() {
		if (searchEngineClassDef == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getSearchEngineClassDefinition")) {
				if (searchEngineClassDef == null) {
					searchEngineClassDef = ConfigFactoryImpl.loadSearchClass(this, root);
				}
			}
		}
		return this.searchEngineClassDef;
	}

	public ConfigImpl resetSearchEngineClassDefinition() {
		if (searchEngineClassDef != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getSearchEngineClassDefinition")) {
				if (searchEngineClassDef != null) {
					searchEngineClassDef = null;
				}
			}
		}
		return this;
	}

	@Override
	public String getSearchEngineDirectory() {
		if (searchEngineDirectory == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getSearchEngineDirectory")) {
				if (searchEngineDirectory == null) {
					searchEngineDirectory = metaSearchEngineDirectory.get(this, root);
				}
			}
		}
		return this.searchEngineDirectory;
	}

	public ConfigImpl resetSearchEngineDirectory() {
		if (searchEngineDirectory != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getSearchEngineDirectory")) {
				if (searchEngineDirectory != null) {
					searchEngineDirectory = null;
				}
			}
		}
		return this;
	}

	@Override
	public int getComponentDataMemberDefaultAccess() {
		if (componentDataMemberDefaultAccess == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getComponentDataMemberDefaultAccess")) {
				if (componentDataMemberDefaultAccess == null) {
					componentDataMemberDefaultAccess = metaComponentDataMemberDefaultAccess.get(this, root);
				}
			}
		}
		return componentDataMemberDefaultAccess;
	}

	public ConfigImpl resetComponentDataMemberDefaultAccess() {
		if (componentDataMemberDefaultAccess != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getComponentDataMemberDefaultAccess")) {
				if (componentDataMemberDefaultAccess != null) {
					componentDataMemberDefaultAccess = null;
				}
			}
		}
		return this;
	}
	// = Component.ACCESS_PUBLIC

	@Override
	@Deprecated
	public String getTimeServer() {
		return "";
	}

	@Override
	public String getComponentDumpTemplate() {
		if (componentDumpTemplate == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getComponentDumpTemplate")) {
				if (componentDumpTemplate == null) {
					componentDumpTemplate = metaComponentDumpTemplate.get(this, root);
				}
			}
		}
		return componentDumpTemplate;
	}

	public ConfigImpl resetComponentDumpTemplate() {
		if (componentDumpTemplate != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getComponentDumpTemplate")) {
				if (componentDumpTemplate != null) {
					componentDumpTemplate = null;
				}
			}
		}
		return this;
	}

	public String createSecurityToken() {
		try {
			return Md5.getDigestAsString(getConfigDir().getAbsolutePath());
		}
		catch (IOException e) {
			return null;
		}

	}

	@Override
	public String getDebugTemplate() {
		throw new PageRuntimeException(new DeprecatedException("no longer supported, use instead getDebugEntry(ip, defaultValue)"));
	}

	@Override
	public String getErrorTemplate(int statusCode) {

		if (statusCode == 404) {
			if (errorTemplate404 == null) {
				synchronized (SystemUtil.createToken("ConfigImpl", "getErrorTemplate404")) {
					if (errorTemplate404 == null) {
						errorTemplate404 = metaErrorTemplate404.get(this, root);
					}
				}
			}
			return errorTemplate404;
		}
		if (errorTemplate500 == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getErrorTemplate500")) {
				if (errorTemplate500 == null) {
					errorTemplate500 = metaErrorTemplate500.get(this, root);
				}
			}
		}
		return errorTemplate500;
	}

	public ConfigImpl resetErrorTemplates() {
		if (errorTemplate404 != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getErrorTemplate404")) {
				if (errorTemplate404 != null) {
					errorTemplate404 = null;
				}
			}
		}
		if (errorTemplate500 != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getErrorTemplate500")) {
				if (errorTemplate500 != null) {
					errorTemplate500 = null;
				}
			}
		}
		return this;
	}

	@Override
	public short getSessionType() {
		if (sessionType == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getSessionType")) {
				if (sessionType == null) {
					sessionType = metaSessionType.get(this, root);
				}
			}
		}
		return sessionType;
	}

	public ConfigImpl resetSessionType() {
		if (sessionType != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getSessionType")) {
				if (sessionType != null) {
					sessionType = null;
				}
			}
		}
		return this;
	}

	@Override
	public abstract String getUpdateType();

	@Override
	public abstract URL getUpdateLocation();

	@Override
	public Resource getLibraryDirectory() {
		Resource dir = getConfigDir().getRealResource("lib");
		if (!dir.exists()) dir.mkdir();
		return dir;
	}

	@Override
	public Resource getEventGatewayDirectory() {
		Resource dir = getConfigDir().getRealResource("context/admin/gdriver");
		if (!dir.exists()) dir.mkdir();
		return dir;
	}

	@Override
	public Resource getClassesDirectory() {
		Resource dir = getConfigDir().getRealResource("classes");
		if (!dir.exists()) dir.mkdir();
		return dir;
	}

	@Override
	public Resource getClassDirectory() {
		if (deployDirectory == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getClassDirectory")) {
				if (deployDirectory == null) {

					String strDeployDirectory = metaDeployDirectory.get(this, root);
					if (!StringUtil.isEmpty(strDeployDirectory, true)) {
						strDeployDirectory = ConfigUtil.translateOldPath(strDeployDirectory);
					}
					deployDirectory = ConfigUtil.getFile(configDir, strDeployDirectory, "cfclasses", configDir, FileUtil.TYPE_DIR, ResourceUtil.LEVEL_GRAND_PARENT_FILE, this);
				}
			}
		}
		return deployDirectory;
	}

	public ConfigImpl resetClassDirectory() {
		if (deployDirectory != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getClassDirectory")) {
				if (deployDirectory != null) {
					deployDirectory = null;
				}
			}
		}
		return this;
	}

	@Override
	public abstract Resource getRootDirectory();

	/**
	 * FUTHER Returns the value of suppresswhitespace.
	 * 
	 * @return value suppresswhitespace
	 */
	@Override
	public boolean isSuppressWhitespace() {
		return suppresswhitespace;
	}

	/**
	 * FUTHER sets the suppresswhitespace value.
	 * 
	 * @param suppresswhitespace The suppresswhitespace to set.
	 */
	protected void setSuppressWhitespace(boolean suppresswhitespace) {
		this.suppresswhitespace = suppresswhitespace;
	}

	@Override
	public boolean isSuppressContent() {
		if (suppressContent == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "isSuppressContent")) {
				if (suppressContent == null) {
					suppressContent = metaSuppressContent.get(this, root);
				}
			}
		}
		return suppressContent;
	}

	public ConfigImpl resetSuppressContent() {
		if (suppressContent != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "isSuppressContent")) {
				if (suppressContent != null) {
					suppressContent = null;
				}
			}
		}
		return this;
	}

	@Override
	public String getDefaultEncoding() {
		return getWebCharset().name();
	}

	@Override
	public Charset getTemplateCharset() {
		return CharsetUtil.toCharset(getTemplateCharSet());
	}

	public CharSet getTemplateCharSet() {
		if (templateCharset == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getTemplateCharSet")) {
				if (templateCharset == null) {
					templateCharset = metaTemplateCharset.get(this, root);
				}
			}
		}
		return templateCharset;
	}

	public ConfigImpl resetTemplateCharSet() {
		if (templateCharset != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getTemplateCharSet")) {
				if (templateCharset != null) {
					templateCharset = null;
				}
			}
		}
		return this;
	}

	@Override
	public Charset getWebCharset() {
		return CharsetUtil.toCharset(getWebCharSet());
	}

	@Override
	public CharSet getWebCharSet() {
		if (webCharset == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getWebCharSet")) {
				if (webCharset == null) {
					webCharset = metaWebCharset.get(this, root);
				}
			}
		}
		return webCharset;
	}

	public ConfigImpl resetWebCharSet() {
		if (webCharset != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getWebCharSet")) {
				if (webCharset != null) {
					webCharset = null;
				}
			}
		}
		return this;
	}

	@Override
	public Charset getResourceCharset() {
		return CharsetUtil.toCharset(getResourceCharSet());
	}

	@Override
	public CharSet getResourceCharSet() {// = SystemUtil.getCharSet()
		if (resourceCharset == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getResourceCharSet")) {
				if (resourceCharset == null) {
					resourceCharset = metaResourceCharset.get(this, root);
				}
			}
		}
		return resourceCharset;
	}

	public ConfigImpl resetResourceCharSet() {// = SystemUtil.getCharSet()
		if (resourceCharset != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getResourceCharSet")) {
				if (resourceCharset != null) {
					resourceCharset = null;
				}
			}
		}
		return this;
	}

	@Override
	public Resource getTldFile() {
		getTLDs();
		return tldFile;
	}

	@Override
	public Map<String, DataSource> getDataSourcesAsMap() {
		if (datasourcesNoQoQ == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getDataSources")) {
				if (datasourcesNoQoQ == null) {
					Map<String, DataSource> map = new HashMap<String, DataSource>();
					Iterator<Entry<String, DataSource>> it = getDataSourcesAll().entrySet().iterator();
					Entry<String, DataSource> entry;
					while (it.hasNext()) {
						entry = it.next();
						if (!entry.getKey().equals(QOQ_DATASOURCE_NAME)) map.put(entry.getKey(), entry.getValue());
					}
					datasourcesNoQoQ = map;
				}
			}
		}
		return datasourcesNoQoQ;
	}

	private Map<String, DataSource> getDataSourcesAll() {
		if (datasourcesAll == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getDataSources")) {
				if (datasourcesAll == null) {
					datasourcesAll = metaDatasourcesAll.map(this, root);
				}
			}
		}
		return datasourcesAll;
	}

	public ConfigImpl resetDataSources() {
		if (datasourcesAll != null || datasourcesNoQoQ != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getDataSources")) {
				if (datasourcesAll != null || datasourcesNoQoQ != null) {
					datasourcesAll = null;
					datasourcesNoQoQ = null;
				}
			}
		}
		return this;
	}

	@Override
	public DataSource[] getDataSources() {
		Map<String, DataSource> map = getDataSourcesAsMap();
		Iterator<DataSource> it = map.values().iterator();
		DataSource[] ds = new DataSource[map.size()];
		int count = 0;

		while (it.hasNext()) {
			ds[count++] = it.next();
		}
		return ds;
	}

	@Override
	public DataSource getDataSource(String datasource) throws DatabaseException {
		DataSource ds = (datasource == null) ? null : (DataSource) getDataSourcesAll().get(datasource.toLowerCase());
		if (ds != null) return ds;

		// create error detail
		DatabaseException de = new DatabaseException("datasource [" + datasource + "] doesn't exist", null, null, null);
		de.setDetail(ExceptionUtil.createSoundexDetail(datasource, getDataSourcesAll().keySet().iterator(), "datasource names"));
		de.setAdditional(KeyConstants._Datasource, datasource);
		throw de;
	}

	@Override
	public DataSource getDataSource(String datasource, DataSource defaultValue) {
		DataSource ds = (datasource == null) ? null : (DataSource) getDataSourcesAll().get(datasource.toLowerCase());
		if (ds != null) return ds;
		return defaultValue;
	}

	/**
	 * @return the mailDefaultCharset
	 */
	@Override
	public Charset getMailDefaultCharset() {
		return getMailDefaultCharSet().toCharset();
	}

	public CharSet getMailDefaultCharSet() {
		if (mailDefaultCharset == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "mail")) {
				if (mailDefaultCharset == null) {
					mailDefaultCharset = metaMailDefaultCharset.get(this, root);
				}
			}
		}
		return mailDefaultCharset;
	}

	public ConfigImpl resetMailDefaultCharSet() {
		if (mailDefaultCharset != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "mail")) {
				if (mailDefaultCharset != null) {
					mailDefaultCharset = null;
				}
			}
		}
		return this;
	}

	/**
	 * @param defaultResourceProvider the defaultResourceProvider to set
	 */
	protected void setDefaultResourceProvider(ResourceProvider defaultResourceProvider) {
		if (defaultResourceProvider == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCacheHandlers")) {
				if (cacheHandlerClasses == null) {
					cacheHandlerClasses = ConfigFactoryImpl.loadCacheHandler(this, root);
				}
			}
		}
		getResources().registerDefaultResourceProvider(defaultResourceProvider);
	}

	/**
	 * @return the defaultResourceProvider
	 */
	@Override
	public ResourceProvider getDefaultResourceProvider() {
		Resources resources = getResources();
		if (defaultResourceProvider == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getDefaultResourceProvider")) {
				if (defaultResourceProvider == null) {
					defaultResourceProvider = ConfigFactoryImpl.loadDefaultResourceProvider(this, root);
					if (defaultResourceProvider == null) defaultResourceProvider = ResourcesImpl.getFileResourceProvider();
					if (defaultResourceProvider != resources.getDefaultResourceProvider()) resources.registerDefaultResourceProvider(defaultResourceProvider);
				}
			}
		}
		return resources.getDefaultResourceProvider();
	}

	public ConfigImpl resetDefaultResourceProvider() {
		if (defaultResourceProvider != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getDefaultResourceProvider")) {
				if (defaultResourceProvider != null) {
					defaultResourceProvider = null;
					getResources().registerDefaultResourceProvider(ResourcesImpl.getFileResourceProvider());
				}
			}
		}
		return this;
	}

	@Override
	public Iterator<Entry<String, Class<CacheHandler>>> getCacheHandlers() {
		if (cacheHandlerClasses == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCacheHandlers")) {
				if (cacheHandlerClasses == null) {
					cacheHandlerClasses = ConfigFactoryImpl.loadCacheHandler(this, root);
				}
			}
		}
		return cacheHandlerClasses.entrySet().iterator();
	}

	public ConfigImpl resetCacheHandlers() {
		if (cacheHandlerClasses != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCacheHandlers")) {
				if (cacheHandlerClasses != null) {
					cacheHandlerClasses = null;
				}
			}
		}
		return this;
	}

	private Resources getResources() {
		if (initResource) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getResources")) {
				if (initResource) {
					ConfigFactoryImpl.loadResourceProvider(this, root);
					initResource = false;
					cacheHandlerClasses = null;
				}
			}
		}
		return resources;
	}

	public ConfigImpl resetResources() {
		if (!initResource) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getResources")) {
				if (!initResource) {
					initResource = true;
					resources.reset();
				}
			}
		}
		return this;
	}

	protected void addResourceProvider(String strProviderScheme, ClassDefinition cd, Map arguments) {
		((ResourcesImpl) resources).registerResourceProvider(strProviderScheme, cd, arguments);
	}

	/**
	 * @return return the resource providers
	 */
	@Override
	public ResourceProvider[] getResourceProviders() {
		return getResources().getResourceProviders();
	}

	/**
	 * @return return the resource providers
	 */
	@Override
	public ResourceProviderFactory[] getResourceProviderFactories() {
		return ((ResourcesImpl) getResources()).getResourceProviderFactories();
	}

	@Override
	public boolean hasResourceProvider(String scheme) {
		ResourceProviderFactory[] factories = ((ResourcesImpl) getResources()).getResourceProviderFactories();
		for (int i = 0; i < factories.length; i++) {
			if (factories[i].getScheme().equalsIgnoreCase(scheme)) return true;
		}
		return false;
	}

	@Override
	public Resource getResource(String path) {
		return getResources().getResource(path);
	}

	@Override
	public ApplicationListener getApplicationListener() {
		if (applicationListener == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getApplicationListener")) {
				if (applicationListener == null) {

					// type
					ApplicationListener listener = ConfigUtil.loadListener(metaApplicationListenerType.get(this, root), null);

					// mode
					listener.setMode(metaApplicationListenerMode.get(this, root));

					// singleton
					if (listener instanceof ModernAppListener) {// FYI Mixed does extend Modern so it is included
						listener.setSingelton(metaApplicationListenerSingleton.get(this, root));
					}
					applicationListener = listener;

				}
			}
		}
		return applicationListener;
	}

	public ConfigImpl resetApplicationListener() {
		if (applicationListener != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getApplicationListener")) {
				if (applicationListener != null) {
					applicationListener = null;
				}
			}
		}
		return this;
	}

	/**
	 * @return the scriptProtect
	 */
	@Override
	public int getScriptProtect() {
		if (scriptProtect == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getScriptProtect")) {
				if (scriptProtect == null) {
					scriptProtect = AppListenerUtil.translateScriptProtect(metaScriptProtect.get(this, root), ApplicationContext.SCRIPT_PROTECT_ALL);
				}
			}
		}
		return scriptProtect;
	}

	public ConfigImpl resetScriptProtect() {
		if (scriptProtect != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getScriptProtect")) {
				if (scriptProtect != null) {
					scriptProtect = null;
				}
			}
		}
		return this;
	}

	/**
	 * @return the proxyPassword
	 */
	@Override
	public ProxyData getProxyData() {
		if (proxy == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getProxyData")) {
				if (proxy == null) {
					boolean enabled = metaProxyEnabled.get(this, root);
					String server = metaProxyServer.get(this, root);

					if (enabled && !StringUtil.isEmpty(server)) {

						String user = metaProxyUser.get(this, root);
						String pass = metaProxyPass.get(this, root);
						Integer port = metaProxyPort.get(this, root);

						ProxyDataImpl pd = (ProxyDataImpl) ProxyDataImpl.getInstance(server, port, user, pass);

						String strIncludes = metaProxyIncludes.get(this, root);
						Set<String> includes = proxy != null ? ProxyDataImpl.toStringSet(strIncludes) : null;
						if (includes != null) pd.setIncludes(includes);

						String strExcludes = metaProxyExcludes.get(this, root);
						Set<String> excludes = proxy != null ? ProxyDataImpl.toStringSet(strExcludes) : null;
						if (excludes != null) pd.setExcludes(excludes);
						proxy = pd;
					}
				}
			}
		}
		return proxy;
	}

	public ConfigImpl resetProxyData() {
		if (proxy != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getProxyData")) {
				if (proxy != null) {
					proxy = null;
				}
			}
		}
		return this;
	}

	@Override
	@Deprecated
	public boolean isProxyEnableFor(String host) {
		return ProxyDataImpl.isProxyEnableFor(getProxyData(), host);
	}

	/**
	 * @return the triggerComponentDataMember
	 */
	@Override
	public boolean getTriggerComponentDataMember() {
		if (triggerComponentDataMember == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getTriggerComponentDataMember")) {
				if (triggerComponentDataMember == null) {
					triggerComponentDataMember = metaTriggerComponentDataMember.get(this, root);
				}
			}
		}
		return triggerComponentDataMember;
	}

	public ConfigImpl resetTriggerComponentDataMember() {
		if (triggerComponentDataMember != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getTriggerComponentDataMember")) {
				if (triggerComponentDataMember != null) {
					triggerComponentDataMember = null;
				}
			}
		}
		return this;
	}

	@Override
	public Resource getClientScopeDir() {
		if (clientScopeDir == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getClientScopeDir")) {
				if (clientScopeDir == null) {
					Resource configDir = getConfigDir();

					String strClientDirectory = metaClientScopeDir.get(this, root);
					if (!StringUtil.isEmpty(strClientDirectory, true)) {
						strClientDirectory = ConfigUtil.translateOldPath(strClientDirectory.trim());
						clientScopeDir = ConfigUtil.getFile(configDir, strClientDirectory, "client-scope", configDir, FileUtil.TYPE_DIR, ResourceUtil.LEVEL_PARENT_FILE, this);
					}
					else {
						clientScopeDir = configDir.getRealResource("client-scope");
					}
				}
			}
		}
		return clientScopeDir;
	}

	public ConfigImpl resetClientScopeDir() {
		if (clientScopeDir != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getClientScopeDir")) {
				if (clientScopeDir != null) {
					clientScopeDir = null;
				}
			}
		}
		return this;
	}

	@Override
	public Resource getSessionScopeDir() {
		if (sessionScopeDir == null) sessionScopeDir = getConfigDir().getRealResource("session-scope");
		return sessionScopeDir;
	}

	@Override
	public long getClientScopeDirSize() {
		if (clientScopeDirSize == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getClientScopeDirSize")) {
				if (clientScopeDirSize == null) {
					clientScopeDirSize = ByteSizeParser.parseByteSizeDefinition(metaClientScopeDirSize.get(this, root).trim(), 1024L * 1024L * 100L);
				}
			}
		}
		return clientScopeDirSize;
	}

	public ConfigImpl resetClientScopeDirSize() {
		if (clientScopeDirSize != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getClientScopeDirSize")) {
				if (clientScopeDirSize != null) {
					clientScopeDirSize = null;
				}
			}
		}
		return this;
	}

	// =

	public long getSessionScopeDirSize() {
		return sessionScopeDirSize;
	}

	protected void setSessionScopeDir(Resource sessionScopeDir) {
		this.sessionScopeDir = sessionScopeDir;
	}

	@Override
	public ClassLoader getRPCClassLoader(boolean reload) throws IOException {
		return PhysicalClassLoaderFactory.getRPCClassLoader(this, getJavaSettings(), reload);
	}

	@Override
	public ClassLoader getRPCClassLoader(boolean reload, JavaSettings js) throws IOException {
		return PhysicalClassLoaderFactory.getRPCClassLoader(this, js != null ? js : getJavaSettings(), reload);
	}

	private static final Object dclt = new SerializableObject();
	private static final long POOL_MAX_IDLE = 60000;

	@Override
	public PhysicalClassLoader getDirectClassLoader(boolean reload) throws IOException {
		if (directClassLoader == null || reload) {
			synchronized (dclt) {
				if (directClassLoader == null || reload) {
					Resource dir = getClassDirectory().getRealResource("direct/");
					if (!dir.exists()) {
						ResourceUtil.createDirectoryEL(dir, true);
					}
					directClassLoader = PhysicalClassLoaderFactory.getPhysicalClassLoader(this, dir, reload);
				}
			}
		}
		return directClassLoader;
	}

	public void clearRPCClassLoader() {
		rpcClassLoaders.clear();
	}

	@Override
	public Resource getCacheDir() {
		if (cacheDir == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCacheDir")) {
				if (cacheDir == null) {
					Resource configDir = getConfigDir();

					String strCacheDirectory = metaCacheDir.get(this, root);
					if (!StringUtil.isEmpty(strCacheDirectory)) {
						strCacheDirectory = ConfigUtil.translateOldPath(strCacheDirectory);
						cacheDir = ConfigUtil.getFile(configDir, strCacheDirectory, "cache", configDir, FileUtil.TYPE_DIR, ResourceUtil.LEVEL_GRAND_PARENT_FILE, this);
					}
					else {
						cacheDir = configDir.getRealResource("cache");
					}
				}
			}
		}
		return cacheDir;
	}

	public ConfigImpl resetCacheDir() {
		if (cacheDir != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCacheDir")) {
				if (cacheDir != null) {
					cacheDir = null;
				}
			}
		}
		return this;
	}

	@Override
	public long getCacheDirSize() {
		if (cacheDirSize == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCacheDirSize")) {
				if (cacheDirSize == null) {
					cacheDirSize = ByteSizeParser.parseByteSizeDefinition(metaCacheDirSize.get(this, root), 1024L * 1024L * 100L);
					;
				}
			}
		}
		return cacheDirSize;
	}

	public ConfigImpl resetCacheDirSize() {
		if (cacheDirSize != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCacheDirSize")) {
				if (cacheDirSize != null) {
					cacheDirSize = null;
				}
			}
		}
		return this;
	}

	public DumpWriterEntry[] getDumpWritersEntries() {
		if (dmpWriterEntries == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getDumpWritersEntries")) {
				if (dmpWriterEntries == null) {
					dmpWriterEntries = ConfigFactoryImpl.loadDumpWriter(this, root, null);
					// MUST handle default value was returned
				}
			}
		}
		return dmpWriterEntries;
	}

	public ConfigImpl resetDumpWritersEntries() {
		if (dmpWriterEntries != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getDumpWritersEntries")) {
				if (dmpWriterEntries != null) {
					dmpWriterEntries = null;
				}
			}
		}
		return this;
	}

	@Override
	public DumpWriter getDefaultDumpWriter(int defaultType) {
		DumpWriterEntry[] entries = getDumpWritersEntries();
		if (entries != null) for (int i = 0; i < entries.length; i++) {
			if (entries[i].getDefaultType() == defaultType) {
				return entries[i].getWriter();
			}
		}
		return new HTMLDumpWriter();
	}

	@Override
	public DumpWriter getDumpWriter(String name) throws DeprecatedException {
		throw new DeprecatedException("this method is no longer supported");
	}

	@Override
	public DumpWriter getDumpWriter(String name, int defaultType) throws ExpressionException {
		if (StringUtil.isEmpty(name)) return getDefaultDumpWriter(defaultType);

		DumpWriterEntry[] entries = getDumpWritersEntries();
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].getName().equals(name)) {
				return entries[i].getWriter();
			}
		}

		// error
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < entries.length; i++) {
			if (i > 0) sb.append(", ");
			sb.append(entries[i].getName());
		}
		throw new ExpressionException("invalid format definition [" + name + "], valid definitions are [" + sb + "]");
	}

	@Override
	public boolean useComponentShadow() {
		if (useComponentShadow == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "useComponentShadow")) {
				if (useComponentShadow == null) {
					useComponentShadow = metaUseComponentShadow.get(this, root);
				}
			}
		}
		return useComponentShadow;
	}

	public ConfigImpl resetComponentShadow() {
		if (useComponentShadow != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "useComponentShadow")) {
				if (useComponentShadow != null) {
					useComponentShadow = null;
				}
			}
		}
		return this;
	}

	@Override
	public boolean useComponentPathCache() {
		if (useComponentPathCache == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "useComponentPathCache")) {
				if (useComponentPathCache == null) {
					useComponentPathCache = metaUseComponentPathCache.get(this, root);
				}
			}
		}
		return useComponentPathCache;
	}

	public ConfigImpl resetComponentPathCache() {
		if (useComponentPathCache != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "useComponentPathCache")) {
				if (useComponentPathCache != null) {
					useComponentPathCache = null;
				}
			}
		}
		return this;
	}

	@Override
	public boolean useCTPathCache() {
		if (useCTPathCache == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "useCTPathCache")) {
				if (useCTPathCache == null) {
					useCTPathCache = metaUseCTPathCache.get(this, root);
				}
			}
		}
		return useCTPathCache;
	}

	public ConfigImpl resetUseCTPathCache() {
		if (useCTPathCache != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "useCTPathCache")) {
				if (useCTPathCache != null) {
					useCTPathCache = null;
				}
			}
		}
		return this;
	}

	public void flushComponentPathCache() {
		componentPathCache.flush();
	}

	public void flushApplicationPathCache() {
		if (applicationPathCache != null) applicationPathCache.clear();
	}

	public void flushCTPathCache() {
		if (ctPatchCache != null) ctPatchCache.clear();
	}

	@Override
	public PrintWriter getErrWriter() {
		if (err == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getErrWriter")) {
				if (err == null) {
					String strErr = metaErr.get(this, root);
					PrintStream ps = ConfigFactoryImpl.toPrintStream(this, strErr, true);

					if (ps == null) {
						err = SystemUtil.getPrintWriter(SystemUtil.ERR);
					}
					else {
						err = new PrintWriter(ps);
						System.setErr(ps);
					}
				}
			}
		}
		return err;
	}

	public ConfigImpl resetErrWriter() {
		if (err != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getErrWriter")) {
				if (err != null) {
					err = null;
				}
			}
		}
		return this;
	}

	@Override
	public PrintWriter getOutWriter() {
		if (out == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getOutWriter")) {
				if (out == null) {
					String strOut = metaOut.get(this, root);
					PrintStream ps = ConfigFactoryImpl.toPrintStream(this, strOut, false);

					if (ps == null) {
						out = SystemUtil.getPrintWriter(SystemUtil.OUT);
					}
					else {
						out = new PrintWriter(ps);
						System.setOut(ps);
					}
				}
			}
		}
		return out;
	}

	public ConfigImpl resetOutWriter() {
		if (out != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getOutWriter")) {
				if (out != null) {
					out = null;
				}
			}
		}
		return this;
	}

	@Override
	public DatasourceConnPool getDatasourceConnectionPool(DataSource ds, String user, String pass) {
		String id = DatasourceConnectionFactory.createId(ds, user, pass);
		DatasourceConnPool pool = pools.get(id);
		if (pool == null) {
			synchronized (id) {
				pool = pools.get(id);
				if (pool == null) {// TODO add config but from where?
					DataSourcePro dsp = (DataSourcePro) ds;
					// MUST merge ConnectionLimit and MaxTotal
					int mt = 0;
					if (dsp.getMaxTotal() > 0) mt = dsp.getMaxTotal();
					else {
						mt = dsp.getConnectionLimit();
						if (mt <= 0) mt = Integer.MAX_VALUE;
					}

					// maxWaitMillis: how long to wait for a connection when pool is exhausted (30 seconds)
					long maxWaitMillis = 30000L;
					// minEvictableIdleTimeMillis: use idleTimeout (in minutes) for how long connection can be idle
					// before eviction
					// -1 = not set (use default 10 minutes), 0 = infinite (no eviction), >0 = use that value
					int idleTimeout = dsp.getIdleTimeout();
					long minEvictableIdleTimeMillis;
					if (idleTimeout > 0) {
						minEvictableIdleTimeMillis = idleTimeout * 60000L;
					}
					else if (idleTimeout == 0) {
						minEvictableIdleTimeMillis = -1; // infinite - disable eviction
					}
					else {
						minEvictableIdleTimeMillis = 10 * 60000L; // default: 10 minutes
					}

					pool = new DatasourceConnPool(this, ds, user, pass, "datasource", DatasourceConnPool.createPoolConfig(null, null, null, dsp.getMinIdle(), dsp.getMaxIdle(), mt,
							maxWaitMillis, minEvictableIdleTimeMillis, 0, 0, 0, null));
					pools.put(id, pool);
					cleanConnectionPools(id);
				}
			}
		}
		return pool;
	}

	private void cleanConnectionPools(String excludeId) {
		DatasourceConnPool pool;
		List<String> keysToRemove = null;
		for (Entry<String, DatasourceConnPool> e: pools.entrySet()) {
			if (excludeId.equals(e.getKey())) {
				continue;
			}
			pool = e.getValue();
			if ((pool.getNumActive() + pool.getNumIdle() + pool.getNumWaiters()) == 0 && (pool.getLastBorrowed() + POOL_MAX_IDLE) < System.currentTimeMillis()) {
				if (keysToRemove == null) keysToRemove = new ArrayList<>();
				keysToRemove.add(e.getKey());
			}
		}

		if (keysToRemove != null) {
			for (String k: keysToRemove) {
				pools.remove(k);
			}
		}
	}

	@Override
	public MockPool getDatasourceConnectionPool() {
		return new MockPool();
	}

	@Override
	public Collection<DatasourceConnPool> getDatasourceConnectionPools() {
		return pools.values();
	}

	@Override
	public void removeDatasourceConnectionPool(DataSource ds) {
		for (Entry<String, DatasourceConnPool> e: pools.entrySet()) {
			if (e.getValue().getFactory().getDatasource().getName().equalsIgnoreCase(ds.getName())) {
				synchronized (e.getKey()) {
					pools.remove(e.getKey());
				}
				e.getValue().clear();
			}
		}
	}

	@Override
	public boolean doLocalCustomTag() {
		if (doLocalCustomTag == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "doLocalCustomTag")) {
				if (doLocalCustomTag == null) {
					doLocalCustomTag = metaDoLocalCustomTag.get(this, root);
				}
			}
		}
		return doLocalCustomTag;
	}

	public ConfigImpl resetLocalCustomTag() {
		if (doLocalCustomTag != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "doLocalCustomTag")) {
				if (doLocalCustomTag != null) {
					doLocalCustomTag = null;
				}
			}
		}
		return this;
	}

	@Override
	public String[] getCustomTagExtensions() {
		if (customTagExtensions == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCustomTagExtensions")) {
				if (customTagExtensions == null) {
					customTagExtensions = ListUtil.trimItems(ListUtil.listToStringArray(metaCustomTagExtensions.get(this, root), ','));
				}
			}
		}
		return customTagExtensions;
	}

	public ConfigImpl resetCustomTagExtensions() {
		if (customTagExtensions != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCustomTagExtensions")) {
				if (customTagExtensions != null) {
					customTagExtensions = null;
				}
			}
		}
		return this;
	}

	@Override
	public boolean doComponentDeepSearch() {
		if (doComponentTagDeepSearch == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "doComponentDeepSearch")) {
				if (doComponentTagDeepSearch == null) {
					doComponentTagDeepSearch = metaDoComponentTagDeepSearch.get(this, root);
				}
			}
		}
		return doComponentTagDeepSearch;
	}

	public ConfigImpl resetComponentDeepSearch() {
		if (doComponentTagDeepSearch != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "doComponentDeepSearch")) {
				if (doComponentTagDeepSearch != null) {
					doComponentTagDeepSearch = null;
				}
			}
		}
		return this;
	}

	@Override
	public boolean doCustomTagDeepSearch() {
		if (doCustomTagDeepSearch == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "doCustomTagDeepSearch")) {
				if (doCustomTagDeepSearch == null) {
					doCustomTagDeepSearch = metaDoCustomTagDeepSearch.get(this, root);
				}
			}
		}
		return doCustomTagDeepSearch;
	}

	public ConfigImpl resetCustomTagDeepSearch() {
		if (doCustomTagDeepSearch != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "doCustomTagDeepSearch")) {
				if (doCustomTagDeepSearch != null) {
					doCustomTagDeepSearch = null;
				}
			}
		}
		return this;
	}

	/**
	 * @return the version
	 */
	@Override
	public double getVersion() {
		if (version == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getVersion")) {
				if (version == null) {
					version = metaVersion.get(this, root);
				}
			}

		}
		return version;
	}

	public ConfigImpl resetVersion() {
		if (version != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getVersion")) {
				if (version != null) {
					version = null;
				}
			}
		}
		return this;
	}

	@Override
	public boolean closeConnection() {
		if (closeConnection == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "closeConnection")) {
				if (closeConnection == null) {
					closeConnection = metaCloseConnection.get(this, root);
				}
			}
		}
		return closeConnection;
	}

	public ConfigImpl resetConnection() {
		if (closeConnection != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "closeConnection")) {
				if (closeConnection != null) {
					closeConnection = null;
				}
			}
		}
		return this;
	}

	@Override
	public boolean contentLength() {
		if (contentLength == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "contentLength")) {
				if (contentLength == null) {
					contentLength = metaContentLength.get(this, root);
				}
			}
		}
		return contentLength;
	}

	public ConfigImpl resetContentLength() {
		if (contentLength != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "contentLength")) {
				if (contentLength != null) {
					contentLength = null;
				}
			}
		}
		return this;
	}

	@Override
	public boolean allowCompression() {
		if (allowCompression == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "allowCompression")) {
				if (allowCompression == null) {
					allowCompression = metaAllowCompression.get(this, root);
				}
			}
		}
		return allowCompression;
	}

	public ConfigImpl resetAllowCompression() {
		if (allowCompression != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "allowCompression")) {
				if (allowCompression != null) {
					allowCompression = null;
				}
			}
		}
		return this;
	}

	/**
	 * @return the constants
	 */
	@Override
	public Struct getConstants() {
		if (constants == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getConstants")) {
				if (constants == null) {
					constants = metaConstants.get(this, root);
				}
			}
		}
		return constants;
	}

	public ConfigImpl resetConstants() {
		if (constants != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getConstants")) {
				if (constants != null) {
					constants = null;
				}
			}
		}
		return this;
	}

	/**
	 * @return the showVersion
	 */
	@Override
	public boolean isShowVersion() {
		if (showVersion == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "isShowVersion")) {
				if (showVersion == null) {
					showVersion = metaShowVersion.get(this, root);
				}
			}
		}
		return showVersion;
	}

	public ConfigImpl resetShowVersion() {
		if (showVersion != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "isShowVersion")) {
				if (showVersion != null) {
					showVersion = null;
				}
			}
		}
		return this;
	}

	@Override
	public RemoteClient[] getRemoteClients() {
		if (remoteClients == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getRemoteClients")) {
				if (remoteClients == null) {
					List<RemoteClient> list = metaRemoteClients.list(this, root);

					remoteClients = list.toArray(new RemoteClient[list.size()]);
				}
			}
		}
		return remoteClients;
	}

	public ConfigImpl resetRemoteClients() {
		if (remoteClients != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getRemoteClients")) {
				if (remoteClients != null) {
					remoteClients = null;
				}
			}
		}
		return this;
	}

	@Override
	public SpoolerEngine getSpoolerEngine() {
		if (remoteClientSpoolerEngine == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getSpoolerEngine")) {
				if (remoteClientSpoolerEngine == null) {
					remoteClientSpoolerEngine = new SpoolerEngineImpl(this, "Remote Client Spooler");
				}
			}
		}
		return remoteClientSpoolerEngine;
	}

	public int getRemoteClientMaxThreads() {
		if (remoteClientMaxThreads == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getRemoteClientMaxThreads")) {
				if (remoteClientMaxThreads == null) {
					remoteClientMaxThreads = metaRemoteClientMaxThreads.get(this, root);
				}
			}
		}
		return remoteClientMaxThreads;
	}

	public ConfigImpl resetRemoteClientMaxThreads() {
		if (remoteClientMaxThreads != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getRemoteClientMaxThreads")) {
				if (remoteClientMaxThreads != null) {
					remoteClientMaxThreads = null;
				}
			}
		}
		return this;
	}

	//
	@Override
	public Resource getRemoteClientDirectory() {
		if (remoteClientDirectory == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getRemoteClientDirectory")) {
				if (remoteClientDirectory == null) {
					String strDir = metaRemoteClientDirectory.get(this, root);
					remoteClientDirectory = ConfigUtil.getFile(getRootDirectory(), strDir, "client-task", getConfigDir(), FileUtil.TYPE_DIR, ResourceUtil.LEVEL_GRAND_PARENT_FILE,
							this);

					if (!remoteClientDirectory.exists()) remoteClientDirectory.mkdirs();
				}
			}
		}
		return remoteClientDirectory;
	}

	public ConfigImpl resetRemoteClientDirectory() {
		if (remoteClientDirectory != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getRemoteClientDirectory")) {
				if (remoteClientDirectory != null) {
					remoteClientDirectory = null;
				}
			}
		}
		return this;
	}

	@Override
	public boolean getErrorStatusCode() {
		if (errorStatusCode == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getErrorStatusCode")) {
				if (errorStatusCode == null) {
					errorStatusCode = metaErrorStatusCode.get(this, root);
				}
			}
		}
		return errorStatusCode;
	}

	public ConfigImpl resetErrorStatusCode() {
		if (errorStatusCode != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getErrorStatusCode")) {
				if (errorStatusCode != null) {
					errorStatusCode = null;
				}
			}
		}
		return this;
	}

	@Override
	public int getLocalMode() {
		if (localMode == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getLocalMode")) {
				if (localMode == null) {
					localMode = metaLocalMode.get(this, root);
				}
			}
		}
		return localMode;
	}

	public ConfigImpl resetLocalMode() {
		if (localMode != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getLocalMode")) {
				if (localMode != null) {
					localMode = null;
				}
			}
		}
		return this;
	}

	@Override
	public Resource getVideoDirectory() {
		// TODO take from tag <video>
		Resource dir = getConfigDir().getRealResource("video");
		if (!dir.exists()) dir.mkdirs();
		return dir;
	}

	@Override
	public ExtensionProvider[] getExtensionProviders() {
		throw new RuntimeException("no longer supported, use getRHExtensionProviders() instead.");
	}

	@Override
	public RHExtensionProvider[] getRHExtensionProviders() {
		if (rhextensionProviders == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getRHExtensionProviders")) {
				if (rhextensionProviders == null) {
					rhextensionProviders = ConfigFactoryImpl.loadExtensionProviders(this, root);
				}
			}
		}
		return rhextensionProviders;
	}

	public ConfigImpl resetRHExtensionProviders() {
		if (rhextensionProviders != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getRHExtensionProviders")) {
				if (rhextensionProviders != null) {
					rhextensionProviders = null;
				}
			}
		}
		return this;
	}

	// = Constants.RH_EXTENSION_PROVIDERS;

	@Override
	public Extension[] getExtensions() {
		throw new PageRuntimeException("no longer supported");
	}

	@Override
	public RHExtension[] getRHExtensions() {
		return rhextensions;
	}

	public ConfigImpl resetRHExtensions() {
		return this;
	}

	public String getExtensionsMD5() {
		return extensionsMD5;
	}

	protected void setExtensions(RHExtension[] extensions, String md5) {
		this.rhextensions = extensions;
		this.extensionsMD5 = md5;
	}

	public List<ExtensionDefintion> getExtensionDefinitions() {
		if (extensionsDefs == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getExtensionDefinitions")) {
				if (extensionsDefs == null) {
					extensionsDefs = ConfigFactoryImpl.loadExtensionDefinition(this, root);
				}
			}
		}
		return this.extensionsDefs;
	}

	public ConfigImpl resetExtensionDefinitions() {
		if (extensionsDefs != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getExtensionDefinitions")) {
				if (extensionsDefs != null) {
					extensionsDefs = null;
				}
			}
		}
		return this;
	}

	@Override
	public boolean isExtensionEnabled() {
		throw new PageRuntimeException("no longer supported");
	}

	@Override
	public boolean allowRealPath() {
		if (allowRealPath == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "allowRealPath")) {
				if (allowRealPath == null) {
					allowRealPath = metaAllowRealPath.get(this, root);
				}
			}
		}
		return allowRealPath;
	}

	public ConfigImpl resetAllowRealPath() {
		if (allowRealPath != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "allowRealPath")) {
				if (allowRealPath != null) {
					allowRealPath = null;

				}
			}
		}
		return this;
	}
	// = true

	/**
	 * @return the classClusterScope
	 */
	@Override
	public Class getClusterClass() {
		return clusterClass;
	}

	/**
	 * @param clusterClass the classClusterScope to set
	 */
	protected void setClusterClass(Class clusterClass) {
		this.clusterClass = clusterClass;
	}

	@Override
	public Struct getRemoteClientUsage() {
		if (remoteClientUsage == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getRemoteClientUsage")) {
				if (remoteClientUsage == null) {
					Struct _clients = ConfigUtil.getAsStruct("remoteClients", root);
					Struct sct = ConfigUtil.getAsStruct(null, _clients, true, "usage");// config.setRemoteClientUsage(toStruct(strUsage));
					if (sct == null) remoteClientUsage = new StructImpl();
					else remoteClientUsage = sct;

				}
			}
		}
		return remoteClientUsage;
	}

	public ConfigImpl resetRemoteClientUsage() {
		if (remoteClientUsage != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getRemoteClientUsage")) {
				if (remoteClientUsage != null) {
					remoteClientUsage = null;

				}
			}
		}
		return this;
	}

	@Override
	public Class<AdminSync> getAdminSyncClass() {
		if (adminSyncClass == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getAdminSyncClass")) {
				if (adminSyncClass == null) {
					try {
						ClassDefinition asc = ConfigFactoryImpl.getClassDefinition(this, root, "adminSync", getIdentification());
						if (!asc.hasClass()) asc = ConfigFactoryImpl.getClassDefinition(this, root, "adminSynchronisation", getIdentification());

						if (asc.hasClass()) {

							Class clazz = asc.getClazz();
							if (!Reflector.isInstaneOf(clazz, AdminSync.class, false))
								throw new ApplicationException("class [" + clazz.getName() + "] does not implement interface [" + AdminSync.class.getName() + "]");
							adminSyncClass = clazz;

						}
					}
					catch (Throwable t) {
						ExceptionUtil.rethrowIfNecessary(t);
						LogUtil.logGlobal(this, ConfigFactoryImpl.class.getName(), t);

					}
					if (adminSyncClass == null) adminSyncClass = AdminSyncNotSupported.class;
				}
			}
		}
		return adminSyncClass;
	}

	@Override
	public AdminSync getAdminSync() throws ClassException {
		if (adminSync == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getAdminSyncClass")) {
				if (adminSync == null) {
					adminSync = (AdminSync) ClassUtil.loadInstance(getAdminSyncClass());
				}
			}

		}
		return this.adminSync;
	}

	public ConfigImpl resetAdminSyncClass() {
		if (adminSyncClass != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getAdminSyncClass")) {
				if (adminSyncClass != null) {
					adminSyncClass = null;
					adminSync = null;
				}
			}
		}
		return this;
	}

	@Override
	public Class getVideoExecuterClass() {
		return videoExecuterClass;
	}

	protected void setVideoExecuterClass(Class videoExecuterClass) {
		this.videoExecuterClass = videoExecuterClass;
	}

	/**
	 * @return the tagMappings
	 */
	@Override
	public Collection<Mapping> getTagMappings() {
		getTLDs();
		return tagMappings.values();

		// MUST 7 flushPageSourcePool(config.getTagMappings());

	}

	@Override
	public Mapping getTagMapping(String mappingName) {
		getTLDs();
		return tagMappings.get(mappingName);
	}

	@Override
	public Mapping getDefaultTagMapping() {
		getTLDs();
		return defaultTagMapping;
	}

	@Override
	public Mapping getFunctionMapping(String mappingName) {
		getFLDs();
		return functionMappings.get(mappingName);
	}

	@Override
	public Mapping getDefaultFunctionMapping() {
		getFLDs();
		return defaultFunctionMapping;
	}

	@Override
	public Collection<Mapping> getFunctionMappings() {
		getFLDs();
		return functionMappings.values();

		// MUST 7 ConfigWebFactory.flushPageSourcePool(config.getFunctionMappings());
	}

	/*
	 * *
	 * 
	 * @return the tagDirectory
	 * 
	 * public Resource getTagDirectory() { return tagDirectory; }
	 */

	/**
	 * mapping used for script (JSR 223)
	 * 
	 * @return
	 */
	public Mapping getScriptMapping() {
		if (scriptMapping == null) {
			// Physical resource TODO make in RAM
			Resource physical = getConfigDir().getRealResource("jsr223");
			if (!physical.exists()) physical.mkdirs();

			this.scriptMapping = new MappingImpl(this, "/mapping-script/", physical.getAbsolutePath(), null, ConfigPro.INSPECT_AUTO, 60000, 1000, true, true, true, true, false,
					true, null, -1, -1);
		}
		return scriptMapping;
	}

	@Override
	public String getDefaultDataSource() {
		// TODO Auto-generated method stub
		return null;
	}

	protected void setDefaultDataSource(String defaultDataSource) {
		// this.defaultDataSource=defaultDataSource;
	}

	/**
	 * @return the inspectTemplate
	 */
	@Override
	public short getInspectTemplate() {
		if (inspectTemplate == -1) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getInspectTemplate")) {
				if (inspectTemplate == -1) {
					String strInspectTemplate = ConfigFactoryImpl.getAttr(this, root, "inspectTemplate");
					if (!StringUtil.isEmpty(strInspectTemplate, true)) {
						inspectTemplate = ConfigUtil.inspectTemplate(strInspectTemplate, ConfigPro.INSPECT_AUTO);
					}
					if (inspectTemplate == -1) inspectTemplate = INSPECT_AUTO;
				}
			}
		}
		return inspectTemplate;
	}

	public ConfigImpl resetInspectTemplate() {
		if (inspectTemplate != -1) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getInspectTemplate")) {
				if (inspectTemplate != -1) {
					inspectTemplate = -1;
				}
			}
		}
		return this;
	}

	@Override
	public boolean getTypeChecking() {
		if (typeChecking == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getTypeChecking")) {
				if (typeChecking == null) {
					typeChecking = metaTypeChecking.get(this, root);
				}
			}
		}
		return typeChecking;
	}

	public ConfigImpl resetTypeChecking() {
		if (typeChecking != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getTypeChecking")) {
				if (typeChecking != null) {
					typeChecking = null;
				}
			}
		}
		return this;
	}

	@Override
	public int getInspectTemplateAutoInterval(boolean slow) {
		if (inspectTemplateAutoIntervalSlow == ConfigPro.INSPECT_INTERVAL_UNDEFINED) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getInspectTemplateAutoInterval")) {
				if (inspectTemplateAutoIntervalSlow == ConfigPro.INSPECT_INTERVAL_UNDEFINED) {
					inspectTemplateAutoIntervalFast = Caster.toIntValue(ConfigFactoryImpl.getAttr(this, root, "inspectTemplateIntervalFast"), ConfigPro.INSPECT_INTERVAL_FAST);
					if (inspectTemplateAutoIntervalFast <= 0) inspectTemplateAutoIntervalFast = ConfigPro.INSPECT_INTERVAL_FAST;
					inspectTemplateAutoIntervalSlow = Caster.toIntValue(ConfigFactoryImpl.getAttr(this, root, "inspectTemplateIntervalSlow"), ConfigPro.INSPECT_INTERVAL_SLOW);
					if (inspectTemplateAutoIntervalSlow <= 0) inspectTemplateAutoIntervalSlow = ConfigPro.INSPECT_INTERVAL_SLOW;
				}
			}
		}
		return slow ? inspectTemplateAutoIntervalSlow : inspectTemplateAutoIntervalFast;
	}

	public ConfigImpl resetInspectTemplateAutoInterval() {
		if (inspectTemplateAutoIntervalSlow != ConfigPro.INSPECT_INTERVAL_UNDEFINED) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getInspectTemplateAutoInterval")) {
				if (inspectTemplateAutoIntervalSlow != ConfigPro.INSPECT_INTERVAL_UNDEFINED) {
					inspectTemplateAutoIntervalSlow = ConfigPro.INSPECT_INTERVAL_UNDEFINED;
					inspectTemplateAutoIntervalFast = ConfigPro.INSPECT_INTERVAL_UNDEFINED;
				}
			}
		}
		return this;
	}

	@Override
	public String getSerialNumber() {
		return "";
	}

	/**
	 * creates a new RamCache, please make sure to finalize.
	 * 
	 * @param arguments possible arguments are "timeToLiveSeconds", "timeToIdleSeconds" and
	 *            "controlInterval"
	 * @throws IOException
	 */
	public Cache createRAMCache(Struct arguments) throws IOException {
		RamCache rc = new RamCache();
		if (arguments == null) arguments = new StructImpl();
		rc.init(this, "" + CreateUniqueId.invoke(), arguments);
		return rc;
	}

	public Map<Integer, String> getCacheDefaultConnectionNames() {
		if (cacheDefaultConnectionNames == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCacheDefaultConnectionName")) {
				if (cacheDefaultConnectionNames == null) {
					Map<Integer, String> names = new HashMap<>();

					// resource
					String str = metaCacheDefaultConnectionNamesResource.get(this, root);
					if (!StringUtil.isEmpty(str, true)) names.put(ConfigPro.CACHE_TYPE_RESOURCE, str);

					// function
					str = metaCacheDefaultConnectionNamesFunction.get(this, root);
					if (!StringUtil.isEmpty(str, true)) names.put(ConfigPro.CACHE_TYPE_FUNCTION, str);

					// include
					str = metaCacheDefaultConnectionNamesInclude.get(this, root);
					if (!StringUtil.isEmpty(str, true)) names.put(ConfigPro.CACHE_TYPE_INCLUDE, str);

					// query
					str = metaCacheDefaultConnectionNamesQuery.get(this, root);
					if (!StringUtil.isEmpty(str, true)) names.put(ConfigPro.CACHE_TYPE_QUERY, str);

					// template
					str = metaCacheDefaultConnectionNamesTemplate.get(this, root);
					if (!StringUtil.isEmpty(str, true)) names.put(ConfigPro.CACHE_TYPE_TEMPLATE, str);

					// object
					str = metaCacheDefaultConnectionNamesObject.get(this, root);
					if (!StringUtil.isEmpty(str, true)) names.put(ConfigPro.CACHE_TYPE_OBJECT, str);

					// file
					str = metaCacheDefaultConnectionNamesFile.get(this, root);
					if (!StringUtil.isEmpty(str, true)) names.put(ConfigPro.CACHE_TYPE_FILE, str);

					// HTTP
					str = metaCacheDefaultConnectionNamesHTTP.get(this, root);
					if (!StringUtil.isEmpty(str, true)) names.put(ConfigPro.CACHE_TYPE_HTTP, str);

					// Webservice
					str = metaCacheDefaultConnectionNamesWebservice.get(this, root);
					if (!StringUtil.isEmpty(str, true)) names.put(ConfigPro.CACHE_TYPE_WEBSERVICE, str);

					cacheDefaultConnectionNames = names;
				}
			}
		}
		return cacheDefaultConnectionNames;
	}

	@Override
	public String getCacheDefaultConnectionName(int type) {
		String res = getCacheDefaultConnectionNames().get(type);
		if (StringUtil.isEmpty(res, true)) return "";
		return res.trim();
	}

	public ConfigImpl resetCacheDefaultConnectionNames() {
		if (cacheDefaultConnectionNames != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCacheDefaultConnectionName")) {
				if (cacheDefaultConnectionNames != null) {
					cacheDefaultConnectionNames = null;
				}
			}
		}
		return this;
	}

	public Map<Integer, CacheConnection> getCacheDefaultConnections() {
		if (cacheDefaultConnection == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCacheDefaultConnection")) {
				if (cacheDefaultConnection == null) {

					Map<Integer, String> names = getCacheDefaultConnectionNames();
					Map<Integer, CacheConnection> tmp = new HashMap<>();

					CacheConnection cc;
					for (Entry<String, CacheConnection> entry: getCacheConnections().entrySet()) {
						cc = entry.getValue();

						for (Entry<Integer, String> e: names.entrySet()) {
							if (cc.getName().equalsIgnoreCase(e.getValue())) {
								tmp.put(e.getKey(), cc);
							}
						}
					}

					// when default was set to null
					/*
					 * for (Entry<Integer, String> e: names.entrySet()) { if (StringUtil.isEmpty(e.getValue()) &&
					 * tmp.get(e.getKey()) != null) { tmp.remove(e.getKey()); } }
					 */
					cacheDefaultConnection = tmp;
				}
			}
		}
		return cacheDefaultConnection;
	}

	@Override
	public CacheConnection getCacheDefaultConnection(int type) {
		return getCacheDefaultConnections().get(type);
	}

	public ConfigImpl resetCacheDefaultConnections() {
		if (cacheDefaultConnection != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCacheDefaultConnection")) {
				if (cacheDefaultConnection != null) {
					cacheDefaultConnection = null;
				}
			}
		}
		return this;
	}

	@Override
	public Map<String, CacheConnection> getCacheConnections() {// = new HashMap<String, CacheConnection>()
		if (cacheConnection == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCacheConnections")) {
				if (cacheConnection == null) {
					cacheConnection = metaCacheConnection.map(this, root);
				}
			}
		}
		return cacheConnection;
	}

	public ConfigImpl resetCacheConnections() {// = new HashMap<String, CacheConnection>()
		if (cacheConnection != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCacheConnections")) {
				if (cacheConnection != null) {
					cacheConnection = null;
				}
			}
		}
		return this;
	}

	public ConfigImpl resetCacheAll() {
		resetCacheDefaultConnectionNames();
		resetCacheDefaultConnections();
		resetCacheConnections();
		resetCacheDefinitions();
		return this;
	}

	@Override
	public boolean getExecutionLogEnabled() {
		if (executionLogEnabled == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getExecutionLogEnabled")) {
				if (executionLogEnabled == null) {
					executionLogEnabled = metaExecutionLogEnabled.get(this, root);
				}
			}
		}
		return executionLogEnabled;
	}

	public ConfigImpl resetExecutionLogEnabled() {
		if (executionLogEnabled != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getExecutionLogEnabled")) {
				if (executionLogEnabled != null) {
					executionLogEnabled = null;
				}
			}
		}
		return this;
	}

	@Override
	public ExecutionLogFactory getExecutionLogFactory() {
		if (executionLogFactory == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getExecutionLogFactory")) {
				if (executionLogFactory == null) {
					executionLogFactory = ConfigFactoryImpl.loadExeLog(this, root);
				}
			}
		}
		return executionLogFactory;
	}

	public ConfigImpl resetExecutionLogFactory() {
		if (executionLogFactory != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getExecutionLogFactory")) {
				if (executionLogFactory != null) {
					executionLogFactory = null;
				}
			}
		}
		return this;
	}

	@Override
	public ORMEngine resetORMEngine(PageContext pc, boolean force) throws PageException {
		// String name = pc.getApplicationContext().getName();
		// ormengines.remove(name);
		ORMEngine e = getORMEngine(pc);
		e.reload(pc, force);
		return e;
	}

	@Override
	public ORMEngine getORMEngine(PageContext pc) throws PageException {
		String name = pc.getApplicationContext().getName();

		ORMEngine engine = ormengines.get(name);
		if (engine == null) {
			// try {
			Throwable t = null;

			try {
				engine = (ORMEngine) ClassUtil.loadInstance(getORMEngineClassDefintion().getClazz());
				engine.init(pc);
			}
			catch (ClassException ce) {
				t = ce;
			}
			catch (BundleException be) {
				t = be;
			}
			catch (NoClassDefFoundError ncfe) {
				t = ncfe;
			}

			if (t != null) {
				ApplicationException ae = new ApplicationException(
						"cannot initialize ORM Engine [" + getORMEngineClassDefintion() + "], make sure you have added all the required jar files");
				ExceptionUtil.initCauseEL(ae, t);
				throw ae;

			}
			ormengines.put(name, engine);
			/*
			 * } catch (PageException pe) { throw pe; }
			 */
		}

		return engine;
	}

	@Override
	public boolean hasORMEngine() {
		return getORMEngineClassDefintion().equals(ConfigFactoryImpl.DUMMY_ORM_ENGINE);
	}

	@Override
	public ClassDefinition<? extends ORMEngine> getORMEngineClassDefintion() {
		if (cdORMEngine == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getORMEngineClassDefintion")) {
				if (cdORMEngine == null) {
					cdORMEngine = ConfigFactoryImpl.loadORMClass(this, root);
				}
			}
		}
		return cdORMEngine;
	}

	public ConfigImpl resetORMEngineClassDefintion() {
		if (cdORMEngine != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getORMEngineClassDefintion")) {
				if (cdORMEngine != null) {
					cdORMEngine = null;
				}
			}
		}
		return this;
	}

	public ClassDefinition<? extends ORMEngine> getORMEngineClass() {
		return getORMEngineClassDefintion();
	}

	@Override
	public ORMConfiguration getORMConfig() {
		if (ormConfig == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getORMConfig")) {
				if (ormConfig == null) {
					ormConfig = ConfigFactoryImpl.loadORMConfig(this, root, null);
				}
			}
		}
		return ormConfig;
	}

	public ConfigImpl resetORMConfig() {
		if (ormConfig != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getORMConfig")) {
				if (ormConfig != null) {
					ormConfig = null;
				}
			}
		}
		return this;
	}

	private Map<String, SoftReference<ConfigUtil.CacheElement>> applicationPathCache = null;// new ArrayList<Page>();
	private Map<String, SoftReference<InitFile>> ctPatchCache = null;// new ArrayList<Page>();
	private Map<String, SoftReference<UDF>> udfCache = new ConcurrentHashMap<String, SoftReference<UDF>>();

	@Override
	public CIPage getCachedPage(PageContext pc, String pathWithCFC) throws PageException {
		return componentPathCache.getPage(pc, pathWithCFC);
	}

	@Override
	public void putCachedPageSource(String pathWithCFC, PageSource ps) {
		componentPathCache.put(pathWithCFC, ps);
	}

	@Override
	public PageSource getApplicationPageSource(PageContext pc, String path, String filename, int mode, RefBoolean isCFC) {
		if (applicationPathCache == null) return null;
		String id = (path + ":" + filename + ":" + mode).toLowerCase();

		SoftReference<CacheElement> tmp = getApplicationPathCacheTimeout() <= 0 ? null : applicationPathCache.get(id);
		if (tmp != null) {
			CacheElement ce = tmp.get();
			if (ce != null && (ce.created + getApplicationPathCacheTimeout()) >= System.currentTimeMillis()) {
				if (ce.pageSource.loadPage(pc, false, (Page) null) != null) {
					if (isCFC != null) isCFC.setValue(ce.isCFC);
					return ce.pageSource;
				}
			}
		}
		return null;
	}

	@Override
	public void putApplicationPageSource(String path, PageSource ps, String filename, int mode, boolean isCFC) {
		if (getApplicationPathCacheTimeout() <= 0) return;
		if (applicationPathCache == null) applicationPathCache = new ConcurrentHashMap<String, SoftReference<CacheElement>>();// MUSTMUST new
		String id = (path + ":" + filename + ":" + mode).toLowerCase();
		applicationPathCache.put(id, new SoftReference<CacheElement>(new CacheElement(ps, isCFC)));
	}

	@Override
	public long getApplicationPathCacheTimeout() {
		if (applicationPathCacheTimeout == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getApplicationPathCacheTimeout")) {
				if (applicationPathCacheTimeout == null) {
					applicationPathCacheTimeout = metaApplicationPathCacheTimeout.get(this, root).getMillis();
				}
			}
		}
		return applicationPathCacheTimeout;
	}

	public ConfigImpl resetApplicationPathCacheTimeout() {
		if (applicationPathCacheTimeout != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getApplicationPathCacheTimeout")) {
				if (applicationPathCacheTimeout != null) {
					applicationPathCacheTimeout = null;
				}
			}
		}
		return this;
	}

	@Override
	public InitFile getCTInitFile(PageContext pc, String key) {
		if (ctPatchCache == null) return null;

		SoftReference<InitFile> tmp = ctPatchCache.get(key.toLowerCase());
		InitFile initFile = tmp == null ? null : tmp.get();
		if (initFile != null) {
			if (MappingImpl.isOK(initFile.getPageSource())) return initFile;
			ctPatchCache.remove(key.toLowerCase());
		}
		return null;
	}

	@Override
	public void putCTInitFile(String key, InitFile initFile) {
		if (ctPatchCache == null) ctPatchCache = new ConcurrentHashMap<String, SoftReference<InitFile>>();// MUSTMUST new ReferenceMap(ReferenceMap.SOFT,ReferenceMap.SOFT);
		ctPatchCache.put(key.toLowerCase(), new SoftReference<InitFile>(initFile));
	}

	@Override
	public Struct listCTCache() {
		Struct sct = new StructImpl();
		if (ctPatchCache == null) return sct;
		Iterator<Entry<String, SoftReference<InitFile>>> it = ctPatchCache.entrySet().iterator();

		Entry<String, SoftReference<InitFile>> entry;
		SoftReference<InitFile> v;
		InitFile initFile;
		while (it.hasNext()) {
			entry = it.next();
			v = entry.getValue();
			if (v != null) {
				initFile = v.get();
				if (initFile != null) sct.setEL(entry.getKey(), initFile.getPageSource().getDisplayPath());
			}
		}
		return sct;
	}

	@Override
	public void clearCTCache() {
		if (ctPatchCache == null) return;
		ctPatchCache.clear();
	}

	@Override
	public void clearFunctionCache() {
		udfCache.clear();
	}

	@Override
	public UDF getFromFunctionCache(String key) {
		SoftReference<UDF> tmp = udfCache.get(key);
		if (tmp == null) return null;
		return tmp.get();
	}

	@Override
	public void putToFunctionCache(String key, UDF udf) {
		udfCache.put(key, new SoftReference<UDF>(udf));
	}

	@Override
	public Struct listComponentCache() {
		return componentPathCache.list();
	}

	@Override
	public void clearComponentCache() {
		componentPathCache.clear();
	}

	@Override
	public void clearApplicationCache() {
		if (applicationPathCache == null) return;
		applicationPathCache.clear();
	}

	@Override
	public ImportDefintion getComponentDefaultImport() {
		if (componentDefaultImport == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getComponentDefaultImport")) {
				if (componentDefaultImport == null) {
					componentDefaultImport = ImportDefintionImpl.getInstance(metaComponentDefaultImport.get(this, root), DEFAULT_IMPORT_DEFINITION);
				}
			}
		}
		return componentDefaultImport;
	}

	public ConfigImpl resetComponentDefaultImport() {
		if (componentDefaultImport != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getComponentDefaultImport")) {
				if (componentDefaultImport != null) {
					this.componentDefaultImport = null;
				}
			}
		}
		return this;
	}

	protected void setComponentDefaultImport(String str) {

	}

	/**
	 * @return the componentLocalSearch
	 */
	@Override
	public boolean getComponentLocalSearch() {
		if (componentLocalSearch == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getComponentLocalSearch")) {
				if (componentLocalSearch == null) {
					componentLocalSearch = metaComponentLocalSearch.get(this, root);
				}
			}
		}
		return componentLocalSearch;
	}

	public ConfigImpl resetComponentLocalSearch() {
		if (componentLocalSearch != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getComponentLocalSearch")) {
				if (componentLocalSearch != null) {
					componentLocalSearch = null;
				}
			}
		}
		return this;
	}
	// = true

	/**
	 * @return the componentLocalSearch
	 */
	@Override
	public boolean getComponentRootSearch() {
		return componentRootSearch;
	}

	/**
	 * @param componentRootSearch the componentLocalSearch to set
	 */
	protected void setComponentRootSearch(boolean componentRootSearch) {
		this.componentRootSearch = componentRootSearch;
	}

	@Override
	public boolean getSessionCluster() {
		return false;
	}

	@Override
	public boolean getClientCluster() {
		return false;
	}

	@Override
	public String getClientStorage() {
		if (clientStorage == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "")) {
				if (clientStorage == null) {
					clientStorage = metaClientStorage.get(this, root);
				}
			}
		}
		return clientStorage;
	}

	public ConfigImpl resetClientStorage() {
		if (clientStorage != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "")) {
				if (clientStorage != null) {
					clientStorage = null;
				}
			}
		}
		return this;
	}

	@Override
	public String getSessionStorage() {
		if (sessionStorage == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getSessionStorage")) {
				if (sessionStorage == null) {
					sessionStorage = metaSessionStorage.get(this, root);
				}
			}
		}
		return sessionStorage;
	}

	public ConfigImpl resetSessionStorage() {
		if (sessionStorage != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getSessionStorage")) {
				if (sessionStorage != null) {
					sessionStorage = null;
				}
			}
		}
		return this;
	}

	private Map<String, ComponentMetaData> componentMetaData = null;

	public ComponentMetaData getComponentMetadata(String key) {
		if (componentMetaData == null) return null;
		return componentMetaData.get(key.toLowerCase());
	}

	public void putComponentMetadata(String key, ComponentMetaData data) {
		if (componentMetaData == null) componentMetaData = new HashMap<String, ComponentMetaData>();
		componentMetaData.put(key.toLowerCase(), data);
	}

	public void clearComponentMetadata() {
		if (componentMetaData == null) return;
		componentMetaData.clear();
	}

	private DebugEntry[] debugEntries;

	@Override
	public DebugEntry[] getDebugEntries() {
		if (debugEntries == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getDebugEntries")) {
				if (debugEntries == null) {
					Array entries = ConfigUtil.getAsArray("debugTemplates", root);
					Map<String, DebugEntry> list = new HashMap<String, DebugEntry>();

					String id;
					if (entries != null) {
						Iterator<?> it = entries.getIterator();
						Struct e;
						while (it.hasNext()) {
							try {
								e = Caster.toStruct(it.next(), null);
								if (e == null) continue;
								id = ConfigFactoryImpl.getAttr(this, e, "id");
								list.put(id,
										new DebugEntry(id, ConfigFactoryImpl.getAttr(this, e, "type"), ConfigFactoryImpl.getAttr(this, e, "iprange"),
												ConfigFactoryImpl.getAttr(this, e, "label"), ConfigFactoryImpl.getAttr(this, e, "path"),
												ConfigFactoryImpl.getAttr(this, e, "fullname"), ConfigUtil.getAsStruct(this, e, true, "custom")));
							}
							catch (Throwable t) {
								ExceptionUtil.rethrowIfNecessary(t);
								ConfigFactoryImpl.log(this, t);
							}
						}
					}
					debugEntries = list.values().toArray(new DebugEntry[list.size()]);
				}
			}
		}
		return debugEntries;
	}

	public ConfigImpl resetDebugEntries() {
		if (debugEntries != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getDebugEntries")) {
				if (debugEntries != null) {
					debugEntries = null;
				}
			}
		}
		return this;
	}

	@Override
	public DebugEntry getDebugEntry(String ip, DebugEntry defaultValue) {
		DebugEntry[] debugEntries = getDebugEntries();
		if (debugEntries.length == 0) return defaultValue;
		InetAddress ia;

		try {
			ia = IPRange.toInetAddress(ip);
		}
		catch (IOException e) {
			return defaultValue;
		}

		for (int i = 0; i < debugEntries.length; i++) {
			if (debugEntries[i].getIpRange().inRange(ia)) return debugEntries[i];
		}
		return defaultValue;
	}

	// debugMaxRecordsLogged = 10

	@Override
	public int getDebugMaxRecordsLogged() {
		if (debugMaxRecordsLogged == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getDebugMaxRecordsLogged")) {
				if (debugMaxRecordsLogged == null) {
					debugMaxRecordsLogged = metaDebugMaxRecordsLogged.get(this, root);
				}
			}
		}
		return debugMaxRecordsLogged;
	}

	public ConfigImpl resetDebugMaxRecordsLogged() {
		if (debugMaxRecordsLogged != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getDebugMaxRecordsLogged")) {
				if (debugMaxRecordsLogged != null) {
					debugMaxRecordsLogged = null;
				}
			}
		}
		return this;
	}

	private Boolean dotNotationUpperCase;

	@Override
	public boolean getDotNotationUpperCase() {
		if (dotNotationUpperCase == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getDotNotationUpperCase")) {
				if (dotNotationUpperCase == null) {
					Boolean tmp = Caster.toBoolean(SystemUtil.getSystemPropOrEnvVar("lucee.preserve.case", null), null);
					if (tmp != null) tmp = !tmp; // invert: lucee.preserve.case=true means dotNotationUpperCase=false
					if (tmp == null) tmp = Caster.toBoolean(ConfigFactoryImpl.getAttr(this, root, "dotNotationUpperCase"), null);
					if (tmp == null) {
						tmp = Caster.toBoolean(ConfigFactoryImpl.getAttr(this, root, "preserveCase"), null);
						if (tmp != null) tmp = !tmp;
					}
					if (tmp == null) dotNotationUpperCase = Boolean.TRUE;
					else dotNotationUpperCase = tmp;
				}
			}
		}
		return dotNotationUpperCase;
	}

	public ConfigImpl resetDotNotationUpperCase() {
		if (dotNotationUpperCase != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getDotNotationUpperCase")) {
				if (dotNotationUpperCase != null) {
					dotNotationUpperCase = null;
				}
			}
		}
		return this;
	}

	@Override
	public boolean preserveCase() {
		return !getDotNotationUpperCase();
	}

	private Boolean defaultFunctionOutput;

	@Override
	public boolean getDefaultFunctionOutput() {
		if (defaultFunctionOutput == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getDefaultFunctionOutput")) {
				if (defaultFunctionOutput == null) {
					String output = ConfigFactoryImpl.getAttr(this, root, "defaultFunctionOutput");
					defaultFunctionOutput = Caster.toBooleanValue(output, true);
				}
			}
		}
		return defaultFunctionOutput;
	}

	public ConfigImpl restDefaultFunctionOutput() {
		if (defaultFunctionOutput != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getDefaultFunctionOutput")) {
				if (defaultFunctionOutput != null) {
					defaultFunctionOutput = null;
				}
			}
		}
		return this;
	}

	private Boolean getSuppressWSBeforeArg;

	@Override
	public boolean getSuppressWSBeforeArg() {
		if (getSuppressWSBeforeArg == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getSuppressWSBeforeArg")) {
				if (getSuppressWSBeforeArg == null) {
					String suppress = SystemUtil.getSystemPropOrEnvVar("lucee.suppress.ws.before.arg", null);
					if (StringUtil.isEmpty(suppress, true)) {
						suppress = ConfigFactoryImpl.getAttr(this, root, new String[] { "suppressWhitespaceBeforeArgument", "suppressWhitespaceBeforecfargument" });
					}
					getSuppressWSBeforeArg = Caster.toBooleanValue(suppress, true);
				}
			}
		}
		return getSuppressWSBeforeArg;
	}

	public ConfigImpl restSuppressWSBeforeArg() {
		if (getSuppressWSBeforeArg != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getSuppressWSBeforeArg")) {
				if (getSuppressWSBeforeArg != null) {
					getSuppressWSBeforeArg = null;
				}
			}
		}
		return this;
	}

	private RestSettings restSetting = new RestSettingImpl(false, UDF.RETURN_FORMAT_JSON);

	protected void setRestSetting(RestSettings restSetting) {
		this.restSetting = restSetting;
	}

	@Override
	public RestSettings getRestSetting() {
		return restSetting;
	}

	public int getMode() {
		if (mode == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getMode")) {
				if (mode == null) {
					String str = ConfigFactoryImpl.getAttr(this, root, "mode");
					if (!StringUtil.isEmpty(str, true)) {
						str = str.trim();
						if ("custom".equalsIgnoreCase(str)) mode = ConfigPro.MODE_CUSTOM;
						else if ("strict".equalsIgnoreCase(str)) mode = ConfigPro.MODE_STRICT;
						else mode = ConfigPro.MODE_CUSTOM;
					}
					else mode = ConfigPro.MODE_CUSTOM;
				}
			}
		}
		return mode;
	}

	public ConfigImpl resetMode() {
		if (mode != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getMode")) {
				if (mode != null) {
					mode = null;
				}
			}
		}
		return this;
	}

	// do not move to Config interface, do instead setCFMLWriterClass
	@Override
	public int getCFMLWriterType() {
		if (writerType == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCFMLWriterType")) {
				if (writerType == null) {
					writerType = metaWriterType.get(this, root);
				}
			}
		}
		return writerType;
	}

	public ConfigImpl resetCFMLWriterType() {
		if (writerType != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCFMLWriterType")) {
				if (writerType != null) {
					writerType = null;
				}
			}
		}
		return this;
	}

	private Boolean bufferOutput;

	private Integer externalizeStringGTE;
	private Map<String, BundleDefinition> extensionBundles;
	private JDBCDriver[] drivers;
	private Resource logDir;

	@Override
	public boolean getBufferOutput() {
		if (bufferOutput == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getBufferOutput")) {
				if (bufferOutput == null) {
					String str = ConfigFactoryImpl.getAttr(this, root, "bufferTagBodyOutput");
					if (!StringUtil.isEmpty(str, true)) {
						bufferOutput = Caster.toBoolean(str.trim(), DEFAULT_BUFFER_TAG_BODY_OUTPUT);
					}
					else bufferOutput = DEFAULT_BUFFER_TAG_BODY_OUTPUT;
				}
			}
		}
		return bufferOutput;
	}

	public ConfigImpl resetBufferOutput() {
		if (bufferOutput != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getBufferOutput")) {
				if (bufferOutput != null) {
					bufferOutput = null;
				}
			}
		}
		return this;
	}

	public int getDebugOptions() {
		if (debugOptions == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getDebugOptions")) {
				if (debugOptions == null) {

					int options = 0;
					if (metaDebugOptionsDatabase.get(this, root)) options += ConfigPro.DEBUG_DATABASE;
					if (metaDebugOptionsException.get(this, root)) options += ConfigPro.DEBUG_EXCEPTION;
					if (metaDebugOptionsTemplate.get(this, root)) options += ConfigPro.DEBUG_TEMPLATE;
					if (metaDebugOptionsDump.get(this, root)) options += ConfigPro.DEBUG_DUMP;
					if (metaDebugOptionsTracing.get(this, root)) options += ConfigPro.DEBUG_TRACING;
					if (metaDebugOptionsTimer.get(this, root)) options += ConfigPro.DEBUG_TIMER;
					if (metaDebugOptionsImplicitAccess.get(this, root)) options += ConfigPro.DEBUG_IMPLICIT_ACCESS;
					if (metaDebugOptionsQueryUsage.get(this, root)) options += ConfigPro.DEBUG_QUERY_USAGE;
					if (metaDebugOptionsThread.get(this, root)) options += ConfigPro.DEBUG_THREAD;

					debugOptions = options;
				}
			}
		}
		return debugOptions;
	}

	public ConfigImpl resetDebugOptions() {
		if (debugOptions != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getDebugOptions")) {
				if (debugOptions != null) {
					debugOptions = null;
				}
			}
		}
		return this;
	}
	// = 0

	@Override
	public boolean hasDebugOptions(int debugOption) {
		return (getDebugOptions() & debugOption) > 0;
	}

	@Override
	public boolean checkForChangesInConfigFile() {
		if (checkForChangesInConfigFile == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "checkForChangesInConfigFile")) {
				if (checkForChangesInConfigFile == null) {
					checkForChangesInConfigFile = metaCheckForChangesInConfigFile.get(this, root);
				}
			}
		}
		return checkForChangesInConfigFile;
	}

	public ConfigImpl resetCheckForChangesInConfigFile() {
		if (checkForChangesInConfigFile != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "checkForChangesInConfigFile")) {
				if (checkForChangesInConfigFile != null) {
					checkForChangesInConfigFile = null;
				}
			}
		}
		return this;
	}

	@Override
	public int getExternalizeStringGTE() {
		if (externalizeStringGTE == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getExternalizeStringGTE")) {
				if (externalizeStringGTE == null) {
					String str = ConfigFactoryImpl.getAttr(this, root, "externalizeStringGte");
					if (Decision.isNumber(str)) {
						externalizeStringGTE = Caster.toIntValue(str, -1);
					}
					else externalizeStringGTE = -1;
				}
			}
		}
		return externalizeStringGTE;
	}

	public ConfigImpl resetExternalizeStringGTE() {
		if (externalizeStringGTE != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getExternalizeStringGTE")) {
				if (externalizeStringGTE != null) {
					externalizeStringGTE = null;
				}
			}
		}
		return this;
	}

	protected void addConsoleLayout(Object layout) {
		consoleLayouts.add(layout);

	}

	protected void addResourceLayout(Object layout) {
		resourceLayouts.add(layout);
	}

	public Object[] getConsoleLayouts() throws PageException {
		if (consoleLayouts.isEmpty()) consoleLayouts.add(getLogEngine().getDefaultLayout());
		return consoleLayouts.toArray(new Object[consoleLayouts.size()]);

	}

	public Object[] getResourceLayouts() throws PageException {
		if (resourceLayouts.isEmpty()) resourceLayouts.add(getLogEngine().getClassicLayout());
		return resourceLayouts.toArray(new Object[resourceLayouts.size()]);
	}

	protected void clearLoggers(Boolean dyn) {
		if (loggers == null || loggers.size() == 0) return;
		synchronized (SystemUtil.createToken("ConfigImpl", "loggers")) {
			List<String> list = dyn != null ? new ArrayList<String>() : null;
			try {
				Iterator<Entry<String, LoggerAndSourceData>> it = loggers.entrySet().iterator();
				Entry<String, LoggerAndSourceData> e;
				while (it.hasNext()) {
					e = it.next();
					if (dyn == null || dyn.booleanValue() == e.getValue().getDyn()) {
						e.getValue().close();
						if (list != null) list.add(e.getKey());
					}

				}
			}
			catch (Exception e) {}

			if (list == null) loggers.clear();
			else {
				Iterator<String> it = list.iterator();
				while (it.hasNext()) {
					loggers.remove(it.next());
				}
			}
			loggers = null;
		}
	}

	public Map<String, LoggerAndSourceData> getLoggers() {
		if (loggers == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "loggers")) {
				if (loggers == null) {
					if (root == null || insideLoggers.get()) {
						return new HashMap<String, LoggerAndSourceData>(); // avoid cycle loop
					}
					insideLoggers.set(true);
					try {
						loggers = metaLoggers.map(this, root);
					}
					finally {
						insideLoggers.set(false);
					}
				}
			}
		}
		return loggers;
	}

	public Map<String, LoggerAndSourceData> resetLoggers() {
		if (loggers != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "loggers")) {
				if (loggers != null) {
					loggers = null;
				}
			}
		}
		return loggers;
	}

	@Override
	public String[] getLogNames() {
		Set<String> keys = getLoggers().keySet();
		return keys.toArray(new String[keys.size()]);
	}

	@Override
	public Log getLog(String name) {
		try {
			return getLog(name, true);
		}
		catch (PageException e) {
			throw new PageRuntimeException(e);
		}
	}

	@Override
	public Log getLog(String name, boolean createIfNecessary) throws PageException {
		if (root == null) return null;
		LoggerAndSourceData lsd = _getLoggerAndSourceData(name, createIfNecessary);
		if (lsd == null) return null;
		return lsd.getLog(false);
	}

	@Override
	public boolean isLoggingLoaded() {
		return loggers != null;
	}

	private LoggerAndSourceData _getLoggerAndSourceData(String name, boolean createIfNecessary) throws PageException {
		LoggerAndSourceData las = getLoggers().get(name.toLowerCase());
		if (las == null) {
			if (!createIfNecessary) return null;

			ClassDefinition appender = getLogEngine().appenderClassDefintion("console");
			ClassDefinition layout = getLogEngine().layoutClassDefintion("pattern");
			las = ConfigFactoryImpl.createLogger(this, name, Log.LEVEL_ERROR, appender, null, layout, null, true, true);

			String id = LoggerAndSourceData.id(name.toLowerCase(), appender, null, layout, null, Log.LEVEL_ERROR, true);
			LoggerAndSourceData existing = loggers.get(name.toLowerCase());
			if (existing != null) {
				if (existing.id().equals(id)) {
					return existing;
				}
				existing.close();
			}

		}
		return las.init();
	}

	@Override
	public Map<Key, Map<Key, Object>> getTagDefaultAttributeValues() {
		return tagDefaultAttributeValues == null ? null : Duplicator.duplicateMap(tagDefaultAttributeValues, new ConcurrentHashMap<Key, Map<Key, Object>>(), true);
	}

	protected void setTagDefaultAttributeValues(Map<Key, Map<Key, Object>> values) {
		this.tagDefaultAttributeValues = values;
	}

	@Override
	public Boolean getHandleUnQuotedAttrValueAsString() {
		if (handleUnQuotedAttrValueAsString == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getHandleUnQuotedAttrValueAsString")) {
				if (handleUnQuotedAttrValueAsString == null) {
					handleUnQuotedAttrValueAsString = metaHandleUnquotedAttributeValueAsString.get(this, root);
				}
			}
		}
		return handleUnQuotedAttrValueAsString;
	}

	public ConfigImpl resetHandleUnQuotedAttrValueAsString() {
		if (handleUnQuotedAttrValueAsString != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getHandleUnQuotedAttrValueAsString")) {
				if (handleUnQuotedAttrValueAsString != null) {
					handleUnQuotedAttrValueAsString = null;
				}
			}
		}
		return this;
	}

	@Override
	public Object getCachedWithin(int type) {
		if (cachedWithins == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCachedWithin")) {
				if (cachedWithins == null) {
					HashMap<Integer, Object> map = new HashMap<Integer, Object>();
					for (int i = 0; i < ConfigPro.CACHE_TYPES.length; i++) {
						try {
							String cw = ConfigFactoryImpl.getAttr(this, root, "cachedWithin" + StringUtil.ucFirst(ConfigPro.STRING_CACHE_TYPES[i]));
							if (!StringUtil.isEmpty(cw, true)) map.put(ConfigPro.CACHE_TYPES[i], cw.trim());
						}
						catch (Throwable t) {
							ExceptionUtil.rethrowIfNecessary(t);
							ConfigFactoryImpl.log(this, t);
						}
					}
					cachedWithins = map;
				}
			}
		}
		return cachedWithins.get(type);
		// = new HashMap<Integer, Object>()
	}

	public ConfigImpl resetCachedWithin() {
		if (cachedWithins != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCachedWithin")) {
				if (cachedWithins != null) {
					cachedWithins = null;
				}
			}
		}
		return this;
	}

	@Override
	public Resource getPluginDirectory() {
		return getConfigDir().getRealResource("context/admin/plugin");
	}

	@Override
	public Resource getLogDirectory() {
		if (logDir == null) {
			logDir = getConfigDir().getRealResource("logs");
			logDir.mkdir();
		}
		return logDir;
	}

	@Override
	public String getSalt() {
		salt = null;// TEST PW
		if (salt == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getSalt")) {
				if (salt == null) {
					this.salt = metaSalt.get(this, root);
					if (StringUtil.isEmpty(this.salt, true)) throw new RuntimeException("context is invalid, there is no salt!");

				}
			}
		}
		return salt;
	}

	public ConfigImpl resetSalt() {
		if (salt != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getSalt")) {
				if (salt != null) {
					salt = null;
				}
			}
		}
		return this;
	}

	@Override
	public int getPasswordType() {
		if (getPassword() == null) return Password.HASHED_SALTED;// when there is no password, we will have a HS password
		return getPassword().getType();
	}

	@Override
	public String getPasswordSalt() {
		if (getPassword() == null || getPassword().getSalt() == null) return getSalt();
		return getPassword().getSalt();
	}

	@Override
	public int getPasswordOrigin() {
		if (getPassword() == null) return Password.ORIGIN_UNKNOW;
		return getPassword().getOrigin();
	}

	@Override
	public Collection<BundleDefinition> getExtensionBundleDefintions() {
		if (this.extensionBundles == null) {
			RHExtension[] rhes = getRHExtensions();
			Map<String, BundleDefinition> extensionBundles = new HashMap<String, BundleDefinition>();

			for (RHExtension rhe: rhes) {
				BundleInfo[] bis;
				try {
					bis = rhe.getMetadata().getBundles();
				}
				catch (Exception e) {
					continue;
				}
				if (bis != null) {
					for (BundleInfo bi: bis) {
						extensionBundles.put(bi.getSymbolicName() + "|" + bi.getVersionAsString(), bi.toBundleDefinition());
					}
				}
			}
			this.extensionBundles = extensionBundles;
		}
		return extensionBundles.values();
	}

	@Override
	public JDBCDriver[] getJDBCDrivers() {
		if (drivers == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getJDBCDrivers")) {
				if (drivers == null) {
					drivers = ConfigFactoryImpl.loadJDBCDrivers(this, root);
				}
			}
		}
		return drivers;
	}

	public ConfigImpl resetJDBCDrivers() {
		if (drivers != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getJDBCDrivers")) {
				if (drivers != null) {
					drivers = null;
				}
			}
		}
		return this;
	}

	@Override
	public JDBCDriver getJDBCDriverByClassName(String className, JDBCDriver defaultValue) {
		for (JDBCDriver d: getJDBCDrivers()) {
			if (d.cd.getClassName().equals(className)) return d;
		}
		return defaultValue;
	}

	@Override
	public JDBCDriver getJDBCDriverById(String id, JDBCDriver defaultValue) {
		if (!StringUtil.isEmpty(id)) {
			for (JDBCDriver d: getJDBCDrivers()) {
				if (d.id != null && d.id.equalsIgnoreCase(id)) return d;
			}
		}
		return defaultValue;
	}

	@Override
	public JDBCDriver getJDBCDriverByBundle(String bundleName, Version version, JDBCDriver defaultValue) {
		for (JDBCDriver d: getJDBCDrivers()) {
			if (d.cd.getName().equals(bundleName) && (version == null || version.equals(d.cd.getVersion()))) return d;
		}
		return defaultValue;
	}

	@Override
	public JDBCDriver getJDBCDriverByCD(ClassDefinition cd, JDBCDriver defaultValue) {
		for (JDBCDriver d: getJDBCDrivers()) {
			if (d.cd.getId().equals(cd.getId())) return d; // TODO comparing cd objects directly?
		}
		return defaultValue;
	}

	@Override
	public int getQueueMax() {
		if (queueMax == -1) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getQueueMax")) {
				if (queueMax == -1) {
					queueMax = metaQueueMax.get(this, root);
				}
			}
		}
		return queueMax;
	}

	public ConfigImpl resetQueueMax() {
		if (queueMax != -1) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getQueueMax")) {
				if (queueMax != -1) {
					queueMax = -1;
				}
			}
		}
		return this;
	}

	@Override
	public long getQueueTimeout() {
		if (queueTimeout == -1) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getQueueTimeout")) {
				if (queueTimeout == -1) {
					queueTimeout = metaQueueTimeout.get(this, root);
					;
				}
			}
		}
		return queueTimeout;
	}

	public ConfigImpl resetQueueTimeout() {
		if (queueTimeout != -1) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getQueueTimeout")) {
				if (queueTimeout != -1) {
					queueTimeout = -1;
				}
			}
		}
		return this;
	}

	@Override
	public boolean getQueueEnable() {
		if (queueEnable == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getQueueEnable")) {
				if (queueEnable == null) {
					queueEnable = metaQueueEnable.get(this, root);
				}
			}
		}
		return queueEnable;
	}

	public ConfigImpl resetQueueEnable() {
		if (queueEnable != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getQueueEnable")) {
				if (queueEnable != null) {
					queueEnable = null;
				}
			}
		}
		return this;
	}

	@Override
	public boolean getCGIScopeReadonly() {
		if (cgiScopeReadonly == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCGIScopeReadonly")) {
				if (cgiScopeReadonly == null) {
					cgiScopeReadonly = metaCgiScopeReadonly.get(this, root);
				}
			}
		}
		return cgiScopeReadonly;
	}

	public ConfigImpl resetCGIScopeReadonly() {
		if (cgiScopeReadonly != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCGIScopeReadonly")) {
				if (cgiScopeReadonly != null) {
					cgiScopeReadonly = null;
				}
			}
		}
		return this;
	}

	@Override
	public Resource getDeployDirectory() {
		if (deployDir == null) {
			// config web
			if (this instanceof ConfigWeb) {
				deployDir = getConfigDir().getRealResource("deploy");
				if (!deployDir.exists()) deployDir.mkdirs();
			}
			// config server
			else {
				try {
					File file = new File(ConfigUtil.getCFMLEngineFactory(this).getResourceRoot(), "deploy");
					if (!file.exists()) file.mkdirs();
					deployDir = ResourcesImpl.getFileResourceProvider().getResource(file.getAbsolutePath());
				}
				catch (IOException ioe) {
					deployDir = getConfigDir().getRealResource("deploy");
					if (!deployDir.exists()) deployDir.mkdirs();
				}
			}
		}
		return deployDir;
	}

	@Override
	@Deprecated
	public Map<String, ClassDefinition> getCacheDefinitions() {
		if (cacheDefinitions == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCacheDefinitions")) {
				if (cacheDefinitions == null) {
					List<ClassDefinition> list = metacCacheDefinitions.list(this, root);
					Map<String, ClassDefinition> map = new HashMap<String, ClassDefinition>();
					for (ClassDefinition cd: list) {
						map.put(cd.getClassName(), cd);
					}
					cacheDefinitions = map;
				}
			}
		}
		return cacheDefinitions;
	}

	public ConfigImpl resetCacheDefinitions() {
		if (cacheDefinitions != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCacheDefinitions")) {
				if (cacheDefinitions != null) {
					cacheDefinitions = null;
				}
			}
		}
		return this;
	}

	@Override
	public ClassDefinition getCacheDefinition(String className) {
		return getCacheDefinitions().get(className);
	}

	@Override
	public Resource getAntiSamyPolicy() {
		if (antiSamyPolicy == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getAntiSamyPolicy")) {
				if (antiSamyPolicy == null) {

					Resource secDir = getConfigDir().getRealResource("security");
					antiSamyPolicy = getConfigDir().getRealResource("antisamy-basic.xml");
					if (!antiSamyPolicy.exists() || newVersion) {
						if (!secDir.exists()) secDir.mkdirs();
						ConfigFactoryImpl.createFileFromResourceEL("/resource/security/antisamy-basic.xml", antiSamyPolicy);
					}

				}
			}
		}
		return antiSamyPolicy;
	}

	public GatewayMap getGatewayEntries() {
		if (gatewayEntries == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getGatewayEntries")) {
				if (gatewayEntries == null) {
					gatewayEntries = (GatewayMap) metaGatewayEntries.map(this, root, new GatewayMap());
				}
			}
		}
		return gatewayEntries;
	}

	public ConfigImpl resetGatewayEntries() {
		if (gatewayEntries != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getGatewayEntries")) {
				if (gatewayEntries != null) {
					gatewayEntries = null;
				}
			}
		}
		return this;
	}

	private ClassDefinition wsHandlerCD;
	private boolean initWsHandlerCD = true;

	protected ClassDefinition getWSHandlerClassDefinition() {
		if (initWsHandlerCD) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getWSHandlerClassDefinition")) {
				if (initWsHandlerCD) {
					wsHandlerCD = ConfigFactoryImpl.loadWS(this, root, null);
					initWsHandlerCD = false;
				}
			}
		}
		return wsHandlerCD;
	}

	protected ConfigImpl resetWSHandlerClassDefinition() {
		if (!initWsHandlerCD) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getWSHandlerClassDefinition")) {
				if (!initWsHandlerCD) {
					wsHandlerCD = null;
					initWsHandlerCD = true;
				}
			}
		}
		return this;
	}

	boolean isEmpty(ClassDefinition cd) {
		return cd == null || StringUtil.isEmpty(cd.getClassName());
	}

	@Override
	public final boolean getFullNullSupport() {
		if (fullNullSupport == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getFullNullSupport")) {
				if (fullNullSupport == null) {
					fullNullSupport = metafullNullSupport.get(this, root);
				}
			}
		}
		return fullNullSupport;
	}

	public final ConfigImpl resetFullNullSupport() {
		if (fullNullSupport != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getFullNullSupport")) {
				if (fullNullSupport != null) {
					fullNullSupport = null;
				}
			}
		}
		return this;
	}

	private static LogEngine logEngine;

	@Override
	public LogEngine getLogEngine() {
		if (logEngine == null) {
			synchronized (token) {
				if (logEngine == null) {
					logEngine = LogEngine.newInstance(this);
				}
			}

		}
		return logEngine;
	}

	@Override
	public TimeSpan getCachedAfterTimeRange() {
		if (initCachedAfterTimeRange) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCachedAfterTimeRange")) {
				if (initCachedAfterTimeRange) {

					TimeSpan ts = metaCachedAfterTimeRange.get(this, root);
					if (ts != null && ts.getMillis() > 0) cachedAfterTimeRange = ts;
					initCachedAfterTimeRange = false;
				}
			}
		}
		return this.cachedAfterTimeRange;
	}

	public ConfigImpl resetCachedAfterTimeRange() {
		if (!initCachedAfterTimeRange) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getCachedAfterTimeRange")) {
				if (!initCachedAfterTimeRange) {
					cachedAfterTimeRange = null;
					initCachedAfterTimeRange = true;
				}
			}
		}
		return this;
	}

	@Override
	public Map<String, Startup> getStartups() {
		if (startups == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getStartups")) {
				if (startups == null) {
					startups = ConfigFactoryImpl.loadStartupHook(this, root);
				}
			}
		}
		return startups;
	}

	public ConfigImpl resetStartups() {
		if (startups != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getStartups")) {
				if (startups != null) {
					// Call finalize() on existing startup hook instances before clearing
					for (Startup startup: startups.values()) {
						try {
							Method fin = Reflector.getMethod(startup.instance.getClass(), "finalize", new Class[0], true, null);
							if (fin != null) {
								fin.invoke(startup.instance, new Object[0]);
							}
						}
						catch (Exception e) {
							// ignore - best effort cleanup
						}
					}
					startups = null;
				}
			}
		}
		return this;
	}

	@Override
	public Regex getRegex() {
		if (regex == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getRegex")) {
				regex = metaRegex.get(this, root);
			}
		}
		return regex;
	}

	public ConfigImpl resetRegex() {
		if (regex != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getRegex")) {
				if (regex != null) regex = null;
			}
		}
		return this;
	}

	@Override
	public boolean getPreciseMath() {
		if (preciseMath == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getPreciseMath")) {
				if (preciseMath == null) {
					preciseMath = metaPreciseMath.get(this, root);
				}
			}
		}
		return preciseMath;
	}

	public ConfigImpl resetPreciseMath() {
		if (preciseMath != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getPreciseMath")) {
				if (preciseMath != null) {
					preciseMath = null;
				}
			}
		}
		return this;
	}

	protected void setMainLogger(String mainLoggerName) {
		if (!StringUtil.isEmpty(mainLoggerName, true)) this.mainLoggerName = mainLoggerName.trim();
	}

	@Override
	public String getMainLogger() {
		if (mainLoggerName == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getMainLogger")) {
				if (mainLoggerName == null) {
					mainLoggerName = metaMainLoggerName.get(this, root);
				}

			}
		}

		return this.mainLoggerName;
	}

	public ConfigImpl resetMainLogger() {
		if (mainLoggerName != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getMainLogger")) {
				if (mainLoggerName != null) {
					mainLoggerName = null;
				}
			}
		}
		return this;
	}

	@Override
	public boolean getFormUrlAsStruct() {
		if (formUrlAsStruct == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getFormUrlAsStruct")) {
				if (formUrlAsStruct == null) {
					formUrlAsStruct = metaFormUrlAsStruct.get(this, root);
				}
			}
		}
		return formUrlAsStruct;
	}

	// = true
	public ConfigImpl resetFormUrlAsStruct() {
		if (formUrlAsStruct != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getFormUrlAsStruct")) {
				if (formUrlAsStruct != null) {
					formUrlAsStruct = null;
				}
			}
		}
		return this;
	}

	@Override
	public int getReturnFormat() {
		if (returnFormat == null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getReturnFormat")) {
				if (returnFormat == null) {
					returnFormat = metaReturnFormat.get(this, root);
				}
			}
		}
		return returnFormat;
	}

	public ConfigImpl resetReturnFormat() {
		if (returnFormat != null) {
			synchronized (SystemUtil.createToken("ConfigImpl", "getReturnFormat")) {
				if (returnFormat != null) {
					returnFormat = null;
				}
			}
		}
		return this;
	}

	@Override
	public JavaSettings getJavaSettings(String id) {
		return javaSettingsInstances.get(id);
	}

	@Override
	public void setJavaSettings(String id, JavaSettings js) {
		javaSettingsInstances.put(id, js);
	}

	@Override
	public JavaSettings getJavaSettings() {
		if (javaSettings == null) {
			synchronized (javaSettingsInstances) {
				if (javaSettings == null) {
					javaSettings = ConfigFactoryImpl.loadJavaSettings(this, root, null);
					if (javaSettings == null) javaSettings = JavaSettingsImpl.getInstance(this, new StructImpl(), null);
				}
			}
		}
		return javaSettings;
	}

	@Override
	public Resource getExtensionDirectory() {
		return getExtensionInstalledDir();
	}

	@Override
	public Resource getExtensionInstalledDir() {
		if (extInstalled == null) {
			synchronized (SystemUtil.createToken("extensions", "installed")) {
				if (extInstalled == null) {
					extInstalled = getConfigDir().getRealResource("extensions/installed");
					if (!extInstalled.exists()) extInstalled.mkdirs();
				}
			}
		}
		return extInstalled;
	}

	public ConfigImpl resetExtensionInstalledDir() {
		if (extInstalled != null) {
			synchronized (SystemUtil.createToken("extensions", "installed")) {
				if (extInstalled != null) {
					extInstalled = null;
				}
			}
		}
		return this;
	}

	@Override
	public Resource getExtensionAvailableDir() {
		if (extAvailable == null) {
			synchronized (SystemUtil.createToken("extensions", "available")) {
				if (extAvailable == null) {
					extAvailable = getConfigDir().getRealResource("extensions/available");
					if (!extAvailable.exists()) extAvailable.mkdirs();
				}
			}
		}
		return extAvailable;
	}

	public ConfigImpl resetExtensionAvailableDir() {
		if (extAvailable != null) {
			synchronized (SystemUtil.createToken("extensions", "available")) {
				if (extAvailable != null) {
					extAvailable = null;
				}
			}
		}
		return this;
	}

	public boolean newVersion() {
		return newVersion;
	}

	@Override
	public void reset() {
		// resources.reset();
		ormengines.clear();
		clearFunctionCache();
		clearCTCache();
		clearComponentCache();
		clearApplicationCache();
		clearLoggers(null);
		clearComponentMetadata();
		baseComponentPageSource = null;
	}

	public void resetAll() throws IOException {
		resetAll(null);
	}

	public void resetAll(ResetFilter filter) throws IOException {
		List<Method> methods = Reflector.getMethods(this.getClass());
		if (filter == null) {
			for (Method method: methods) {
				if (!method.getName().startsWith("reset") || method.getName().equals("reset") || method.getName().equals("resetAll") || method.getArgumentCount() != 0) continue;
				method.invoke(this);
			}
		}
		else {
			for (Method method: methods) {
				if (method.getArgumentCount() == 0 && filter.allow(method.getName())) method.invoke(this);
			}
		}
	}

	/*
	 * public static void main(String[] args) throws IOException { List<Method> methods; Resource res =
	 * ResourcesImpl.getFileResourceProvider().getResource("/Users/mic/Tmp3/wwwwww");
	 * ResourceUtil.deleteContent(res, null); DynamicInvoker.getInstance(res);
	 * 
	 * methods = Reflector.getMethods(lucee.runtime.config.ConfigWebImpl.class); methods =
	 * Reflector.getMethods(lucee.runtime.config.ConfigServerImpl.class);
	 * 
	 * int max = 5; for (Method method: methods) { if (!method.getName().startsWith("reset") ||
	 * method.getName().equals("reset") || method.getName().equals("resetAll") ||
	 * method.getArgumentCount() != 0) continue; print.e("->" +
	 * method.getDeclaringProviderClassNameWithSameAccess() + ":" +
	 * method.getDeclaringProviderClassName() + ":" + method.getName()); // if (--max == 0) break; } }
	 */
}