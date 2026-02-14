package lucee.runtime.config;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import lucee.commons.io.SystemUtil;
import lucee.commons.io.log.LogUtil;
import lucee.commons.io.res.Resource;
import lucee.commons.lang.ExceptionUtil;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.exp.PageException;
import lucee.runtime.op.Caster;
import lucee.runtime.type.Collection;
import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.Struct;
import lucee.runtime.util.Cast;

public final class CFConfigImport {

	private static Key FROM_CFCONFIG;
	private static Key ACTION;
	private static Key TYPE;
	private static Key PASSWORD;
	private static Key MAPPINGS;
	private static Key DATASOURCES;
	private static Key VIRTUAL;
	private static Key NAME;
	private static Key DATABASE;

	private Resource file;
	private Charset charset;
	private String password;
	// private Tag tag;
	// private DynamicAttributes dynAttr;
	private String type = "server";
	private CFMLEngine engine;
	private final ConfigPro config;
	private Struct placeHolderData;
	private Struct data;
	private boolean pwCheckedServer = false;
	private boolean pwCheckedWeb = false;
	private final boolean setPasswordIfNecessary;
	private final boolean validatePassword;
	private final boolean flushExistingData;
	private PageException exd = null;

	public CFConfigImport(Config config, Resource file, Charset charset, String password, String type, Struct placeHolderData, boolean setPasswordIfNecessary,
			boolean validatePassword, boolean flushExistingData) throws PageException {

		this.file = file;
		this.charset = charset;
		this.password = password;
		this.type = type;
		this.placeHolderData = placeHolderData;
		this.setPasswordIfNecessary = setPasswordIfNecessary;
		this.validatePassword = validatePassword;
		this.flushExistingData = flushExistingData;
		this.engine = CFMLEngineFactory.getInstance();
		if ("web".equalsIgnoreCase(type) && !(config instanceof ConfigWeb))
			throw engine.getExceptionUtil().createApplicationException("cannot manipulate a web context when you pass in a server config to the constructor!");

		if ("server".equalsIgnoreCase(type) && config instanceof ConfigWeb) {
			setPasswordIfNecessary((ConfigPro) config);
			this.config = (ConfigPro) config.getConfigServer(password);
		}
		else this.config = (ConfigPro) config;
	}

	public CFConfigImport(Config config, Struct data, Charset charset, String password, String type, Struct placeHolderData, boolean setPasswordIfNecessary,
			boolean validatePassword, boolean flushExistingData) throws PageException {
		this.data = data;
		this.charset = charset;
		this.password = password;
		this.validatePassword = validatePassword;
		this.type = type;
		this.placeHolderData = placeHolderData;
		this.flushExistingData = flushExistingData;
		this.engine = CFMLEngineFactory.getInstance();
		this.setPasswordIfNecessary = setPasswordIfNecessary;
		if ("web".equalsIgnoreCase(type) && !(config instanceof ConfigWeb))
			throw engine.getExceptionUtil().createApplicationException("cannot manipulate a web context when you pass in a server config to the constructor!");
		if ("server".equalsIgnoreCase(type) && config instanceof ConfigWeb) {
			setPasswordIfNecessary((ConfigPro) config);
			this.config = (ConfigPro) config.getConfigServer(password);
		}
		else this.config = (ConfigPro) config;
	}

