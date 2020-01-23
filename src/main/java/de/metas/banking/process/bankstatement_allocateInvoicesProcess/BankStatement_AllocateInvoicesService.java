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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.metas.banking.interfaces.I_C_BankStatementLine_Ref;
import de.metas.banking.model.I_C_BankStatementLine;
import de.metas.banking.payment.IBankStatmentPaymentBL;
import de.metas.bpartner.BPartnerId;
import de.metas.document.engine.DocStatus;
import de.metas.invoice.InvoiceId;
import de.metas.payment.PaymentId;
import de.metas.payment.api.IPaymentDAO;
import de.metas.ui.web.window.datatypes.LookupValuesList;
import de.metas.ui.web.window.descriptor.DocumentFieldWidgetType;
import de.metas.ui.web.window.descriptor.LookupDescriptor;
import de.metas.ui.web.window.descriptor.sql.SqlLookupDescriptor;
import de.metas.ui.web.window.model.lookup.LookupDataSourceFactory;
import de.metas.util.Services;
import org.adempiere.ad.dao.IQueryBL;
import org.adempiere.model.InterfaceWrapperHelper;
import org.compiere.model.I_C_Invoice;
import org.compiere.model.I_C_Payment;
import org.compiere.util.DisplayType;
import org.springframework.stereotype.Service;

@Service
public class BankStatement_AllocateInvoicesService
{
	private final IQueryBL queryBL = Services.get(IQueryBL.class);
	private final IPaymentDAO paymentDAO = Services.get(IPaymentDAO.class);
	private final IBankStatmentPaymentBL bankStatmentPaymentBL = Services.get(IBankStatmentPaymentBL.class);

	public BankStatement_AllocateInvoicesService()
	{
	}

	/**
	 * Link the single Payment to the BankStatementLine directly.
	 */
	public void handleSinglePaymentAllocation(final I_C_BankStatementLine bankStatementLine, final ImmutableList<PaymentId> paymentIds)
	{
		final I_C_Payment payment = paymentDAO.getById(paymentIds.iterator().next());
		bankStatmentPaymentBL.setC_Payment(bankStatementLine, payment);
		InterfaceWrapperHelper.save(bankStatementLine);
		// interceptors will update the bank statement and line
	}

	/**
	 * Iterate over the Payments and link them to the BankStatementLine via BankStatementLineRef
	 */
	public void handleMultiplePaymentsAllocation(final I_C_BankStatementLine bankStatementLine, final ImmutableList<PaymentId> paymentIds)
	{
		bankStatementLine.setIsMultiplePaymentOrInvoice(true);
		bankStatementLine.setIsMultiplePayment(true);
		int lineNumber = 10;
		for (final PaymentId paymentId : paymentIds)
		{
			// Create bank statement line ref
			final I_C_BankStatementLine_Ref lineRef = InterfaceWrapperHelper.newInstance(I_C_BankStatementLine_Ref.class);
			lineRef.setC_BankStatementLine_ID(bankStatementLine.getC_BankStatementLine_ID());
			lineRef.setLine(lineNumber);
			lineNumber += 10;

			final I_C_Payment payment = paymentDAO.getById(paymentId);
			bankStatmentPaymentBL.setC_Payment(lineRef, payment);
			InterfaceWrapperHelper.save(lineRef);
		}
		// interceptors will update the bank statement and line
		InterfaceWrapperHelper.save(bankStatementLine);
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
