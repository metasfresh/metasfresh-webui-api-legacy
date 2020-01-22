package de.metas.ui.web.view;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import javax.annotation.Nullable;

import org.adempiere.util.lang.SynchronizedMutable;

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
	private final SynchronizedMutable<ViewRowIdsOrderedSelections> currentSelectionsRef = SynchronizedMutable.of(null);

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
	}

	private ViewRowIdsOrderedSelections getCurrentSelections()
	{
		return currentSelectionsRef.computeIfNull(this::createViewRowIdsOrderedSelections);
	}

	private ViewRowIdsOrderedSelections computeCurrentSelections(@NonNull final UnaryOperator<ViewRowIdsOrderedSelections> remappingFunction)
	{
		return currentSelectionsRef.compute(previousSelections -> {
			final ViewRowIdsOrderedSelections selections = previousSelections != null
					? previousSelections
					: createViewRowIdsOrderedSelections();

			return remappingFunction.apply(selections);
		});
	}

	public ViewRowIdsOrderedSelections computeCurrentSelectionsIfPresent(@NonNull final UnaryOperator<ViewRowIdsOrderedSelections> remappingFunction)
	{
		return currentSelectionsRef.computeIfNotNull(remappingFunction);
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

		return ViewRowIdsOrderedSelections.ofDefaultSelection(defaultSelection);
	}

	public void forgetCurrentSelections()
	{
		selectionDeleteBeforeCreate.set(true);
		final ViewRowIdsOrderedSelections selections = currentSelectionsRef.setValueAndReturnPrevious(null);
		if (selections != null)
		{
			final ImmutableSet<String> selectionIds = selections.getSelectionIds();
			viewDataRepository.scheduleDeleteSelections(selectionIds);
		}
	}

	private ViewEvaluationCtx getViewEvaluationCtx()
	{
		return viewEvaluationCtxSupplier.get();
	}

	public ViewRowIdsOrderedSelection getDefaultSelection()
	{
		return getCurrentSelections().getDefaultSelection();
	}

	public void removeRowIdsNotMatchingFilters(final Set<DocumentId> rowIds)
	{
		if (rowIds.isEmpty())
		{
			return;
		}

		computeCurrentSelectionsIfPresent(selections -> removeRowIdsNotMatchingFilters(selections, rowIds));
	}

	private ViewRowIdsOrderedSelections removeRowIdsNotMatchingFilters(
			@NonNull final ViewRowIdsOrderedSelections selections,
			@NonNull final Set<DocumentId> rowIds)
	{
		return selections.withDefaultSelection(
				viewDataRepository.removeRowIdsNotMatchingFilters(
						selections.getDefaultSelection(),
						allFilters,
						rowIds));
	}

	public ViewRowIdsOrderedSelection getOrderedSelection(final DocumentQueryOrderByList orderBys)
	{
		return computeCurrentSelections(selections -> computeOrderBySelectionIfAbsent(selections, orderBys))
				.getSelection(orderBys);
	}

	private ViewRowIdsOrderedSelections computeOrderBySelectionIfAbsent(
			@NonNull final ViewRowIdsOrderedSelections selections,
			@Nullable final DocumentQueryOrderByList orderBys)
	{
		return selections.withOrderBysSelectionIfAbsent(
				orderBys,
				this::createSelectionFromSelection);
	}

	private ViewRowIdsOrderedSelection createSelectionFromSelection(
			@NonNull final ViewRowIdsOrderedSelection fromSelection,
			@Nullable final DocumentQueryOrderByList orderBys)
	{
		return viewDataRepository.createOrderedSelectionFromSelection(getViewEvaluationCtx(), fromSelection, orderBys);
	}
}
