/**
 * Copyright (c) 2024, Lucee Association Switzerland
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
package lucee.transformer.library.tag;

/**
 * LDEV-5901: Represents an attribute group for documentation purposes.
 * Groups related attributes by action/mode for clearer documentation.
 */
public final class TagLibTagAttrGroup {

	private String name;
	private String label;
	private String description;
	private String attributes; // comma-separated attribute names

	public TagLibTagAttrGroup() {
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public void setAttributes(String attributes) {
		this.attributes = attributes;
	}

	public String getAttributes() {
		return attributes;
	}
}
