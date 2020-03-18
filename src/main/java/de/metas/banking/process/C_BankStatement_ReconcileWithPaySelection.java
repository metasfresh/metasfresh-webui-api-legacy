package de.metas.banking.process;

import java.util.Set;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.SpringContextHolder;

import de.metas.banking.model.PaySelectionId;
import de.metas.banking.payment.IPaySelectionBL;
import de.metas.payment.PaymentId;
import de.metas.process.IProcessPreconditionsContext;
import de.metas.process.Param;
import de.metas.process.ProcessExecutionResult.ViewOpenTarget;
import de.metas.process.ProcessExecutionResult.WebuiViewToOpen;
import de.metas.process.ProcessPreconditionsResolution;
import de.metas.ui.web.bankstatement_reconciliation.BankStatementReconciliationViewFactory;
import de.metas.ui.web.bankstatement_reconciliation.BankStatementReconciliationView;
import de.metas.ui.web.bankstatement_reconciliation.BanksStatementReconciliationViewCreateRequest;
import de.metas.ui.web.view.ViewId;
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

public class C_BankStatement_ReconcileWithPaySelection extends BankStatementBasedProcess
{
	private final IPaySelectionBL paySelectionBL = Services.get(IPaySelectionBL.class);
	private final BankStatementReconciliationViewFactory bankStatementReconciliationViewFactory = SpringContextHolder.instance.getBean(BankStatementReconciliationViewFactory.class);

	@Param(parameterName = "C_PaySelection_ID", mandatory = true)
	private PaySelectionId paySelectionId;

	@Override
	public ProcessPreconditionsResolution checkPreconditionsApplicable(@NonNull final IProcessPreconditionsContext context)
	{
		return checkBankStatementIsDraftOrInProcessOrCompleted(context)
				.and(() -> checkSingleLineSelectedWhichIsNotReconciled(context));
	}

	@Override
	protected String doIt()
	{
		final Set<PaymentId> paymentIds = paySelectionBL.getPaymentIds(paySelectionId);
		if (paymentIds.isEmpty())
		{
			throw new AdempiereException("@NoPayments@");
		}

		final BankStatementReconciliationView view = bankStatementReconciliationViewFactory.createView(BanksStatementReconciliationViewCreateRequest.builder()
				.bankStatementLineId(getSingleSelectedBankStatementLineId())
				.paymentIds(paymentIds)
				.build());
		final ViewId viewId = view.getViewId();

		getResult().setWebuiViewToOpen(WebuiViewToOpen.builder()
				.viewId(viewId.toJson())
				.target(ViewOpenTarget.ModalOverlay)
				.build());

		return MSG_OK;
	}
}
