package lucee.transformer.dynamic.meta.dynamic;

import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiFunction;

import lucee.print;
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
	public Object invoke(Object obj, Object... args) throws Exception {
		if (ConstructorDynamic.USE_DYN_CLASS_CREATION) {
			DynamicInvoker di = DynamicInvoker.getExistingInstance();
			Clazz clazzz = di.toClazz(getDeclaringClass());
			return ((BiFunction<Object, Object[], Object>) di.getInstance(clazzz, this, args)).apply(obj, args);

		}

		if (method == null) {
			method = getAccessibleMethod(getDeclaringClass());
		}

		// Verify the cached method works for this object
		if (obj != null && !method.getDeclaringClass().isAssignableFrom(obj.getClass())) {
			print.e("----> " + method.getDeclaringClass().getName() + ":" + obj.getClass().getName());
			print.e("----> " + method.getDeclaringClass().getClassLoader() + ":" + obj.getClass().getClassLoader());
			method = getAccessibleMethod(obj.getClass());
		}

		return method.invoke(obj, args);

	}

	private java.lang.reflect.Method getAccessibleMethod(Class<?> startClass) throws IllegalAccessException, SecurityException {
		String methodName = getName();
		Class<?>[] argTypes = getArgumentClasses();

		// If the starting class itself is public, try it first
		if (Modifier.isPublic(startClass.getModifiers())) {
			try {
				java.lang.reflect.Method m = startClass.getMethod(methodName, argTypes);
				if (Modifier.isPublic(m.getModifiers())) {
					return m;
				}
			}
			catch (NoSuchMethodException e) {
				// continue
			}
		}

		// Walk up to find a public interface that declares this method as public
		for (Class<?> iface: getAllInterfaces(startClass)) {
			if (Modifier.isPublic(iface.getModifiers())) {
				try {
					java.lang.reflect.Method m = iface.getMethod(methodName, argTypes);
					if (Modifier.isPublic(m.getModifiers())) {
						return m;
					}
				}
				catch (NoSuchMethodException e) {
					// continue
				}
			}
		}

		// Walk up the superclass hierarchy
		Class<?> current = startClass.getSuperclass();
		while (current != null) {
			if (Modifier.isPublic(current.getModifiers())) {
				try {
					java.lang.reflect.Method m = current.getMethod(methodName, argTypes);
					if (Modifier.isPublic(m.getModifiers())) {
						return m;
					}
				}
				catch (NoSuchMethodException e) {
					// continue
				}
			}
			current = current.getSuperclass();
		}

		throw new IllegalAccessException("Unable to find an accessible method for [" + toString() + "]");
	}

	// Helper to get all interfaces including inherited ones
	private static Set<Class<?>> getAllInterfaces(Class<?> clazz) {
		Set<Class<?>> interfaces = new LinkedHashSet<>();
		collectInterfaces(clazz, interfaces);
		return interfaces;
	}

	private static void collectInterfaces(Class<?> clazz, Set<Class<?>> interfaces) {
		if (clazz == null) return;

		for (Class<?> iface: clazz.getInterfaces()) {
			if (interfaces.add(iface)) {
				collectInterfaces(iface, interfaces); // interfaces can extend other interfaces
			}
		}
		collectInterfaces(clazz.getSuperclass(), interfaces); // superclass may implement more interfaces
	}
}
