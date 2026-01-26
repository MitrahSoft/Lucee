package lucee.runtime.extension;

import java.io.IOException;

import org.osgi.framework.BundleException;

import lucee.commons.io.log.Log;
import lucee.commons.io.res.Resource;
import lucee.commons.io.res.util.ResourceUtil;
import lucee.commons.lang.StringUtil;
import lucee.runtime.config.Config;
import lucee.runtime.config.ConfigFactoryImpl;
import lucee.runtime.config.ConfigImpl;
import lucee.runtime.config.ConfigPro;
import lucee.runtime.config.Prop;
import lucee.runtime.config.PropFactory;
import lucee.runtime.op.Caster;
import lucee.runtime.osgi.BundleInfo;
import lucee.runtime.osgi.OSGiUtil;
import lucee.runtime.type.Array;
import lucee.runtime.type.ArrayImpl;
import lucee.runtime.type.KeyImpl;
import lucee.runtime.type.Struct;
import lucee.runtime.type.StructImpl;
import lucee.runtime.type.util.KeyConstants;

public class RHExtensionFactory implements PropFactory<RHExtension> {

	private static RHExtensionFactory instance;

	public static RHExtensionFactory getInstance() {
		if (instance == null) {
			instance = new RHExtensionFactory();
		}
		return instance;
	}

	@Override
	public RHExtension evaluate(Config c, String name, Object val, RHExtension defaultValue) {

		Struct data = Caster.toStruct(val, null);
		if (data == null) return defaultValue;

		ConfigPro config = (ConfigPro) c;
		String id = ConfigFactoryImpl.getAttr(config, data, KeyConstants._id);
		try {

			// one of the keys resource, path or url
			String strRes = ConfigFactoryImpl.getAttr(config, data, KeyConstants._resource, KeyConstants._path, KeyConstants._url);
			if (StringUtil.isEmpty(id) && StringUtil.isEmpty(strRes)) return defaultValue;

			Resource res = null;
			if (!StringUtil.isEmpty(strRes, true)) {
				res = ResourceUtil.toResourceExisting(config, strRes, null);
				if (res == null) {
					if (!StringUtil.isEmpty(id, true)) {
						ConfigFactoryImpl.log(config, Log.LEVEL_ERROR, "the resource [" + strRes + "] from the extension [" + id + "] cannot be resolved");
					}
					else {
						ConfigFactoryImpl.log(config, Log.LEVEL_ERROR, "the extension resource [" + strRes + "] cannot be resolved");
					}
				}
				else {
					if (!StringUtil.isEmpty(id, true)) {
						ConfigFactoryImpl.log(config, Log.LEVEL_INFO, "the resource [" + strRes + "] from the extension [" + id + "] is valid");
					}
					else {
						ConfigFactoryImpl.log(config, Log.LEVEL_INFO, "the extension resource [" + strRes + "] is valid");
					}
				}
			}
			return RHExtension.installExtension(config, id, ConfigFactoryImpl.getAttr(config, data, KeyConstants._version), res, false);
		}
		catch (Exception ex) {
			ConfigFactoryImpl.log(config, ex);
		}
		return defaultValue;

	}

	@Override
	public Struct schema(Prop<RHExtension> prop) {
		Struct sct = new StructImpl(Struct.TYPE_LINKED);
		sct.setEL(KeyConstants._type, "object");
		sct.setEL(KeyConstants._description, "Defines a Lucee Extension (LEX). You must provide either a unique 'id' or a valid 'resource/path/url' for auto-discovery.");

		Struct properties = new StructImpl(Struct.TYPE_LINKED);
		sct.setEL(KeyConstants._properties, properties);

		// Identification (Optional if path is provided)
		addProp(properties, "id", "string", "The unique identifier (UUID) of the extension. Optional if a resource path is provided.");
		addProp(properties, "version", "string", "The specific version of the extension. If omitted with a path, the version is extracted from the .lex header.");

		// Unified Resource Provider Keys
		String resourceDesc = "The location of the .lex file via Lucee's VFS (local, http, s3, etc.).";
		addProp(properties, "resource", "string", resourceDesc);
		addProp(properties, "path", "string", "Alias for 'resource'.");
		addProp(properties, "url", "string", "Alias for 'resource'.");

		// Validation Logic: Require ID OR a Resource path
		// We represent this in the schema so the UI/Validator knows at least one is needed
		Array anyOf = new ArrayImpl();

		Struct optionId = new StructImpl();
		Array reqId = new ArrayImpl();
		reqId.appendEL("id");
		optionId.setEL("required", reqId);

		Struct optionRes = new StructImpl();
		Array reqRes = new ArrayImpl(); // In Lucee 7.2, we accept any of the 3 aliases
		reqRes.appendEL("path"); // Or resource/url depending on how strict you want the schema
		optionRes.setEL("required", reqRes);

		anyOf.appendEL(optionId);
		anyOf.appendEL(optionRes);

		// In a standard Lucee schema struct, we'd use the "anyOf" key
		sct.setEL(KeyImpl.init("anyOf"), anyOf);

		return sct;
	}

	@Override
	public Object resolvedValue(RHExtension value) {
		return value;
	}

	private void addProp(Struct properties, String name, String type, String desc) {
		Struct p = new StructImpl(Struct.TYPE_LINKED);
		p.setEL(KeyConstants._type, type);
		p.setEL(KeyConstants._description, desc);
		properties.setEL(name, p);
	}

	public static void startBundles(ConfigImpl config, RHExtension rhe, boolean firstLoad) throws IOException, BundleException {
		if (rhe.getMetadata().isStartBundles()) {
			if (!firstLoad) {
				rhe.deployBundles(config, true);
			}
			else {
				try {
					BundleInfo[] bundles = rhe.getMetadata().getBundles();
					if (bundles != null) {
						for (BundleInfo bi: bundles) {
							OSGiUtil.loadBundleFromLocal(bi.getSymbolicName(), bi.getVersion(), null, false, null);
						}
					}
				}
				catch (Exception ex) {
					rhe.deployBundles(config, true);
				}
			}
		}

	}

}
