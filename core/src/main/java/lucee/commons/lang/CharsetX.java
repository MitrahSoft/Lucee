/**
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
package lucee.commons.lang;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import lucee.commons.io.CharsetUtil;

public final class CharsetX implements Externalizable {

	public static final CharsetX UTF8 = new CharsetX(CharsetUtil.UTF8);
	public static final CharsetX ISO88591 = new CharsetX(CharsetUtil.ISO88591);
	public static final CharsetX UTF16BE = new CharsetX(CharsetUtil.UTF16BE);
	public static final CharsetX UTF16LE = new CharsetX(CharsetUtil.UTF16LE);
	public static final CharsetX UTF32BE = new CharsetX(CharsetUtil.UTF32BE);

	private transient java.nio.charset.Charset charset;

	/**
	 * NEVER USE THIS CONSTRUCTOR DIRECTLY, THIS IS FOR Externalizable ONLY
	 */
	public CharsetX() {
	}

	public CharsetX(String charsetName) {
		this.charset = java.nio.charset.Charset.forName(charsetName);
	}

	public CharsetX(java.nio.charset.Charset charset) {
		this.charset = charset;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeUTF(charset.name());
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		this.charset = java.nio.charset.Charset.forName(in.readUTF());
	}

	public String toString() {
		return charset.name();
	}

	public String name() {
		return charset.name();
	}

	public java.nio.charset.Charset toCharset() {
		return charset;
	}
}