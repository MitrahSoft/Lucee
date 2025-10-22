package lucee.runtime.functions.system;

import java.util.Map;
import java.util.Map.Entry;

import org.osgi.framework.Version;

import lucee.commons.io.res.Resource;
import lucee.runtime.PageContext;
import lucee.runtime.config.ConfigPro;
import lucee.runtime.config.maven.ExtensionProvider;
import lucee.runtime.exp.FunctionException;
import lucee.runtime.exp.PageException;
import lucee.runtime.ext.function.BIF;
import lucee.runtime.op.Caster;
import lucee.runtime.osgi.OSGiUtil;
import lucee.runtime.type.Array;
import lucee.runtime.type.ArrayImpl;
import lucee.runtime.type.Struct;
import lucee.runtime.type.StructImpl;
import lucee.runtime.type.util.KeyConstants;

public final class LuceeExtension extends BIF {

	private static final long serialVersionUID = -6051430362930149903L;

	@Override
	public Object invoke(PageContext pc, Object[] args) throws PageException {

		ExtensionProvider ep = new ExtensionProvider();
		try {
			// list extensions
			if (args.length == 0) {
				Array arr = new ArrayImpl();
				for (String artifact: ep.list()) {
					arr.appendEL(artifact);
				}
				return arr;
			}
			// list versions of an extension
			else if (args.length == 1) {
				Array arr = new ArrayImpl();
				for (Version version: ep.list(Caster.toString(args[0]))) {
					arr.appendEL(version.toString());
				}
				return arr;
			}
			// detail to a specific extension
			else if (args.length == 2) {
				String artifactId = Caster.toString(args[0]);
				Version version = OSGiUtil.toVersion(Caster.toString(args[1]));

				// detail
				Struct sct = new StructImpl();
				Map<String, Object> data = ep.detail(artifactId, version);
				for (Entry<String, Object> e: data.entrySet()) {
					sct.set(Caster.toKey(e.getKey()), e.getValue());
				}

				// local resource
				Resource local = ep.getResource((ConfigPro) pc.getConfig(), artifactId, version);
				sct.set(KeyConstants._local, local.getAbsolutePath());

				return sct;
			}
			else {
				throw new FunctionException(pc, "LuceeVersionsListS3", 0, 1, args.length);
			}
		}
		catch (Exception e) {
			throw Caster.toPageException(e);
		}
	}
}