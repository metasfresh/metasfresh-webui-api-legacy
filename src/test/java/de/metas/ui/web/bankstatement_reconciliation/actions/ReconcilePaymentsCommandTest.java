package de.metas.ui.web.bankstatement_reconciliation.actions;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.adempiere.test.AdempiereTestHelper;
import org.adempiere.test.AdempiereTestWatcher;
import org.compiere.SpringContextHolder;
import org.compiere.model.I_C_BP_BankAccount;
import org.compiere.model.I_C_BPartner;
import org.compiere.model.I_C_BankStatement;
import org.compiere.model.I_C_Payment;
import org.compiere.util.Trace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import de.metas.attachments.AttachmentEntryService;
import de.metas.banking.api.BankAccountId;
import de.metas.banking.model.BankStatementId;
import de.metas.banking.model.BankStatementLineId;
import de.metas.banking.model.BankStatementLineReference;
import de.metas.banking.payment.IBankStatmentPaymentBL;
import de.metas.banking.service.BankStatementCreateRequest;
import de.metas.banking.service.BankStatementLineCreateRequest;
import de.metas.banking.service.IBankStatementBL;
import de.metas.banking.service.IBankStatementDAO;
import de.metas.banking.service.impl.BankStatementBL;
import de.metas.bpartner.BPartnerId;
import de.metas.business.BusinessTestHelper;
import de.metas.currency.CurrencyRepository;
import de.metas.document.engine.DocStatus;
import de.metas.i18n.IMsgBL;
import de.metas.money.CurrencyId;
import de.metas.money.Money;
import de.metas.money.MoneyService;
import de.metas.organization.OrgId;
import de.metas.payment.PaymentId;
import de.metas.payment.TenderType;
import de.metas.payment.api.DefaultPaymentBuilder;
import de.metas.payment.api.IPaymentBL;
import de.metas.payment.api.IPaymentDAO;
import de.metas.payment.esr.api.impl.ESRImportBL;
import de.metas.ui.web.bankstatement_reconciliation.BankStatementLineAndPaymentsToReconcileRepository;
import de.metas.ui.web.bankstatement_reconciliation.BankStatementLineRow;
import de.metas.ui.web.bankstatement_reconciliation.PaymentToReconcileRow;
import de.metas.util.Services;
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

@ExtendWith(AdempiereTestWatcher.class)
public class ReconcilePaymentsCommandTest
{
	private final IBankStatementDAO bankStatementDAO = Services.get(IBankStatementDAO.class);
	private final IPaymentBL paymentBL = Services.get(IPaymentBL.class);
	private final IPaymentDAO paymentDAO = Services.get(IPaymentDAO.class);

	private final IMsgBL msgBL = Services.get(IMsgBL.class);
	private final IBankStatmentPaymentBL bankStatmentPaymentBL = Services.get(IBankStatmentPaymentBL.class);
	private ESRImportBL esrImportBL;
	private BankStatementLineAndPaymentsToReconcileRepository rowsRepo;

	private static final LocalDate statementDate = LocalDate.parse("2020-03-21");
	private static final LocalDate paymentDate = LocalDate.parse("2020-03-10");
	private CurrencyId euroCurrencyId;
	private BankAccountId euroOrgBankAccountId;

	@BeforeEach
	public void beforeEach()
	{
		AdempiereTestHelper.get().init();

		final CurrencyRepository currencyRepository = new CurrencyRepository();
		SpringContextHolder.registerJUnitBean(new MoneyService(currencyRepository));

		final BankStatementBL bankStatementBL = new BankStatementBL()
		{
			public void unpost(I_C_BankStatement bankStatement)
			{
				System.out.println("In JUnit test BankStatementBL.unpost() does nothing"
						+ "\n\t bank statement: " + bankStatement
						+ "\n\t called via " + Trace.toOneLineStackTraceString());
			}
		};
		Services.registerService(IBankStatementBL.class, bankStatementBL);

		esrImportBL = new ESRImportBL(AttachmentEntryService.createInstanceForUnitTesting());

		this.rowsRepo = new BankStatementLineAndPaymentsToReconcileRepository(currencyRepository);
		rowsRepo.setBpartnerLookup(new MockedBPartnerLookupDataSource());

		createMasterdata();
	}

	private void createMasterdata()
	{
		euroCurrencyId = BusinessTestHelper.getEURCurrencyId();
		euroOrgBankAccountId = createOrgBankAccount(euroCurrencyId);
	}

	private BPartnerId createCustomer()
	{
		I_C_BPartner customer = BusinessTestHelper.createBPartner("le customer");
		return BPartnerId.ofRepoId(customer.getC_BPartner_ID());
	}