	public Struct execute(boolean throwException) throws PageException {
		Struct json = null;

		try {
			if (validatePassword && Util.isEmpty(password)) {
				String sysprop = "lucee." + type.toUpperCase() + ".admin.password";
				String envVarName = sysprop.replace('.', '_').toUpperCase();
				password = SystemUtil.getSystemPropOrEnvVar(sysprop, null);
				if (password == null) throw engine.getExceptionUtil()
						.createApplicationException("missing password to access the Lucee configutation. This can be set in two ways, as enviroment variable [" + envVarName
								+ "] or as system property [" + sysprop + "].");
			}

			Cast cast = engine.getCastUtil();
			if (ACTION == null) ACTION = cast.toKey("action");
			if (TYPE == null) TYPE = cast.toKey("type");
			if (PASSWORD == null) PASSWORD = cast.toKey("password");
			if (MAPPINGS == null) MAPPINGS = cast.toKey("mappings");
			if (VIRTUAL == null) VIRTUAL = cast.toKey("virtual");
			if (DATASOURCES == null) DATASOURCES = cast.toKey("datasources");
			if (NAME == null) NAME = cast.toKey("name");
			if (DATABASE == null) DATABASE = cast.toKey("database");
			if (FROM_CFCONFIG == null) FROM_CFCONFIG = cast.toKey("fromCFConfig");

			if (data != null) {
				json = data;
			}
			else {
				json = ConfigFile.read(file, charset);
			}

			replacePlaceHolder(json, toMap(placeHolderData));

			// dynAttr = (DynamicAttributes) tag;
			boolean isServer = "server".equalsIgnoreCase(type);
			String strPW = ConfigUtil.decrypt(password);
			Password pw; // hash password if
			if (isServer && config instanceof ConfigWebPro) {
				pw = ConfigUtil.getConfigServerImpl(config).isPasswordEqual(strPW);
			}
			else {
				pw = config.isPasswordEqual(strPW);
			}

			boolean updated = setPasswordIfNecessary(config);
			ConfigAdmin admin = ConfigAdmin.newInstance(config, pw, updated || !validatePassword);

			admin.updateConfig(json, flushExistingData);
			admin.storeAndReload();
			ConfigUtil.getConfigServerImpl(config).resetAll(null);
		}
		catch (Throwable t) {
			ExceptionUtil.rethrowIfNecessary(t);
			if (throwException) {
				throw Caster.toPageException(t);
			}
			LogUtil.log("deploy", "config-imprt", t);
		}
		if (throwException && exd != null) throw exd;
		return json;

	}

	private Map<Key, String> toMap(Struct placeHolderData) {
		if (placeHolderData == null) return null;
		Map<Key, String> map = new HashMap<>();
		Iterator<Entry<Key, Object>> it = placeHolderData.entryIterator();
		while (it.hasNext()) {
			Entry<Key, Object> e = it.next();
			map.put(e.getKey(), Caster.toString(e.getValue(), ""));
		}
		return map;
	}

	private void replacePlaceHolder(Collection coll, Map<Key, String> placeHolderData) {
		// ${MAILSERVER_HOST:smtp.sendgrid.net}
		Iterator<Entry<Key, Object>> it = coll.entryIterator();
		Entry<Key, Object> e;
		Object obj;
		while (it.hasNext()) {
			e = it.next();
			obj = e.getValue();
			if (obj instanceof String) {

				String str = (String) e.getValue();
				String resolved = config.replacePlaceHolder(str, placeHolderData);
				if (!str.equals(resolved)) e.setValue(resolved);
			}
			if (obj instanceof Collection) replacePlaceHolder((Collection) obj, placeHolderData);
		}
	}

	private boolean setPasswordIfNecessary(ConfigPro config) throws PageException {
		if (!setPasswordIfNecessary) return false;
		boolean isServer = "server".equalsIgnoreCase(type);
		if ((isServer && !pwCheckedServer) || (!isServer && !pwCheckedWeb)) {
			boolean hasPassword = isServer ? config.hasServerPassword() : config.hasPassword();
			if (!hasPassword) {
				// create password
				try {
					if (config instanceof ConfigWebPro && isServer) ((ConfigWebPro) config).updatePassword(isServer, null, password);
					else {
						PasswordImpl.updatePassword(config, null, password);
					}
					return true;
				}
				catch (Exception e) {
					throw Caster.toPageException(e);
				}
			}
			if (isServer) pwCheckedServer = true;
			else pwCheckedWeb = true;
		}
		return false;
	}
}