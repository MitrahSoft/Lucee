package lucee.commons.io.log;

import lucee.runtime.config.Config;
import lucee.runtime.config.ConfigFactoryImpl;
import lucee.runtime.config.ConfigPro;
import lucee.runtime.config.Prop;
import lucee.runtime.config.PropFactory;
import lucee.runtime.op.Caster;
import lucee.runtime.type.Array;
import lucee.runtime.type.ArrayImpl;
import lucee.runtime.type.KeyImpl;
import lucee.runtime.type.Struct;
import lucee.runtime.type.StructImpl;
import lucee.runtime.type.util.KeyConstants;

public class LogFactory implements PropFactory<LoggerAndSourceData> {

	private static LogFactory instance;

	public static LogFactory getInstance() {
		if (instance == null) {
			instance = new LogFactory();
		}
		return instance;
	}

	@Override
	public LoggerAndSourceData evaluate(Config config, String name, Object val, LoggerAndSourceData defaultValue) {
		try {
			return ConfigFactoryImpl.loadLogger((ConfigPro) config, name, Caster.toStruct(val));
		}
		catch (Exception e) {
			LogUtil.log("log-factory", e);
			return defaultValue;
		}
	}

	@Override
	public Struct schema(Prop<LoggerAndSourceData> prop) {
		Struct sct = new StructImpl(Struct.TYPE_LINKED);
		sct.setEL(KeyConstants._type, "object");

		Struct properties = new StructImpl(Struct.TYPE_LINKED);
		sct.setEL(KeyConstants._properties, properties);

		// 1. Levels (Handling 'level' and 'logLevel' aliases)
		Struct level = PropFactory.createSimple("string", "The logging level (e.g., 'info', 'error').");
		Array levels = new ArrayImpl();
		levels.appendEL("fatal");
		levels.appendEL("error");
		levels.appendEL("warn");
		levels.appendEL("info");
		levels.appendEL("debug");
		levels.appendEL("trace");
		level.setEL("enum", levels);

		properties.setEL(KeyConstants._level, level);
		properties.setEL(KeyImpl.init("logLevel"), level);

		// 2. Appender Section
		// loadLogger looks for "appender" (simple name) or "appender-class", etc.
		properties.setEL(KeyImpl.init("appender"), PropFactory.createSimple("string", "The appender name or class."));

		// Calls the unified method with "appender" prefix
		PropFactory.appendClassDefinitionProps(properties, "appender");

		properties.setEL(KeyImpl.init("appenderArguments"), PropFactory.createSimple("object", "Arguments passed to the log appender."));

		// 3. Layout Section
		// loadLogger looks for "layout" (simple name) or "layout-class", etc.
		properties.setEL(KeyImpl.init("layout"), PropFactory.createSimple("string", "The layout name or class (e.g. 'classic', 'xml')."));

		// Calls the unified method with "layout" prefix
		PropFactory.appendClassDefinitionProps(properties, "layout");

		properties.setEL(KeyImpl.init("layoutArguments"), PropFactory.createSimple("object", "Arguments passed to the log layout."));

		// 4. Flags
		properties.setEL(KeyConstants._readOnly, PropFactory.createSimple("boolean", "If true, the logger cannot be modified via the administrator."));

		return sct;
	}

	@Override
	public Object resolvedValue(LoggerAndSourceData value) {
		return value;
	}

}
