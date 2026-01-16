package lucee.runtime.config;

import lucee.runtime.op.Caster;
import lucee.runtime.type.Struct;
import lucee.runtime.type.StructImpl;
import lucee.runtime.type.util.KeyConstants;

public class PasswordFactory implements PropFactory<Password> {

	private static PasswordFactory instance;
	private static PasswordFactory instanceDefault;
	private boolean def;

	private PasswordFactory(boolean def) {
		this.def = def;
	}

	public static PasswordFactory getInstanceDefault() {
		if (instanceDefault == null) {
			instanceDefault = new PasswordFactory(true);
		}
		return instanceDefault;
	}

	public static PasswordFactory getInstance() {
		if (instance == null) {
			instance = new PasswordFactory(false);
		}
		return instance;
	}

	@Override
	public Password evaluate(Config config, String name, Object val, Password defaultValue) {
		((ConfigPro) config).getSalt();

		String strPW = Caster.toString(val, null);
		if (strPW == null) return defaultValue;

		return PasswordImpl.read(config, name, strPW, ((ConfigPro) config).getSalt(), def);
	}

	@Override
	public Struct schema(Prop<Password> prop) {
		Struct sct = new StructImpl(Struct.TYPE_LINKED);
		sct.setEL(KeyConstants._type, "string");

		// Descriptions based on the 'def' flag in the Factory
		String desc = this.def ? "The default administrator password. Supports plain text (auto-encrypted), hashed, or hashed+salted."
				: "The administrator password. Supports plain text (auto-encrypted), hashed, or hashed+salted.";

		sct.setEL(KeyConstants._description, desc);

		// In createConfigSchema, the loop will register this schema under multiple keys.
		// Based on PasswordImpl.read, the relevant keys are:
		// If def=false: adminPassword, adminpw, adminhspw
		// If def=true: adminDefaultPassword, adminDefaultpw, adminDefaulthspw, adminPasswordDefault

		return sct;
	}

	@Override
	public Object resolvedValue(Password value) {
		return value;
	}
}
