package lucee.transformer.library;

import lucee.commons.io.log.Log;
import lucee.runtime.config.Config;
import lucee.runtime.config.ConfigFactoryImpl;
import lucee.runtime.config.Prop;
import lucee.runtime.config.PropFactory;
import lucee.runtime.db.ClassDefinition;
import lucee.runtime.op.Caster;
import lucee.runtime.type.Struct;
import lucee.runtime.type.StructImpl;
import lucee.runtime.type.util.KeyConstants;

public class ClassDefinitionFactory implements PropFactory<ClassDefinition> {
	private static ClassDefinitionFactory instance;

	public static ClassDefinitionFactory getInstance() {
		if (instance == null) {
			instance = new ClassDefinitionFactory();
		}
		return instance;
	}

	@Override
	public ClassDefinition evaluate(Config config, String name, Object val, ClassDefinition defaultValue) {

		try {
			Struct cache = Caster.toStruct(val);
			if (cache == null) return defaultValue;
			ClassDefinitionImpl cd = (ClassDefinitionImpl) ConfigFactoryImpl.getClassDefinition(config, cache, "", config.getIdentification());

			if (cd.isBundle() || cd.isMaven()) {
				return cd;
			}
			ConfigFactoryImpl.log(config, Log.LEVEL_INFO, "[" + cd + "] does not have bundle nor maven info");
			return defaultValue;
		}
		catch (Exception ex) {
			ConfigFactoryImpl.log(config, ex);
		}

		return defaultValue;
	}

	@Override
	public Struct schema(Prop<ClassDefinition> prop) {
		Struct sct = new StructImpl(Struct.TYPE_LINKED);
		sct.setEL(KeyConstants._type, "object");

		Struct properties = new StructImpl(Struct.TYPE_LINKED);
		sct.setEL(KeyConstants._properties, properties);

		PropFactory.appendClassDefinitionProps(properties);

		return sct;
	}

	@Override
	public Object resolvedValue(ClassDefinition value) {
		return value;
	}

}
