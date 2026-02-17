package lucee.transformer.dynamic.meta.dynamic;

import java.io.IOException;
import java.util.function.BiFunction;

import lucee.commons.lang.ExceptionUtil;
import lucee.transformer.dynamic.DynamicInvoker;
import lucee.transformer.dynamic.meta.Clazz;
import lucee.transformer.dynamic.meta.Method;

class MethodDynamic extends FunctionMemberDynamic implements Method {

	private static final long serialVersionUID = 7046827988301434206L;
	private java.lang.reflect.Method method;

	public MethodDynamic(Class declaringClass, String name) {
		super(name);
	}

	@Override
	public Object invoke(Object obj, Object... args) throws IOException {
		if (ConstructorDynamic.USE_DYN_CLASS_CREATION) {
			DynamicInvoker di = DynamicInvoker.getExistingInstance();
			Clazz clazzz = di.toClazz(getDeclaringClass());
			try {
				return ((BiFunction<Object, Object[], Object>) di.getInstance(clazzz, this, args)).apply(obj, args);
			}
			catch (Exception e) {
				throw ExceptionUtil.toIOException(e);
			}
		}

		try {
			if (method == null) {
				method = getDeclaringClass().getMethod(getName(), getArgumentClasses());
			}
			return method.invoke(obj, args);
		}
		catch (Exception e) {
			throw ExceptionUtil.toIOException(e);
		}

	}
}
