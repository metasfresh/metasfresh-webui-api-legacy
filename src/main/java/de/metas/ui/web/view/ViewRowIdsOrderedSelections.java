package de.metas.ui.web.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

import javax.annotation.Nullable;

import org.adempiere.exceptions.AdempiereException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import de.metas.ui.web.window.model.DocumentQueryOrderBy;
import lombok.NonNull;

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

final class ViewRowIdsOrderedSelections
{
	private ViewRowIdsOrderedSelection defaultSelection;
	private final HashMap<ImmutableList<DocumentQueryOrderBy>, ViewRowIdsOrderedSelection> selectionsByOrderBys = new HashMap<>();

	public ViewRowIdsOrderedSelections(@NonNull final ViewRowIdsOrderedSelection defaultSelection)
	{
		this.defaultSelection = defaultSelection;
	}

	public synchronized ViewRowIdsOrderedSelection getDefaultSelection()
	{
		return defaultSelection;
	}

	public synchronized ViewRowIdsOrderedSelection computeDefaultSelection(@NonNull final UnaryOperator<ViewRowIdsOrderedSelection> mapper)
	{
		final ViewRowIdsOrderedSelection newDefaultSelection = mapper.apply(defaultSelection);
		if (newDefaultSelection == null)
		{
			throw new AdempiereException("null default selection is not allowed");
		}

		if (!defaultSelection.equals(newDefaultSelection))
		{
			this.defaultSelection = newDefaultSelection;
			selectionsByOrderBys.clear();
		}

		return defaultSelection;
	}

	@FunctionalInterface
	public interface ViewRowIdsOrderedSelectionFactory
	{
		ViewRowIdsOrderedSelection create(ViewRowIdsOrderedSelection defaultSelection, List<DocumentQueryOrderBy> orderBys);
	}

	public synchronized ViewRowIdsOrderedSelection computeIfAbsent(
			@Nullable final List<DocumentQueryOrderBy> orderBys,
			@NonNull final ViewRowIdsOrderedSelections.ViewRowIdsOrderedSelectionFactory factory)
	{
		if (orderBys == null || orderBys.isEmpty())
		{
			return defaultSelection;
		}

		if (Objects.equals(defaultSelection.getOrderBys(), orderBys))
		{
			return defaultSelection;
		}

		return selectionsByOrderBys.computeIfAbsent(
				ImmutableList.copyOf(orderBys),
				orderBysImmutable -> factory.create(defaultSelection, orderBysImmutable));
	}

	public synchronized ImmutableSet<String> getSelectionIds()
	{
		final ImmutableSet.Builder<String> selectionIds = ImmutableSet.builder();
		selectionIds.add(defaultSelection.getSelectionId());
		for (final ViewRowIdsOrderedSelection selection : new ArrayList<>(selectionsByOrderBys.values()))
		{
			selectionIds.add(selection.getSelectionId());
		}

		return selectionIds.build();
	}
}
