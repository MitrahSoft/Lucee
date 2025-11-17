/**
 *
 * Copyright (c) 2014, the Railo Company Ltd. All rights reserved.
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
/**
 * Implements the CFML Function gettemplatepath
 */
package lucee.runtime.functions.system;

import java.io.IOException;

import lucee.runtime.Mapping;
import lucee.runtime.MappingImpl;
import lucee.runtime.PageContext;
import lucee.runtime.config.ConfigWebUtil;
import lucee.runtime.exp.PageException;
import lucee.runtime.ext.function.Function;
import lucee.runtime.op.Caster;
import lucee.runtime.type.Query;
import lucee.runtime.type.QueryImpl;

public final class PagePoolInfo implements Function {

	private static final long serialVersionUID = -8114072486800418753L;

	public static Query call(PageContext pc) throws PageException {
		QueryImpl q = new QueryImpl(new String[] { "class", "iterations", "size", "totaSize" }, 0, "poolInfo");

		try {
			// int row;
			for (Mapping m: ConfigWebUtil.getAllMappings(pc)) {
				// row = q.addRow();
				// q.absolute(row);
				populate((MappingImpl) m, q);
			}
			// q.beforeFirst();
		}
		catch (Exception e) {
			throw Caster.toPageException(e);
		}
		return q;
	}

	private static void populate(MappingImpl mapping, Query query) throws IOException {
		mapping.populateClassInfo(query);
	}

}