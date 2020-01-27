package de.metas.ui.web.document.filter.provider.facets;

import java.util.Collection;

import javax.annotation.Nullable;

import org.adempiere.ad.element.api.AdTabId;
import org.adempiere.exceptions.AdempiereException;
import org.springframework.stereotype.Component;

import de.metas.i18n.IMsgBL;
import de.metas.i18n.ITranslatableString;
import de.metas.invoicecandidate.model.I_C_Invoice_Candidate;
import de.metas.ui.web.document.filter.DocumentFilterDescriptor;
import de.metas.ui.web.document.filter.DocumentFilterParamDescriptor;
import de.metas.ui.web.document.filter.provider.DocumentFilterDescriptorsProvider;
import de.metas.ui.web.document.filter.provider.DocumentFilterDescriptorsProviderFactory;
import de.metas.ui.web.document.filter.provider.NullDocumentFilterDescriptorsProvider;
import de.metas.ui.web.view.IViewsRepository;
import de.metas.ui.web.window.datatypes.PanelLayoutType;
import de.metas.ui.web.window.descriptor.DocumentFieldDescriptor;
import de.metas.ui.web.window.descriptor.DocumentFieldWidgetType;
import de.metas.util.Services;
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

@Component
public class FacetsDocumentFilterDescriptorsProviderFactory implements DocumentFilterDescriptorsProviderFactory
{
	private final IMsgBL msgBL = Services.get(IMsgBL.class);
	private final IViewsRepository viewsRepository;

	public FacetsDocumentFilterDescriptorsProviderFactory(@NonNull final IViewsRepository viewsRepository)
	{
		this.viewsRepository = viewsRepository;
	}

	@Override
	public DocumentFilterDescriptorsProvider createFiltersProvider(
			@Nullable final AdTabId adTabId,
			@Nullable final String tableName,
			final Collection<DocumentFieldDescriptor> fields)
	{
		if (de.metas.invoicecandidate.model.I_C_Invoice_Candidate.Table_Name.equals(tableName))
		{
			return createFiltersProvider_InvoiceCandidates(adTabId, tableName, fields);
		}
		else
		{
			return NullDocumentFilterDescriptorsProvider.instance;
		}
	}

	private FacetsDocumentFilterDescriptorsProvider createFiltersProvider_InvoiceCandidates(
			final AdTabId adTabId,
			final String tableName,
			final Collection<DocumentFieldDescriptor> fields)
	{
		return FacetsDocumentFilterDescriptorsProvider.builder()
				.filterDescriptor(createFilter_InvoiceCandidate_BillBPartner(fields))
				.filterDescriptor(createFilter_InvoiceCandidate_Order(fields))
				.filterDescriptor(createFilter_InvoiceCandidate_InOut(fields))
				.filterDescriptor(createFilter_InvoiceCandidate_DeliveryDate(fields))
				.filterDescriptor(createFilter_InvoiceCandidate_IsSOTrx(fields))
				.build();
	}

	private DocumentFilterDescriptor createFilter_InvoiceCandidate_Order(final Collection<DocumentFieldDescriptor> fields)
	{
		final String tableName = I_C_Invoice_Candidate.Table_Name;
		final String columnName = I_C_Invoice_Candidate.COLUMNNAME_C_Order_ID;
		final String filterId = "facet-" + tableName + "-" + columnName;

		final DocumentFieldDescriptor fieldDescriptor = extractFieldDescriptorByName(fields, columnName);
		final FacetsFilterLookupDescriptor lookupDescriptor = FacetsFilterLookupDescriptor.builder()
				.viewsRepository(viewsRepository)
				.id(filterId)
				.fieldName(columnName)
				.widgetType(DocumentFieldWidgetType.List)
				.fieldLookupDescriptor(fieldDescriptor.getLookupDescriptorForFiltering().get())
				.build();

		final ITranslatableString displayName = msgBL.translatable(columnName);

		return DocumentFilterDescriptor.builder()
				.setFilterId(filterId)
				.setFrequentUsed(true)
				.setParametersLayoutType(PanelLayoutType.Panel)
				.setDisplayName(displayName)
				.setFacetFilter(true)
				.addParameter(DocumentFilterParamDescriptor.builder()
						.setFieldName(columnName)
						.setDisplayName(displayName)
						.setMandatory(true)
						.setWidgetType(DocumentFieldWidgetType.List)
						.setLookupDescriptor(lookupDescriptor))
				.build();
	}

	private DocumentFilterDescriptor createFilter_InvoiceCandidate_InOut(final Collection<DocumentFieldDescriptor> fields)
	{
		final String tableName = I_C_Invoice_Candidate.Table_Name;
		final String columnName = I_C_Invoice_Candidate.COLUMNNAME_M_InOut_ID;
		final String filterId = "facet-" + tableName + "-" + columnName;

		final DocumentFieldDescriptor fieldDescriptor = extractFieldDescriptorByName(fields, columnName);
		final FacetsFilterLookupDescriptor lookupDescriptor = FacetsFilterLookupDescriptor.builder()
				.viewsRepository(viewsRepository)
				.id(filterId)
				.fieldName(columnName)
				.widgetType(DocumentFieldWidgetType.List)
				.fieldLookupDescriptor(fieldDescriptor.getLookupDescriptorForFiltering().get())
				.build();

		final ITranslatableString displayName = msgBL.translatable(columnName);

		return DocumentFilterDescriptor.builder()
				.setFilterId(filterId)
				.setFrequentUsed(true)
				.setParametersLayoutType(PanelLayoutType.Panel)
				.setDisplayName(displayName)
				.setFacetFilter(true)
				.addParameter(DocumentFilterParamDescriptor.builder()
						.setFieldName(columnName)
						.setDisplayName(displayName)
						.setMandatory(true)
						.setWidgetType(DocumentFieldWidgetType.List)
						.setLookupDescriptor(lookupDescriptor))
				.build();
	}

