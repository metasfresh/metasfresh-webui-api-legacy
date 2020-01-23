/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2019 metas GmbH
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

package de.metas.banking.process.bankstatement_allocateInvoicesProcess;

import com.google.common.collect.ImmutableSet;
import de.metas.bpartner.BPartnerId;
import de.metas.document.engine.DocStatus;
import de.metas.invoice.InvoiceId;
import de.metas.ui.web.window.datatypes.LookupValuesList;
import de.metas.ui.web.window.descriptor.DocumentFieldWidgetType;
import de.metas.ui.web.window.descriptor.LookupDescriptor;
import de.metas.ui.web.window.descriptor.sql.SqlLookupDescriptor;
import de.metas.ui.web.window.model.lookup.LookupDataSourceFactory;
import de.metas.util.Services;
import org.adempiere.ad.dao.IQueryBL;
import org.compiere.model.I_C_Invoice;
import org.compiere.util.DisplayType;
import org.springframework.stereotype.Service;

@Service
public class BankStatement_AllocateInvoicesService
{
	private final IQueryBL queryBL = Services.get(IQueryBL.class);

	public BankStatement_AllocateInvoicesService()
	{
	}

	public LookupValuesList getInvoiceLookupProvider_UnpaidByBpartner(final BPartnerId bPartnerId)
	{
		final ImmutableSet<InvoiceId> invoiceIds = queryBL.createQueryBuilder(I_C_Invoice.class)
				.addOnlyActiveRecordsFilter()
				.addEqualsFilter(I_C_Invoice.COLUMNNAME_DocStatus, DocStatus.Completed)
				.addEqualsFilter(I_C_Invoice.COLUMNNAME_IsPaid, false)
				.addEqualsFilter(I_C_Invoice.COLUMNNAME_C_BPartner_ID, bPartnerId)
				.orderBy(I_C_Invoice.COLUMNNAME_DateInvoiced)
				.create()
				.listIds(InvoiceId::ofRepoId);

		final LookupDescriptor invoiceByIdLookupDescriptor = SqlLookupDescriptor.builder()
				.setCtxTableName(I_C_Invoice.Table_Name)
				.setCtxColumnName(I_C_Invoice.COLUMNNAME_C_Invoice_ID)
				.setDisplayType(DisplayType.Search)
				.setWidgetType(DocumentFieldWidgetType.Lookup)
				.buildForDefaultScope();

		return LookupDataSourceFactory.instance.getLookupDataSource(invoiceByIdLookupDescriptor).findByIdsOrdered(invoiceIds);
	}
}
