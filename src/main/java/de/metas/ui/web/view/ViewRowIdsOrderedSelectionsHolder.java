package de.metas.ui.web.view;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.adempiere.util.lang.ExtendedMemorizingSupplier;

import com.google.common.collect.ImmutableSet;

import de.metas.ui.web.document.filter.DocumentFilterList;
import de.metas.ui.web.document.filter.sql.SqlDocumentFilterConverterContext;
import de.metas.ui.web.window.datatypes.DocumentId;
import de.metas.ui.web.window.model.DocumentQueryOrderByList;
import lombok.Builder;
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
	private final DocumentFilterList allFilters;
	private final Supplier<ViewEvaluationCtx> viewEvaluationCtxSupplier;

	private final AtomicBoolean selectionDeleteBeforeCreate = new AtomicBoolean(false);
	private final ExtendedMemorizingSupplier<ViewRowIdsOrderedSelections> selectionsRef;

	@Builder
	private ViewRowIdsOrderedSelectionsHolder(
			@NonNull final IViewDataRepository viewDataRepository,
			@NonNull final ViewId viewId,
			final boolean applySecurityRestrictions,
			@NonNull final DocumentFilterList stickyFilters,
			@NonNull final DocumentFilterList filters,
			@NonNull final Supplier<ViewEvaluationCtx> viewEvaluationCtxSupplier)
	{
		this.viewDataRepository = viewDataRepository;
		this.viewId = viewId;
		this.applySecurityRestrictions = applySecurityRestrictions;
		this.allFilters = stickyFilters.mergeWith(filters);
		this.viewEvaluationCtxSupplier = viewEvaluationCtxSupplier;

		selectionsRef = ExtendedMemorizingSupplier.of(this::createViewRowIdsOrderedSelections);
	}

	private ViewRowIdsOrderedSelections createViewRowIdsOrderedSelections()
	{
		if (selectionDeleteBeforeCreate.get())
		{
			viewDataRepository.deleteSelection(viewId);
		}

		final ViewRowIdsOrderedSelection defaultSelection = viewDataRepository.createOrderedSelection(
				getViewEvaluationCtx(),
				viewId,
				allFilters,
				applySecurityRestrictions,
				SqlDocumentFilterConverterContext.EMPTY);

		return new ViewRowIdsOrderedSelections(defaultSelection);
	}

	private ViewEvaluationCtx getViewEvaluationCtx()
	{
		return viewEvaluationCtxSupplier.get();
	}

	public ViewRowIdsOrderedSelection getDefaultSelection()
	{
		return selectionsRef.get().getDefaultSelection();
	}

	public void forgetCurrentSelections()
	{
		selectionDeleteBeforeCreate.set(true);
		final ViewRowIdsOrderedSelections selections = selectionsRef.forget();
		if (selections != null)
		{
			final ImmutableSet<String> selectionIds = selections.getSelectionIds();
			viewDataRepository.scheduleDeleteSelections(selectionIds);
		}
	}

	public void removeRowIdsNotMatchingFilters(final Set<DocumentId> rowIds)
	{
		selectionsRef.get().computeDefaultSelection(defaultSelection -> viewDataRepository.removeRowIdsNotMatchingFilters(defaultSelection, allFilters, rowIds));
	}

	public ViewRowIdsOrderedSelection getOrderedSelection(final DocumentQueryOrderByList orderBysParam)
	{
		return selectionsRef.get().computeIfAbsent(
				orderBysParam,
				(defaultSelection, orderBys) -> viewDataRepository.createOrderedSelectionFromSelection(getViewEvaluationCtx(), defaultSelection, orderBys));
	}
}
