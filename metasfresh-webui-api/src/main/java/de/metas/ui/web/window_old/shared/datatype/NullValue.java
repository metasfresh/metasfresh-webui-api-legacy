package de.metas.ui.web.window_old.shared.datatype;

import java.io.Serializable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/*
 * #%L
 * de.metas.ui.web.vaadin
 * %%
 * Copyright (C) 2016 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

@JsonSerialize(using = NullValueJSONSerializer.class)
@JsonDeserialize(using = NullValueJSONDeserializer.class)
@Deprecated
public final class NullValue implements Serializable
{
	private static final long serialVersionUID = 1L;

	public static final Object makeNotNull(final Object value)
	{
		return value == null ? instance : value;
	}

	public static final boolean isNull(final Object value)
	{
		if (value == null || value == instance)
		{
			return true;
		}

		if (value instanceof NullValue)
		{
			// FIXME: this shall not happen... but currently happens because of how we serialize/deserialize from JSON
			return true;
		}

		return false;
	}

	public static final NullValue getInstance()
	{
		return instance;
	}

	public static final transient NullValue instance = new NullValue();

	private NullValue()
	{
		super();
	}

	@Override
	public String toString()
	{
		return "NullValue";
	}
}
