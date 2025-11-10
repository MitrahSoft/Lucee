/**
 * Copyright (c) 2025, Lucee Association Switzerland. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 *
 **/
package lucee.transformer.bytecode.statement.tag;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import lucee.runtime.op.Caster;
import lucee.transformer.Factory;
import lucee.transformer.Position;
import lucee.transformer.TransformerException;
import lucee.transformer.bytecode.BytecodeContext;
import lucee.transformer.bytecode.statement.FlowControlFinal;
import lucee.transformer.bytecode.util.ASMConstants;
import lucee.transformer.bytecode.util.Types;
import lucee.transformer.expression.Expression;
import lucee.transformer.statement.tag.Attribute;
import lucee.transformer.statement.tag.Tag;

/**
 * Optimized bytecode generation for cfproperty tags. Bypasses tag lifecycle overhead by calling
 * ComponentUtil.registerProperty directly.
 */
public final class TagProperty extends TagBase {

	private static final Method REGISTER_PROPERTY = new Method("registerProperty", Type.VOID_TYPE, new Type[] { Types.PAGE_CONTEXT, Types.STRING, Types.STRING, Types.OBJECT,
			Types.STRING, Types.STRING, Types.STRING, Type.BOOLEAN_TYPE, Type.BOOLEAN_TYPE, Type.BOOLEAN_TYPE });

	private static final Method REGISTER_PROPERTY_WITH_DYNAMIC = new Method("registerProperty", Type.VOID_TYPE, new Type[] { Types.PAGE_CONTEXT, Types.STRING, Types.STRING,
			Types.OBJECT, Types.STRING, Types.STRING, Types.STRING, Type.BOOLEAN_TYPE, Type.BOOLEAN_TYPE, Type.BOOLEAN_TYPE, Types.STRUCT });

	public TagProperty(Factory f, Position start, Position end) {
		super(f, start, end);
	}

	@Override
	public FlowControlFinal getFlowControlFinal() {
		return null;
	}

	@Override
	public void _writeOut(BytecodeContext bc) throws TransformerException {
		final GeneratorAdapter adapter = bc.getAdapter();
		Tag tag = this;

		bc.visitLine(tag.getStart());

		try {
			String name = null;
			String type = null;
			Attribute defaultValueAttr = null;
			String access = null;
			String hint = null;
			String displayname = null;
			boolean required = false;
			boolean setter = true;
			boolean getter = true;

			List<Attribute> dynamicAttrs = new ArrayList<>();
			Iterator<Attribute> it = tag.getAttributes().values().iterator();
			while (it.hasNext()) {
				Attribute attr = it.next();
				String attrName = attr.getName().toLowerCase();

				if ("name".equals(attrName)) {
					name = getLiteralString(attr);
				}
				else if ("type".equals(attrName)) {
					type = getLiteralString(attr);
				}
				else if ("default".equals(attrName)) {
					defaultValueAttr = attr;
				}
				else if ("access".equals(attrName)) {
					access = getLiteralString(attr);
				}
				else if ("hint".equals(attrName)) {
					hint = getLiteralString(attr);
				}
				else if ("displayname".equals(attrName)) {
					displayname = getLiteralString(attr);
				}
				else if ("required".equals(attrName)) {
					String val = getLiteralString(attr);
					required = Caster.toBoolean(val, false);
				}
				else if ("setter".equals(attrName)) {
					String val = getLiteralString(attr);
					setter = Caster.toBoolean(val, true);
				}
				else if ("getter".equals(attrName)) {
					String val = getLiteralString(attr);
					getter = Caster.toBoolean(val, true);
				}
				else {
					dynamicAttrs.add(attr);
				}
			}

			adapter.loadArg(0);

			if (name != null) {
				adapter.push(name);
			}
			else {
				ASMConstants.NULL(adapter);
			}

			if (type != null) {
				adapter.push(type);
			}
			else {
				ASMConstants.NULL(adapter);
			}
			if (defaultValueAttr != null) {
				defaultValueAttr.getValue().writeOut(bc, Expression.MODE_REF);
			}
			else {
				ASMConstants.NULL(adapter);
			}

			if (access != null) {
				adapter.push(access);
			}
			else {
				ASMConstants.NULL(adapter);
			}

			if (hint != null) {
				adapter.push(hint);
			}
			else {
				ASMConstants.NULL(adapter);
			}

			if (displayname != null) {
				adapter.push(displayname);
			}
			else {
				ASMConstants.NULL(adapter);
			}

			adapter.push(required);

			adapter.push(setter);

			adapter.push(getter);

			boolean hasExplicitRequired = tag.getAttributes().containsKey("required") && tag.getAttribute("required") != null;

			if (!dynamicAttrs.isEmpty() || hasExplicitRequired) {
				adapter.newInstance(Types.STRUCT_IMPL);
				adapter.dup();
				adapter.invokeConstructor(Types.STRUCT_IMPL, new Method("<init>", Type.VOID_TYPE, new Type[] {}));

				for (Attribute dynAttr: dynamicAttrs) {
					adapter.dup();
					adapter.push(dynAttr.getName());
					dynAttr.getValue().writeOut(bc, Expression.MODE_REF);
					adapter.invokeInterface(Types.STRUCT, new Method("setEL", Types.OBJECT, new Type[] { Types.STRING, Types.OBJECT }));
					adapter.pop();
				}

				// Only add required if explicitly set
				if (hasExplicitRequired) {
					adapter.dup();
					adapter.push("required");
					adapter.push(required ? "yes" : "no");
					adapter.invokeInterface(Types.STRUCT, new Method("setEL", Types.OBJECT, new Type[] { Types.STRING, Types.OBJECT }));
					adapter.pop();
				}

				adapter.invokeStatic(Type.getType("Llucee/runtime/type/util/ComponentUtil;"), REGISTER_PROPERTY_WITH_DYNAMIC);
			}
			else {
				adapter.invokeStatic(Type.getType("Llucee/runtime/type/util/ComponentUtil;"), REGISTER_PROPERTY);
			}

			bc.visitLine(tag.getEnd());

		}
		catch (Exception e) {
			if (e instanceof TransformerException) throw (TransformerException) e;
			throw new TransformerException(bc, e, tag.getStart());
		}
	}

	private String getLiteralString(Attribute attr) {
		try {
			if (attr != null && attr.getValue() != null) {
				return attr.getValue().toString();
			}
		}
		catch (Exception e) {
			// Fall back to null if we can't get literal value
		}
		return null;
	}

}