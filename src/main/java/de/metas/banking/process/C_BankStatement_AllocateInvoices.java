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

package de.metas.banking.process;

import com.google.common.collect.ImmutableList;
import de.metas.banking.api.BankAccountId;
import de.metas.banking.model.I_C_BankStatement;
import de.metas.banking.model.I_C_BankStatementLine;
import de.metas.banking.process.bankstatement_allocateInvoicesProcess.BankStatement_AllocateInvoicesService;
import de.metas.banking.process.bankstatement_allocateInvoicesProcess.PaymentsForInvoicesCreator;
import de.metas.banking.service.IBankStatementDAO;
import de.metas.bpartner.BPartnerId;
import de.metas.document.engine.DocStatus;
import de.metas.i18n.IMsgBL;
import de.metas.invoice.InvoiceId;
import de.metas.payment.PaymentId;
import de.metas.process.IProcessPrecondition;
import de.metas.process.IProcessPreconditionsContext;
import de.metas.process.JavaProcess;
import de.metas.process.Param;
import de.metas.process.ProcessPreconditionsResolution;
import de.metas.ui.web.process.descriptor.ProcessParamLookupValuesProvider;
import de.metas.ui.web.window.datatypes.LookupValuesList;
import de.metas.ui.web.window.descriptor.DocumentLayoutElementFieldDescriptor;
import de.metas.util.Services;
import lombok.NonNull;
import org.adempiere.util.lang.impl.TableRecordReference;
import org.compiere.SpringContextHolder;
import org.compiere.model.I_C_Invoice;
import org.compiere.util.TimeUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class C_BankStatement_AllocateInvoices extends JavaProcess implements IProcessPrecondition
{
	public static final String C_INVOICE_1_ID_PARAM_NAME = "C_Invoice_1_ID";
	@Param(parameterName = C_INVOICE_1_ID_PARAM_NAME, mandatory = true)
	private InvoiceId c_invoice_1_id;

	public static final String C_INVOICE_2_ID_PARAM_NAME = "C_Invoice_2_ID";
	@Param(parameterName = C_INVOICE_2_ID_PARAM_NAME)
	private InvoiceId c_invoice_2_id;

	public static final String C_INVOICE_3_ID_PARAM_NAME = "C_Invoice_3_ID";
	@Param(parameterName = C_INVOICE_3_ID_PARAM_NAME)
	private InvoiceId c_invoice_3_id;

	public static final String C_INVOICE_4_ID_PARAM_NAME = "C_Invoice_4_ID";
	@Param(parameterName = C_INVOICE_4_ID_PARAM_NAME)
	private InvoiceId c_invoice_4_id;

	public static final String C_INVOICE_5_ID_PARAM_NAME = "C_Invoice_5_ID";
	@Param(parameterName = C_INVOICE_5_ID_PARAM_NAME)
	private InvoiceId c_invoice_5_id;

	private final IMsgBL iMsgBL = Services.get(IMsgBL.class);
	private final IBankStatementDAO bankStatementDAO = Services.get(IBankStatementDAO.class);

	private final BankStatement_AllocateInvoicesService bankStatement_AllocateInvoicesService = SpringContextHolder.instance.getBean(BankStatement_AllocateInvoicesService.class);

	@Override
	public ProcessPreconditionsResolution checkPreconditionsApplicable(@NonNull final IProcessPreconditionsContext context)
	{
		if (context.isNoSelection())
		{
			return ProcessPreconditionsResolution.rejectBecauseNoSelection();
		}

		if (!context.isSingleSelection())
		{
			return ProcessPreconditionsResolution.rejectBecauseNotSingleSelection();
		}

		final I_C_BankStatement selectedBankStatement = context.getSelectedModel(I_C_BankStatement.class);
		final DocStatus docStatus = DocStatus.ofCode(selectedBankStatement.getDocStatus());
		if (docStatus.isCompletedOrClosedReversedOrVoided())
		{
			return ProcessPreconditionsResolution.reject(iMsgBL.getTranslatableMsgText("BankStatement should be open"));
		}

		// there should be a single line selected
		final Set<TableRecordReference> selectedLineReferences = context.getSelectedIncludedRecords();
		if (selectedLineReferences.size() != 1)
		{
			return ProcessPreconditionsResolution.reject(iMsgBL.getTranslatableMsgText("A single line should be selected."));
		}

		final TableRecordReference reference = selectedLineReferences.iterator().next();
		final I_C_BankStatementLine line = reference.getModel(I_C_BankStatementLine.class);
		if (line == null)
		{
			return ProcessPreconditionsResolution.reject(iMsgBL.getTranslatableMsgText("A single line should be selected."));
		}

		if (line.getC_BPartner_ID() <= 0)
		{
			return ProcessPreconditionsResolution.reject(iMsgBL.getTranslatableMsgText("Line should have a Business Partner."));
		}

		return ProcessPreconditionsResolution.accept();
	}

	@ProcessParamLookupValuesProvider(parameterName = C_INVOICE_1_ID_PARAM_NAME, numericKey = true, lookupSource = DocumentLayoutElementFieldDescriptor.LookupSource.lookup, lookupTableName = I_C_Invoice.Table_Name)
	public LookupValuesList invoice1LookupProvider()
	{
		final BPartnerId bPartnerId = BPartnerId.ofRepoId(getSelectedBankStatementLine().getC_BPartner_ID());
		return bankStatement_AllocateInvoicesService.getInvoiceLookupProvider_UnpaidByBpartner(bPartnerId);
	}

	@ProcessParamLookupValuesProvider(parameterName = C_INVOICE_2_ID_PARAM_NAME, numericKey = true, lookupSource = DocumentLayoutElementFieldDescriptor.LookupSource.lookup, lookupTableName = I_C_Invoice.Table_Name)
	public LookupValuesList invoice2LookupProvider()
	{
		final BPartnerId bPartnerId = BPartnerId.ofRepoId(getSelectedBankStatementLine().getC_BPartner_ID());
		return bankStatement_AllocateInvoicesService.getInvoiceLookupProvider_UnpaidByBpartner(bPartnerId);
	}

	@ProcessParamLookupValuesProvider(parameterName = C_INVOICE_3_ID_PARAM_NAME, numericKey = true, lookupSource = DocumentLayoutElementFieldDescriptor.LookupSource.lookup, lookupTableName = I_C_Invoice.Table_Name)
	public LookupValuesList invoice3LookupProvider()
	{
		final BPartnerId bPartnerId = BPartnerId.ofRepoId(getSelectedBankStatementLine().getC_BPartner_ID());
		return bankStatement_AllocateInvoicesService.getInvoiceLookupProvider_UnpaidByBpartner(bPartnerId);
	}

	@ProcessParamLookupValuesProvider(parameterName = C_INVOICE_4_ID_PARAM_NAME, numericKey = true, lookupSource = DocumentLayoutElementFieldDescriptor.LookupSource.lookup, lookupTableName = I_C_Invoice.Table_Name)
	public LookupValuesList invoice4LookupProvider()
	{
		final BPartnerId bPartnerId = BPartnerId.ofRepoId(getSelectedBankStatementLine().getC_BPartner_ID());
		return bankStatement_AllocateInvoicesService.getInvoiceLookupProvider_UnpaidByBpartner(bPartnerId);
	}

	@ProcessParamLookupValuesProvider(parameterName = C_INVOICE_5_ID_PARAM_NAME, numericKey = true, lookupSource = DocumentLayoutElementFieldDescriptor.LookupSource.lookup, lookupTableName = I_C_Invoice.Table_Name)
	public LookupValuesList invoice5LookupProvider()
	{
		final BPartnerId bPartnerId = BPartnerId.ofRepoId(getSelectedBankStatementLine().getC_BPartner_ID());
		return bankStatement_AllocateInvoicesService.getInvoiceLookupProvider_UnpaidByBpartner(bPartnerId);
	}

	@Override
	protected String doIt() throws Exception
	{
		just___DO_IT();

		return MSG_OK;
	}

	private void just___DO_IT()
	{
		final I_C_BankStatement bankStatement = getSelectedBankStatement();
		final I_C_BankStatementLine bankStatementLine = getSelectedBankStatementLine();
		final ImmutableList<InvoiceId> invoiceIds = getSelectedInvoices();

		final BankAccountId bankAccountId = BankAccountId.ofRepoId(bankStatement.getC_BP_BankAccount_ID());
		// TODO tbp @teo: not sure if stmt amount is the correct one. Help?
		//    problem is the bank statement line could already have some allocations, so i have to figure that out somehow
		//    sa vad cum sunt calculate stmt amount si trx amount
		//    -in mod normal stmt ar trebui sa fie importat din extrasul bancar
		//    	- cat e nealocat este stmt amount - trx amount -> asta tre sa folosesc eu
		final BigDecimal maxAmountForAllocation = bankStatementLine.getStmtAmt();
		// TODO tbp: please help with above
		final LocalDate paymentDateAcct = TimeUtil.asLocalDate(bankStatementLine.getStatementLineDate());
		final ImmutableList<PaymentId> paymentIds = new PaymentsForInvoicesCreator()
				.retrieveOrCreatePaymentsForInvoicesOldestFirst(invoiceIds, bankAccountId, maxAmountForAllocation, paymentDateAcct);

		if (paymentIds.size() == 0)
		{
			return;
		}

		if (paymentIds.size() == 1)
		{
			bankStatement_AllocateInvoicesService.handleSinglePaymentAllocation(bankStatementLine, paymentIds);
		}
		else
		{
			bankStatement_AllocateInvoicesService.handleMultiplePaymentsAllocation(bankStatementLine, paymentIds);
		}
	}

	private I_C_BankStatement getSelectedBankStatement()
	{
		final int bankStatementId = getRecord_ID();
		final IBankStatementDAO bankStatementDAO = this.bankStatementDAO;
		return bankStatementDAO.getById(bankStatementId);
	}

	private I_C_BankStatementLine getSelectedBankStatementLine()
	{
		final Integer lineId = getSelectedIncludedRecordIds(I_C_BankStatementLine.class).iterator().next();
		return bankStatementDAO.getLineById(lineId);
	}

	private ImmutableList<InvoiceId> getSelectedInvoices()
	{
		final HashSet<InvoiceId> invoiceIds = new HashSet<>();
		invoiceIds.add(c_invoice_1_id);
		invoiceIds.add(c_invoice_2_id);
		invoiceIds.add(c_invoice_3_id);
		invoiceIds.add(c_invoice_4_id);
		invoiceIds.add(c_invoice_5_id);
		invoiceIds.removeIf(Objects::isNull);

		return ImmutableList.copyOf(invoiceIds);
	}
}
