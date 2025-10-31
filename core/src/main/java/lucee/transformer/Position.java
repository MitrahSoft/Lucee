/**
 * Copyright (c) 2015, Lucee Association Switzerland. All rights reserved.
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
 */
package lucee.transformer;

public final class Position {

	public final int line;
	public final int column;
	public final int pos;
	public final int offset;

	public Position(int line, int column, int position, int offset) {

		this.line = line;
		this.column = column;
		this.pos = position;
		this.offset = offset;
	}

	public int displayPosition() {
		// we need to substract the offset
		if (offset > 0 && (pos - offset) >= 0) {
			return pos - offset;
		}
		return pos;
	}

	public int displayColumn() {
		// we need to substract the offset from the first line for display
		if (offset > 0 && line == 1 && (column - offset) >= 0) {
			return column - offset;
		}
		return column;
	}

	@Override
	public String toString() {
		return new StringBuilder("line:").append(line).append(";column:").append(column).append(";pos:").append(pos).append(";offset:").append(offset).toString();
	}

}