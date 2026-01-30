package lucee.runtime.config;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import lucee.runtime.exp.PageException;
import lucee.runtime.op.Caster;
import lucee.runtime.type.util.ListUtil;

public class ResetFilter {

	private Set<String> names = new HashSet<>();

	public void add(String... names) {
		for (String name: names) {
			this.names.add(name);
		}
	}

	public boolean allow(String name) {
		return names.contains(name);
	}

	public void reset(Config config) throws IOException {
		ConfigUtil.getConfigServerImpl(config).resetAll(this);
	}

	public void resetThrowPageException(Config config) throws PageException {
		try {
			ConfigUtil.getConfigServerImpl(config).resetAll(this);
		}
		catch (IOException e) {
			throw Caster.toPageException(e);
		}
	}

	@Override
	public String toString() {
		return ListUtil.toList(names, ", ");
	}

}
