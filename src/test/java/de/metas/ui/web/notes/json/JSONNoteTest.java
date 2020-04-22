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

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class JSONNoteTest
{

	@Test
	void testSerialisationDeserialisation()
	{
		final JSONObjectMapper<JSONNote> jsonObjectMapper = JSONObjectMapper.forClass(JSONNote.class);

		final JSONNote expectedNote = JSONNote.builder()
				.createdBy("who created this?")
				.created(ZonedDateTime.of(2020, 4, 22, 11, 11, 11, 0, ZoneId.of("UTC+3")))
				.text("This is a test note.\nTra la la.")
				.build();

		final String json = jsonObjectMapper.writeValueAsString(expectedNote);
		final JSONNote deserialisedRequest = jsonObjectMapper.readValue(json);
		assertThat(deserialisedRequest).isEqualToIgnoringGivenFields(expectedNote);
	}

}