	private BankAccountId createOrgBankAccount(final CurrencyId eurCurrencyId)
	{
		final String metasfreshIban = "123456";
		final I_C_BPartner metasfreshBPartner = BusinessTestHelper.createBPartner("metasfresh");
		final I_C_BP_BankAccount metasfreshBankAccount = BusinessTestHelper.createBpBankAccount(BPartnerId.ofRepoId(metasfreshBPartner.getC_BPartner_ID()), eurCurrencyId, metasfreshIban);
		return BankAccountId.ofRepoId(metasfreshBankAccount.getC_BP_BankAccount_ID());
	}

	@Builder(builderMethodName = "bankStatement", builderClassName = "BankStatementBuilder")
	private BankStatementId createBankStatement(@NonNull final DocStatus docStatus)
	{
		final BankStatementId bankStatementId = bankStatementDAO.createBankStatement(BankStatementCreateRequest.builder()
				.orgId(OrgId.ANY)
				.orgBankAccountId(euroOrgBankAccountId)
				.name("Bank Statement 1")
				.statementDate(statementDate)
				.build());

		final I_C_BankStatement bankStatement = bankStatementDAO.getById(bankStatementId);
		bankStatement.setDocStatus(docStatus.getCode());
		bankStatementDAO.save(bankStatement);

		return bankStatementId;
	}

	@Builder(builderMethodName = "bankStatementLineRow", builderClassName = "BankStatementLineRowBuilder")
	private BankStatementLineRow createBankStatementLineRow(
			@NonNull final BankStatementId bankStatementId,
			@NonNull final Money statementAmt)
	{
		final BankStatementLineId bankStatementLineId = bankStatementDAO.createBankStatementLine(BankStatementLineCreateRequest.builder()
				.bankStatementId(bankStatementId)
				.orgId(OrgId.ANY)
				.lineNo(10)
				.statementLineDate(statementDate)
				.statementAmt(statementAmt)
				.build());

		return rowsRepo.getBankStatementLineRowsByIds(ImmutableSet.of(bankStatementLineId)).get(0);
	}

	@Builder(builderMethodName = "paymentRow", builderClassName = "PaymentRowBuilder")
	private PaymentToReconcileRow createPaymentRow(
			@NonNull final Boolean inboundPayment,
			@NonNull final BPartnerId customerId,
			@NonNull final Money paymentAmt)
	{
		final DefaultPaymentBuilder builder = inboundPayment
				? paymentBL.newInboundReceiptBuilder()
				: paymentBL.newOutboundPaymentBuilder();

		final I_C_Payment payment = builder
				.adOrgId(OrgId.ANY)
				.bpartnerId(customerId)
				.orgBankAccountId(euroOrgBankAccountId)
				.currencyId(paymentAmt.getCurrencyId())
				.payAmt(paymentAmt.toBigDecimal())
				.dateAcct(paymentDate)
				.dateTrx(paymentDate)
				.tenderType(TenderType.Check)
				.createAndProcess();
		payment.setDocumentNo("documentNo-" + payment.getC_Payment_ID());
		paymentDAO.save(payment);
		final PaymentId paymentId = PaymentId.ofRepoId(payment.getC_Payment_ID());

		return rowsRepo.getPaymentToReconcileRowsByIds(ImmutableSet.of(paymentId)).get(0);
	}

	@Test
	public void test()
	{
		final BankStatementId bankStatementId = bankStatement()
				.docStatus(DocStatus.Drafted)
				.build();

		final BankStatementLineRow bankStatementLineRow = bankStatementLineRow()
				.bankStatementId(bankStatementId)
				.statementAmt(Money.of("1000", euroCurrencyId))
				.build();

		final BPartnerId customerId = createCustomer();
		final List<PaymentToReconcileRow> paymentRows = ImmutableList.of(
				paymentRow().inboundPayment(true).customerId(customerId).paymentAmt(Money.of("1000", euroCurrencyId)).build());

		ReconcilePaymentsCommand.builder()
				.msgBL(msgBL)
				.bankStatmentPaymentBL(bankStatmentPaymentBL)
				.esrImportBL(esrImportBL)
				//
				.request(ReconcilePaymentsRequest.builder()
						.selectedBankStatementLine(bankStatementLineRow)
						.selectedPaymentsToReconcile(paymentRows)
						.build())
				//
				.build()
				.execute();

		//
		// Assertions
		{
			final ImmutableList<BankStatementLineReference> lineReferences = ImmutableList.copyOf(bankStatementDAO.retrieveLineReferences(bankStatementLineRow.getBankStatementLineId()));
			assertThat(lineReferences).hasSize(1);

			final BankStatementLineReference lineReference = lineReferences.get(0);
			assertThat(lineReference.getPaymentId()).isEqualTo(paymentRows.get(0).getPaymentId());
			assertThat(lineReference.getBpartnerId()).isEqualTo(customerId);
			assertThat(lineReference.getTrxAmt()).isEqualTo(Money.of("1000", euroCurrencyId));
		}
	}
}
