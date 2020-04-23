/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2020 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package de.metas.ui.web.notes.json;

import de.metas.util.JSONObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JSONNoteCreateRequestTest
{
	@Test
	void testSerialisationDeserialisation()
	{
		final JSONObjectMapper<JSONNoteCreateRequest> jsonObjectMapper = JSONObjectMapper.forClass(JSONNoteCreateRequest.class);

		final JSONNoteCreateRequest expectedNote = new JSONNoteCreateRequest("This is a test note.\nTra la la.");

		final String json = jsonObjectMapper.writeValueAsString(expectedNote);
		final JSONNoteCreateRequest deserialisedRequest = jsonObjectMapper.readValue(json);
		assertThat(deserialisedRequest).isEqualToIgnoringGivenFields(expectedNote);
		System.out.println(deserialisedRequest);
	}

}
