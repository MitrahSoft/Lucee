package lucee.transformer.library;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lucee.commons.io.log.Log;
import lucee.commons.lang.StringUtil;
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
	private static Map<String, ClassDefinitionFactory> instances = new ConcurrentHashMap<>();

	public static ClassDefinitionFactory getInstance() {
		return getInstance("");
	}

	public static ClassDefinitionFactory getInstance(String prefix) {
		ClassDefinitionFactory instance = instances.get(prefix);
		if (instances == null) {
			instances.put(prefix, instance = new ClassDefinitionFactory(prefix));
		}
		return instance;
	}

	private String prefix;

	public ClassDefinitionFactory(String prefix) {
		this.prefix = StringUtil.emptyIfNull(prefix).trim();
	}

	@Override
	public ClassDefinition evaluate(Config config, String name, Object val, ClassDefinition defaultValue) {

		try {
			Struct cache = Caster.toStruct(val);
			if (cache == null) return defaultValue;
			ClassDefinitionImpl cd = (ClassDefinitionImpl) ConfigFactoryImpl.getClassDefinition(config, cache, prefix, config.getIdentification());

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

		PropFactory.appendClassDefinitionProps(properties, prefix);

		return sct;
	}

	@Override
	public Object resolvedValue(ClassDefinition value) {
		return value;
	}

}