	private DocumentFilterDescriptor createFilter_InvoiceCandidate_BillBPartner(final Collection<DocumentFieldDescriptor> fields)
	{
		final String tableName = I_C_Invoice_Candidate.Table_Name;
		final String columnName = I_C_Invoice_Candidate.COLUMNNAME_Bill_BPartner_ID;
		final String filterId = "facet-" + tableName + "-" + columnName;

		final DocumentFieldDescriptor fieldDescriptor = extractFieldDescriptorByName(fields, columnName);
		final FacetsFilterLookupDescriptor lookupDescriptor = FacetsFilterLookupDescriptor.builder()
				.viewsRepository(viewsRepository)
				.id(filterId)
				.fieldName(columnName)
				.widgetType(DocumentFieldWidgetType.List)
				.fieldLookupDescriptor(fieldDescriptor.getLookupDescriptorForFiltering().get())
				.build();

		final ITranslatableString displayName = msgBL.translatable(columnName);

		return DocumentFilterDescriptor.builder()
				.setFilterId(filterId)
				.setFrequentUsed(true)
				.setParametersLayoutType(PanelLayoutType.Panel)
				.setDisplayName(displayName)
				.setFacetFilter(true)
				.addParameter(DocumentFilterParamDescriptor.builder()
						.setFieldName(columnName)
						.setDisplayName(displayName)
						.setMandatory(true)
						.setWidgetType(DocumentFieldWidgetType.List)
						.setLookupDescriptor(lookupDescriptor))
				.build();
	}

	private DocumentFilterDescriptor createFilter_InvoiceCandidate_DeliveryDate(final Collection<DocumentFieldDescriptor> fields)
	{
		final String tableName = I_C_Invoice_Candidate.Table_Name;
		final String columnName = I_C_Invoice_Candidate.COLUMNNAME_DeliveryDate;
		final String filterId = "facet-" + tableName + "-" + columnName;

		final FacetsFilterLookupDescriptor lookupDescriptor = FacetsFilterLookupDescriptor.builder()
				.viewsRepository(viewsRepository)
				.id(filterId)
				.fieldName(columnName)
				.widgetType(DocumentFieldWidgetType.LocalDate)
				.build();

		final ITranslatableString displayName = msgBL.translatable(columnName);

		return DocumentFilterDescriptor.builder()
				.setFilterId(filterId)
				.setFrequentUsed(true)
				.setParametersLayoutType(PanelLayoutType.Panel)
				.setDisplayName(displayName)
				.setFacetFilter(true)
				.addParameter(DocumentFilterParamDescriptor.builder()
						.setFieldName(columnName)
						.setDisplayName(displayName)
						.setMandatory(true)
						.setWidgetType(DocumentFieldWidgetType.List)
						.setLookupDescriptor(lookupDescriptor))
				.build();
	}

	private DocumentFilterDescriptor createFilter_InvoiceCandidate_IsSOTrx(final Collection<DocumentFieldDescriptor> fields)
	{
		final String tableName = I_C_Invoice_Candidate.Table_Name;
		final String columnName = I_C_Invoice_Candidate.COLUMNNAME_IsSOTrx;
		final String filterId = "facet-" + tableName + "-" + columnName;

		final FacetsFilterLookupDescriptor lookupDescriptor = FacetsFilterLookupDescriptor.builder()
				.viewsRepository(viewsRepository)
				.id(filterId)
				.fieldName(columnName)
				.widgetType(DocumentFieldWidgetType.YesNo)
				.build();

		final ITranslatableString displayName = msgBL.translatable(columnName);

		return DocumentFilterDescriptor.builder()
				.setFilterId(filterId)
				.setFrequentUsed(true)
				.setParametersLayoutType(PanelLayoutType.Panel)
				.setDisplayName(displayName)
				.setFacetFilter(true)
				.addParameter(DocumentFilterParamDescriptor.builder()
						.setFieldName(columnName)
						.setDisplayName(displayName)
						.setMandatory(true)
						.setWidgetType(DocumentFieldWidgetType.List)
						.setLookupDescriptor(lookupDescriptor))
				.build();
	}

	private static DocumentFieldDescriptor extractFieldDescriptorByName(final Collection<DocumentFieldDescriptor> fields, final String fieldName)
	{
		return fields.stream()
				.filter(field -> fieldName.equals(field.getFieldName()))
				.findFirst()
				.orElseThrow(() -> new AdempiereException("Field " + fieldName + " was not found in " + fields));
	}
}
