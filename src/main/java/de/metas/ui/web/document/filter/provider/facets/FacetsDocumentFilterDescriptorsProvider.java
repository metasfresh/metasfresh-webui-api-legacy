package de.metas.ui.web.document.filter.provider.facets;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import de.metas.ui.web.document.filter.DocumentFilterDescriptor;
import de.metas.ui.web.document.filter.provider.DocumentFilterDescriptorsProvider;
import lombok.Builder;
import lombok.Singular;

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

public class FacetsDocumentFilterDescriptorsProvider implements DocumentFilterDescriptorsProvider
{
	private final ImmutableMap<String, DocumentFilterDescriptor> filterDescriptorsByFilterId;

	@Builder
	private FacetsDocumentFilterDescriptorsProvider(
			@Singular final List<DocumentFilterDescriptor> filterDescriptors)
	{
		this.filterDescriptorsByFilterId = Maps.uniqueIndex(filterDescriptors, DocumentFilterDescriptor::getFilterId);
	}

	@Override
	public Collection<DocumentFilterDescriptor> getAll()
	{
		return filterDescriptorsByFilterId.values();
	}

	@Override
	public DocumentFilterDescriptor getByFilterIdOrNull(String filterId)
	{
		return filterDescriptorsByFilterId.get(filterId);
	}

}
