package de.metas.ui.web.document.filter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import de.metas.util.GuavaCollectors;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

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

@EqualsAndHashCode
@ToString
public class DocumentFilterList
{
	public static final DocumentFilterList ofList(@Nullable final Collection<DocumentFilter> list)
	{
		return list != null && !list.isEmpty()
				? new DocumentFilterList(list)
				: EMPTY;
	}

	public static final DocumentFilterList of(@NonNull final DocumentFilter filter)
	{
		return ofList(ImmutableList.of(filter));
	}

	public static final DocumentFilterList of(@NonNull final DocumentFilter... filters)
	{
		return ofList(Arrays.asList(filters));
	}

	public static Collector<DocumentFilter, ?, DocumentFilterList> toDocumentFilterList()
	{
		return GuavaCollectors.collectUsingListAccumulator(DocumentFilterList::ofList);
	}

	public static final DocumentFilterList EMPTY = new DocumentFilterList(ImmutableList.of());

	private final ImmutableList<DocumentFilter> list;

	private DocumentFilterList(@NonNull final Collection<DocumentFilter> list)
	{
		this.list = ImmutableList.copyOf(list);
	}

	public static boolean equals(final DocumentFilterList list1, final DocumentFilterList list2)
	{
		return Objects.equals(list1, list2);
	}

	public boolean isEmpty()
	{
		return list.isEmpty();
	}

	public ImmutableList<DocumentFilter> toList()
	{
		return list;
	}

	public Stream<DocumentFilter> stream()
	{
		return list.stream();
	}

	public DocumentFilterList mergeWith(@NonNull final DocumentFilterList other)
	{
		if (isEmpty())
		{
			return other;
		}
		else if (other.isEmpty())
		{
			return this;
		}
		else
		{
			return ofList(ImmutableList.<DocumentFilter> builder()
					.addAll(this.list)
					.addAll(other.list)
					.build());
		}
	}

	public DocumentFilterList mergeWith(@NonNull final DocumentFilter filter)
	{
		if (isEmpty())
		{
			return of(filter);
		}
		else
		{
			return ofList(ImmutableList.<DocumentFilter> builder()
					.addAll(this.list)
					.add(filter)
					.build());
		}
	}

	public Optional<DocumentFilter> getFilterById(@NonNull final String filterId)
	{
		return stream()
				.filter(filter -> filterId.equals(filter.getFilterId()))
				.findFirst();
	}

	public void forEach(@NonNull final Consumer<DocumentFilter> consumer)
	{
		list.forEach(consumer);
	}

	public String getParamValueAsString(final String filterId, final String parameterName)
	{
		final DocumentFilter filter = getFilterById(filterId).orElse(null);
		if (filter == null)
		{
			return null;
		}

		return filter.getParameterValueAsString(parameterName);
	}

	public int getParamValueAsInt(final String filterId, final String parameterName, final int defaultValue)
	{
		final DocumentFilter filter = getFilterById(filterId).orElse(null);
		if (filter == null)
		{
			return defaultValue;
		}

		return filter.getParameterValueAsInt(parameterName, defaultValue);
	}

	public boolean getParamValueAsBoolean(final String filterId, final String parameterName, final boolean defaultValue)
	{
		final DocumentFilter filter = getFilterById(filterId).orElse(null);
		if (filter == null)
		{
			return defaultValue;
		}

		return filter.getParameterValueAsBoolean(parameterName, defaultValue);
	}
}
