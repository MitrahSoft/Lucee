package lucee.runtime.security;

import lucee.runtime.exp.ApplicationException;

public class EnvVarSecretProvider extends SecretProviderSupport {

	@Override
	public String getSecret(String key) throws ApplicationException {
		String sec = System.getenv(key);
		if (sec == null) throw new ApplicationException("there was no enviroment variable found with the name [" + key + "]");
		return sec;
	}

	@Override
	public String getSecret(String key, String defaultValue) {
		String sec = System.getenv(key);
		if (sec == null) return defaultValue;
		return sec;
	}

	@Override
	public boolean hasSecret(String key) {
		return System.getenv(key) != null;
	}

	@Override
	public void refresh() {
		// no cache so no refresh needed
	}

}
