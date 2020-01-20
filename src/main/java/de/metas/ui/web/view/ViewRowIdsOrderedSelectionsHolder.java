package de.metas.ui.web.view;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;

import javax.annotation.Nullable;

import org.adempiere.util.lang.ExtendedMemorizingSupplier;

import com.google.common.collect.ImmutableSet;

import de.metas.ui.web.document.filter.DocumentFilterList;
import de.metas.ui.web.document.filter.sql.SqlDocumentFilterConverterContext;
import de.metas.ui.web.window.model.DocumentQueryOrderByList;
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

final class ViewRowIdsOrderedSelectionsHolder
{
	private final IViewDataRepository viewDataRepository;
	private final ViewId viewId;
	private final boolean applySecurityRestrictions;
	private final DocumentFilterList stickyFilters;
	private final DocumentFilterList filters;
	private final ViewEvaluationCtx viewEvaluationCtx;

	private final AtomicBoolean defaultSelectionDeleteBeforeCreate = new AtomicBoolean(false);
	private final ExtendedMemorizingSupplier<ViewRowIdsOrderedSelections> selectionsRef;

	@lombok.Builder
	private ViewRowIdsOrderedSelectionsHolder(
			@NonNull final IViewDataRepository viewDataRepository,
			@NonNull final ViewId viewId,
			final boolean applySecurityRestrictions,
			@NonNull final DocumentFilterList stickyFilters,
			@NonNull final DocumentFilterList filters,
			@NonNull final ViewEvaluationCtx viewEvaluationCtx)
	{
		this.viewDataRepository = viewDataRepository;
		this.viewId = viewId;
		this.applySecurityRestrictions = applySecurityRestrictions;
		this.stickyFilters = stickyFilters;
		this.filters = filters;
		this.viewEvaluationCtx = viewEvaluationCtx;

		selectionsRef = ExtendedMemorizingSupplier.of(this::createViewRowIdsOrderedSelections);
	}

	private ViewRowIdsOrderedSelections createViewRowIdsOrderedSelections()
	{
		if (defaultSelectionDeleteBeforeCreate.get())
		{
			viewDataRepository.deleteSelection(viewId);
		}

		final ViewRowIdsOrderedSelection defaultSelection = viewDataRepository.createOrderedSelection(
				viewEvaluationCtx,
				viewId,
				stickyFilters.mergeWith(filters),
				applySecurityRestrictions,
				SqlDocumentFilterConverterContext.EMPTY);

		return new ViewRowIdsOrderedSelections(defaultSelection);
	}

	public ViewRowIdsOrderedSelection getDefaultSelection()
	{
		return selectionsRef.get().getDefaultSelection();
	}

	public ImmutableSet<String> forgetCurrentSelections()
	{
		final ViewRowIdsOrderedSelections selections = selectionsRef.forget();
		return selections != null
				? selections.getSelectionIds()
				: ImmutableSet.of();
	}

	public void setDeleteBeforeCreate(final boolean deleteBeforeCreate)
	{
		defaultSelectionDeleteBeforeCreate.set(true);
	}

	public ViewRowIdsOrderedSelection computeDefaultSelection(@NonNull final UnaryOperator<ViewRowIdsOrderedSelection> mapper)
	{
		return selectionsRef.get().computeDefaultSelection(mapper);
	}

	public ViewRowIdsOrderedSelection computeIfAbsent(
			@Nullable final DocumentQueryOrderByList orderBys,
			@NonNull final ViewRowIdsOrderedSelections.ViewRowIdsOrderedSelectionFactory factory)
	{
		return selectionsRef.get().computeIfAbsent(orderBys, factory);
	}
}
