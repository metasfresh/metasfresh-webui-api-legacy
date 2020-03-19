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

import java.math.BigDecimal;

import org.compiere.model.I_C_BankStatement;
import org.compiere.model.I_C_BankStatementLine;
import org.compiere.model.I_C_Payment;

import com.google.common.collect.ImmutableSet;

import de.metas.bpartner.BPartnerId;
import de.metas.money.CurrencyId;
import de.metas.money.Money;
import de.metas.payment.PaymentId;
import de.metas.payment.api.IPaymentDAO;
import de.metas.process.IProcessPreconditionsContext;
import de.metas.process.Param;
import de.metas.process.ProcessPreconditionsResolution;
import de.metas.ui.web.process.descriptor.ProcessParamLookupValuesProvider;
import de.metas.ui.web.window.datatypes.LookupValuesList;
import de.metas.ui.web.window.descriptor.DocumentLayoutElementFieldDescriptor;
import de.metas.ui.web.window.model.lookup.LookupDataSourceFactory;
import de.metas.util.Services;
import lombok.NonNull;

public class C_BankStatementLine_AddBpartnerAndPayment extends BankStatementBasedProcess
{
	private static final String C_BPartner_ID_PARAM_NAME = "C_BPartner_ID";
	@Param(parameterName = C_BPartner_ID_PARAM_NAME)
	private BPartnerId bPartnerId;

	private static final String C_Payment_ID_PARAM_NAME = "C_Payment_ID";
	@Param(parameterName = C_Payment_ID_PARAM_NAME)
	private PaymentId paymentId;

	@Override
	public ProcessPreconditionsResolution checkPreconditionsApplicable(@NonNull final IProcessPreconditionsContext context)
	{
		return checkBankStatementIsDraftOrInProcessOrCompleted(context)
				.and(() -> checkSingleLineSelectedWhichIsNotReconciled(context));
	}

	@ProcessParamLookupValuesProvider(parameterName = C_Payment_ID_PARAM_NAME, numericKey = true, lookupSource = DocumentLayoutElementFieldDescriptor.LookupSource.lookup, lookupTableName = I_C_Payment.Table_Name)
	private LookupValuesList paymentLookupProvider()
	{
		final I_C_BankStatementLine line = getSingleSelectedBankStatementLine();
		final CurrencyId currencyId = CurrencyId.ofRepoId(line.getC_Currency_ID());
		final boolean isReceipt = line.getStmtAmt().signum() >= 0;
		final BigDecimal paymentAmount = isReceipt ? line.getStmtAmt() : line.getStmtAmt().negate();
		final Money money = Money.of(paymentAmount, currencyId);

		final ImmutableSet<PaymentId> paymentIds = Services.get(IPaymentDAO.class).retrieveAllMatchingPayments(isReceipt, bPartnerId, money);

		return LookupDataSourceFactory.instance.searchInTableLookup(I_C_Payment.Table_Name).findByIdsOrdered(paymentIds);
	}

	@Override
	protected String doIt()
	{
		final I_C_BankStatement bankStatement = getSelectedBankStatement();
		final I_C_BankStatementLine bankStatementLine = getSingleSelectedBankStatementLine();

		bankStatementLine.setC_BPartner_ID(bPartnerId.getRepoId());
		bankStatementPaymentBL.setOrCreateAndLinkPaymentToBankStatementLine(bankStatement, bankStatementLine, paymentId);

		return MSG_OK;
	}
}
