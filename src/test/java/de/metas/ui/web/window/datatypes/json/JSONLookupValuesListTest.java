package de.metas.ui.web.window.datatypes.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import de.metas.JsonObjectMapperHolder;
import de.metas.ui.web.window.datatypes.LookupValue.StringLookupValue;
import de.metas.ui.web.window.datatypes.LookupValuesList;

public class JSONLookupValuesListTest
{
	private ObjectMapper jsonObjectMapper;

	@BeforeEach
	public void init()
	{
		jsonObjectMapper = JsonObjectMapperHolder.newJsonObjectMapper();
	}

	@Test
	public void test_EMPTY_toString()
	{
		JSONLookupValuesList lookupValuesList = JSONLookupValuesList.ofLookupValuesList(LookupValuesList.EMPTY, "en_US");
		lookupValuesList.toString();
	}

	@Test
	public void testSerializeDeserialize() throws Exception
	{
		final LookupValuesList list = LookupValuesList.fromCollection(
				ImmutableList.of(
						StringLookupValue.of("Y", "Yes"),
						StringLookupValue.of("N", "No")));

		final JSONLookupValuesList jsonList = JSONLookupValuesList.ofLookupValuesList(list, "en_US");

		testSerializeDeserialize(jsonList);
	}

	private void testSerializeDeserialize(final JSONLookupValuesList obj) throws IOException
	{
		final String json = jsonObjectMapper.writeValueAsString(obj);
		System.out.println("JSON: " + json);

		final JSONLookupValuesList objDeserialized = jsonObjectMapper.readValue(json, JSONLookupValuesList.class);
		assertThat(objDeserialized).isEqualTo(obj);
	}

}
