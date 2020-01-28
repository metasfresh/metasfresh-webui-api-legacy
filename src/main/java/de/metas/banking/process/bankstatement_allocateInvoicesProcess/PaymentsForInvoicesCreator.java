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
import de.metas.allocation.api.IAllocationDAO;
import de.metas.banking.api.BankAccountId;
import de.metas.bpartner.BPartnerId;
import de.metas.invoice.InvoiceId;
import de.metas.money.CurrencyId;
import de.metas.organization.OrgId;
import de.metas.payment.PaymentId;
import de.metas.payment.TenderType;
import de.metas.payment.api.DefaultPaymentBuilder;
import de.metas.payment.api.IPaymentBL;
import de.metas.util.Services;
import lombok.NonNull;
import org.adempiere.invoice.service.IInvoiceDAO;
import org.compiere.model.I_C_Invoice;
import org.compiere.model.I_C_Payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class PaymentsForInvoicesCreator
{
	private final IPaymentBL paymentBL = Services.get(IPaymentBL.class);
	private final IInvoiceDAO invoiceDAO = Services.get(IInvoiceDAO.class);
	private final IAllocationDAO allocationDAO = Services.get(IAllocationDAO.class);

	/**
	 * Iterate over the selected invoices and create/retrieve payments, until the grand total of the invoice or the current line amount is reached
	 */
	public ImmutableList<PaymentId> retrieveOrCreatePaymentsForInvoicesOldestFirst(
			@NonNull final ImmutableList<InvoiceId> invoiceIds,
			@NonNull final BankAccountId bankAccountId,
			@NonNull final BigDecimal maxAmountForAllocation,
			@NonNull final LocalDate paymentDateAcct,
			@NonNull final OrgId adOrgId,
			@NonNull final BPartnerId bpartnerId,
			@NonNull final CurrencyId currencyId
	)
	{
		BigDecimal amountLeftForAllocation = maxAmountForAllocation;

		final ImmutableList.Builder<PaymentId> paymentIdsAccumulator = ImmutableList.builder();

		if (invoiceIds.isEmpty())
		{
			//
			// case when the user has not selected any invoices
			final boolean isReceipt = amountLeftForAllocation.signum() >= 0;
			final BigDecimal payAmt = isReceipt ? amountLeftForAllocation : amountLeftForAllocation.negate();
			final PaymentId paymentId = createAndCompletePaymentNoInvoice(bankAccountId, paymentDateAcct, payAmt, isReceipt, adOrgId, bpartnerId, currencyId);

			paymentIdsAccumulator.add(paymentId);
		}
		else
		{
			// TODO tbp: this is disables until i clarify with mark what do we do with the invoice numbers in the description field
			// //
			// // case when the user has selected some invoices
			// // TODO tbp: order invoices by date, asc, since we want to try and pay the oldest invoices first. note: id order != date order
			// for (final InvoiceId invoiceId : invoiceIds)
			// {
			//
			// 	if (amountLeftForAllocation.compareTo(BigDecimal.ZERO) <= 0)
			// 	{
			// 		break;
			// 	}
			// TODO tbp: handle negative amount (for outbound payments)
			// 	amountLeftForAllocation = selectOrCreatePaymentsForInvoice(invoiceId, bankAccountId, amountLeftForAllocation, paymentIdsAccumulator, paymentDateAcct);
			// }
		}
		return paymentIdsAccumulator.build();
	}

	private BigDecimal selectOrCreatePaymentsForInvoice(
			final InvoiceId invoiceId,
			final BankAccountId bankAccountId,
			/*not final*/ BigDecimal amountLeftForAllocation,
			final ImmutableList.Builder<PaymentId> paymentIdsAccumulator,
			final LocalDate paymentDateAcct)
	{
		final I_C_Invoice invoice = invoiceDAO.getByIdInTrx(invoiceId);

		// only allocate payments which summed, are less than the amount left for allocation
		final List<I_C_Payment> payments = allocationDAO.retrieveInvoicePayments(invoice);
		for (final I_C_Payment payment : payments)
		{
			if (amountLeftForAllocation.compareTo(BigDecimal.ZERO) <= 0)
			{
				break;
			}

			// TODO: I am not sure here so i'm asking (teo's also not sure):
			//   what happens in the case where a portion of the payment is allocated?
			//   ie. paytment amount = 600, allocated amount = 100.
			//   how to handle this case?
			if (payment.isReconciled())
			{
				continue;
			}

			final BigDecimal payAmt = payment.getPayAmt();
			// if amount left for allocation - pay amount < 0 => skip this payment
			if (amountLeftForAllocation.subtract(payAmt).compareTo(BigDecimal.ZERO) < 0)
			{
				continue;
			}

			amountLeftForAllocation = amountLeftForAllocation.subtract(payAmt);
			paymentIdsAccumulator.add(PaymentId.ofRepoId(payment.getC_Payment_ID()));
		}

		// if the invoice is paid, there's no other payment to create
		if (invoice.isPaid())
		{
			return amountLeftForAllocation;
		}

		final BigDecimal openAmt = invoiceDAO.retrieveOpenAmt(invoiceId).getAsBigDecimal();
		final BigDecimal openAmtSelectedToAllocate = openAmt.min(amountLeftForAllocation);

		amountLeftForAllocation = amountLeftForAllocation.subtract(openAmtSelectedToAllocate);
		final PaymentId paymentId = createAndCompletePayment(bankAccountId, paymentDateAcct, invoice, openAmtSelectedToAllocate);
		paymentIdsAccumulator.add(paymentId);

		return amountLeftForAllocation;
	}

	private PaymentId createAndCompletePayment(final BankAccountId bankAccountId, final LocalDate dateAcct, final I_C_Invoice invoice, final BigDecimal payAmt)
	{
		final I_C_Payment payment = paymentBL.newBuilderOfInvoice(invoice)
				.bpBankAccountId(bankAccountId)
				.payAmt(payAmt)
				// .currencyId() // already set by the builder of invoice
				.dateAcct(dateAcct)
				.dateTrx(dateAcct)
				.description("Automatically created from Invoice open amount during BankStatementLine allocation.")
				.tenderType(TenderType.DirectDeposit)
				.createAndProcess();
		return PaymentId.ofRepoId(payment.getC_Payment_ID());
	}

	private PaymentId createAndCompletePaymentNoInvoice(
			final BankAccountId bankAccountId,
			final LocalDate dateAcct,
			final BigDecimal payAmt,
			final boolean isReceipt,
			final OrgId adOrgId,
			final BPartnerId bpartnerId,
			final CurrencyId currencyId
	)
	{
		final DefaultPaymentBuilder paymentBuilder;

		if (isReceipt)
		{
			paymentBuilder = paymentBL.newInboundReceiptBuilder();
		}
		else
		{
			paymentBuilder = paymentBL.newOutboundPaymentBuilder();
		}

		final I_C_Payment payment = paymentBuilder
				.adOrgId(adOrgId)
				.bpartnerId(bpartnerId)
				.bpBankAccountId(bankAccountId)
				.currencyId(currencyId)
				.payAmt(payAmt)
				.dateAcct(dateAcct)
				.dateTrx(dateAcct)
				.description("Automatically created from Invoice open amount during BankStatementLine allocation.")
				.tenderType(TenderType.DirectDeposit)
				.createAndProcess();

		return PaymentId.ofRepoId(payment.getC_Payment_ID());
	}
}


