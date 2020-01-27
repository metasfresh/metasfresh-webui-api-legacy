package de.metas.ui.web.document.filter.provider.facets;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.DisplayType;

import com.google.common.base.Predicates;

import de.metas.i18n.IMsgBL;
import de.metas.i18n.TranslatableStrings;
import de.metas.ui.web.view.DefaultView;
import de.metas.ui.web.view.IViewDataRepository;
import de.metas.ui.web.view.IViewsRepository;
import de.metas.ui.web.view.ViewEvaluationCtx;
import de.metas.ui.web.view.ViewId;
import de.metas.ui.web.window.datatypes.LookupValue;
import de.metas.ui.web.window.datatypes.LookupValue.StringLookupValue;
import de.metas.ui.web.window.datatypes.LookupValuesList;
import de.metas.ui.web.window.datatypes.Values;
import de.metas.ui.web.window.descriptor.DocumentFieldWidgetType;
import de.metas.ui.web.window.descriptor.LookupDescriptor;
import de.metas.ui.web.window.descriptor.SimpleLookupDescriptorTemplate;
import de.metas.ui.web.window.model.lookup.LookupDataSourceContext;
import de.metas.util.Services;
import de.metas.util.StringUtils;
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

final class FacetsFilterLookupDescriptor extends SimpleLookupDescriptorTemplate
{
	private final IMsgBL msgBL = Services.get(IMsgBL.class);
	private final IViewsRepository viewsRepository;

	private final String id;
	private final String fieldName;
	private final DocumentFieldWidgetType widgetType;
	private final LookupDescriptor fieldLookupDescriptor;

	private static final int MAX_FACETS = 10;

	@Builder
	private FacetsFilterLookupDescriptor(
			@NonNull final IViewsRepository viewsRepository,
			//
			@NonNull final String id,
			@NonNull final String fieldName,
			@NonNull final DocumentFieldWidgetType widgetType,
			@Nullable final LookupDescriptor fieldLookupDescriptor)
	{
		this.viewsRepository = viewsRepository;

		this.id = id;
		this.fieldName = fieldName;
		this.widgetType = widgetType;
		this.fieldLookupDescriptor = fieldLookupDescriptor;
	}

	@Override
	public Optional<String> getLookupTableName()
	{
		return fieldLookupDescriptor.getLookupDataSourceFetcher().getLookupTableName();
	}

	@Override
	public boolean isNumericKey()
	{
		if (widgetType.isLookup())
		{
			return fieldLookupDescriptor.isNumericKey();
		}
		else if (widgetType.isNumeric())
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	@Override
	public Set<String> getDependsOnFieldNames()
	{
		return fieldLookupDescriptor.getDependsOnFieldNames();
	}

	@Override
	public LookupDataSourceContext.Builder newContextForFetchingById(final Object id)
	{
		final LookupDataSourceContext.Builder builder = fieldLookupDescriptor != null
				? fieldLookupDescriptor.getLookupDataSourceFetcher().newContextForFetchingById(id)
				: LookupDataSourceContext.builderWithoutTableName().putFilterById(id);

		return builder
				.requiresParameter(LookupDataSourceContext.PARAM_ViewId)
				.requiresParameter(LookupDataSourceContext.PARAM_ViewSize);
	}

	@Override
	public LookupValue retrieveLookupValueById(final LookupDataSourceContext evalCtx)
	{
		return fieldLookupDescriptor.getLookupDataSourceFetcher().retrieveLookupValueById(evalCtx);
	}

	@Override
	public LookupDataSourceContext.Builder newContextForFetchingList()
	{
		final LookupDataSourceContext.Builder builder = fieldLookupDescriptor != null
				? fieldLookupDescriptor.getLookupDataSourceFetcher().newContextForFetchingList()
				: LookupDataSourceContext.builderWithoutTableName();

		return builder
				.requiresParameter(LookupDataSourceContext.PARAM_ViewId)
				.requiresParameter(LookupDataSourceContext.PARAM_ViewSize);
	}

	@Override
	public LookupValuesList retrieveEntities(final LookupDataSourceContext evalCtx)
	{
		final DefaultView view = getView(evalCtx);

		return view.getFacetFiltersCacheMap()
				.computeIfAbsent(id, () -> createFacetFilterViewCache(view))
				.getAvailableValues();
	}

	private FacetFilterViewCache createFacetFilterViewCache(final DefaultView view)
	{
		final IViewDataRepository viewDataRepository = view.getViewDataRepository();
		final ViewEvaluationCtx viewEvalCtx = view.getViewEvaluationCtx();
		final String selectionId = view.getDefaultSelectionBeforeFacetsFiltering().getSelectionId();

		final List<Object> fieldValues = viewDataRepository.retrieveFieldValues(
				viewEvalCtx,
				selectionId,
				fieldName,
				MAX_FACETS);

		final LookupValuesList availableValues = fieldValues.stream()
				.map(this::convertRawFieldValueToLookupValue)
				.filter(Predicates.notNull())
				.distinct()
				.collect(LookupValuesList.collect());

		return FacetFilterViewCache.builder()
				.id(id)
				.availableValues(availableValues)
				.build();
	}

	private DefaultView getView(@NonNull final LookupDataSourceContext evalCtx)
	{
		final ViewId viewId = evalCtx.getViewId();
		return DefaultView.cast(viewsRepository.getView(viewId));
	}

	private LookupValue convertRawFieldValueToLookupValue(final Object fieldValue)
	{
		if (fieldValue == null)
		{
			return null;
		}
		else if (fieldValue instanceof LookupValue)
		{
			return (LookupValue)fieldValue;
		}
		else if (fieldValue instanceof LocalDate)
		{
			final LocalDate date = (LocalDate)fieldValue;
			return StringLookupValue.of(
					Values.localDateToJson(date),
					TranslatableStrings.date(date));
		}
		else if (fieldValue instanceof Boolean)
		{
			final boolean booleanValue = StringUtils.toBoolean(fieldValue);
			return StringLookupValue.of(
					DisplayType.toBooleanString(booleanValue),
					msgBL.getTranslatableMsgText(booleanValue));
		}
		else
		{
			throw new AdempiereException("Value not supported: " + fieldValue + " (" + fieldValue.getClass() + ")")
					.appendParametersToMessage()
					.setParameter("fieldName", fieldName);
		}
	}
}
