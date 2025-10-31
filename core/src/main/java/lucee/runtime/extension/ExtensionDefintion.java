package lucee.runtime.extension;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.framework.Version;

import lucee.commons.io.res.Resource;
import lucee.commons.lang.StringUtil;
import lucee.runtime.config.Config;
import lucee.runtime.exp.ApplicationException;
import lucee.runtime.exp.PageException;
import lucee.runtime.mvn.MavenUtil.GAVSO;
import lucee.runtime.osgi.OSGiUtil;

public final class ExtensionDefintion {

	private String id;
	private Map<String, String> params = new HashMap<String, String>();
	private Resource source;
	private Config config;
	private RHExtension rhe;
	private GAVSO gavso;

	public ExtensionDefintion() {
	}

	public ExtensionDefintion(String id) {
		this.id = id;
	}

	public ExtensionDefintion(String id, String version) {
		this.id = id;
		setParam("version", version);
	}

	public ExtensionDefintion(RHExtension rhe, String id, String version) {
		this.rhe = rhe;
		this.id = id;
		setParam("version", version);
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public GAVSO getGAVSO() {
		return gavso;
	}

	public void setGAVSO(GAVSO gavso) {
		this.gavso = gavso;
		if (gavso != null) {
			if (!StringUtil.isEmpty(gavso.g, true)) setParam("groupId", gavso.g);
			if (!StringUtil.isEmpty(gavso.a, true)) setParam("artifactId", gavso.a);
			if (!StringUtil.isEmpty(gavso.v, true)) setParam("version", gavso.v);
			if (!StringUtil.isEmpty(gavso.s, true)) setParam("scope", gavso.s);
			if (!StringUtil.isEmpty(gavso.o, true)) setParam("optional", gavso.o);
		}
	}

	public String getArtifactId() {
		if (gavso != null) return gavso.a;
		return null;
	}

	public String getGroupId() {
		if (gavso != null) return gavso.g;
		return null;
	}

	public String getSymbolicName() {
		String sn = params.get("symbolic-name");
		if (StringUtil.isEmpty(sn, true)) return getId();
		return sn.trim();
	}

	public void setParam(String name, String value) {
		params.put(name, value);
	}

	public Map<String, String> getParams() {
		return params;
	}

	public String getVersion() {
		String version = params.get("version");
		if (StringUtil.isEmpty(version)) version = params.get("extension-version");
		if (StringUtil.isEmpty(version)) return null;
		return version;
	}

	public Version getSince() {
		String since = params.get("since");
		if (StringUtil.isEmpty(since)) return null;
		return OSGiUtil.toVersion(since, null);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getId());
		Iterator<Entry<String, String>> it = params.entrySet().iterator();
		Entry<String, String> e;
		while (it.hasNext()) {
			e = it.next();
			sb.append(';').append(e.getKey()).append('=').append(e.getValue());
		}
		return sb.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ExtensionDefintion) {
			ExtensionDefintion ed = (ExtensionDefintion) other;
			if (!ed.getId().equalsIgnoreCase(getId())) return false;
			if (ed.getVersion() == null || getVersion() == null) return true;
			return ed.getVersion().equalsIgnoreCase(getVersion());
		}
		else if (other instanceof RHExtension) {
			RHExtension ed = (RHExtension) other;
			if (!ed.getId().equalsIgnoreCase(getId())) return false;
			if (ed.getVersion() == null || getVersion() == null) return true;
			return ed.getVersion().equalsIgnoreCase(getVersion());
		}
		return false;
	}

	public void setSource(RHExtension rhe) {
		this.rhe = rhe;
		if (rhe.getExtensionFile() != null) this.source = rhe.getExtensionFile();
	}

	public ExtensionDefintion setSource(Config config, Resource source) {
		this.config = config;
		this.source = source;
		return this;
	}

	public RHExtension toRHExtension(RHExtension defaultValue) {
		if (rhe != null) return rhe;

		if (source == null) {
			return defaultValue;
		}
		rhe = RHExtension.getInstance(config, source, defaultValue);
		return rhe;
	}

	public RHExtension toRHExtension() throws PageException {
		if (rhe != null) return rhe;

		if (source == null) {
			// MUST try to load the Extension
			throw new ApplicationException("ExtensionDefinition does not contain the necessary data to create the requested object.");
		}
		rhe = RHExtension.getInstance(config, source);
		return rhe;
	}

	//
	public Resource getSource(Resource defaultValue) {
		if (source != null) return source;
		if (rhe != null) return rhe.getExtensionFile();
		try {
			return source = RHExtension.getExtensionInstalledFile(config, getId(), getVersion(), false);
		}
		catch (Exception e) {
			return defaultValue;
		}
	}

	public Resource getSource() throws ApplicationException {
		if (source != null) return source;
		if (rhe != null) return rhe.getExtensionFile();

		return source = RHExtension.getExtensionInstalledFile(config, getId(), getVersion(), false);
	}

}
