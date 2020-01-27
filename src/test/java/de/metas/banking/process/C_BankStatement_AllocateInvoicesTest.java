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
import de.metas.banking.model.BankStatementId;
import de.metas.banking.model.I_C_BankStatement;
import de.metas.banking.model.I_C_BankStatementLine;
import de.metas.banking.model.I_C_Payment;
import de.metas.banking.process.bankstatement_allocateInvoicesProcess.BankStatement_AllocateInvoicesService;
import de.metas.bpartner.BPartnerId;
import de.metas.business.BusinessTestHelper;
import de.metas.money.CurrencyId;
import de.metas.util.time.SystemTime;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.test.AdempiereTestHelper;
import org.compiere.model.I_C_BP_BankAccount;
import org.compiere.model.I_C_BPartner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class C_BankStatement_AllocateInvoicesTest
{
	private final BankStatementTestHelper bankStatementTestHelper = new BankStatementTestHelper();
	private BankStatement_AllocateInvoicesService bankStatement_allocateInvoicesService = new BankStatement_AllocateInvoicesService();

	@BeforeEach
	void setUp()
	{
		AdempiereTestHelper.get().init();
	}

	@Test
	void a()
	{
		//
		// create test data
		final Timestamp statementDate = SystemTime.asTimestamp();
		final BigDecimal beginningBalance = BigDecimal.ZERO;
		final CurrencyId eurCurrencyId = BusinessTestHelper.getEURCurrencyId();

		final I_C_BPartner metasfreshBPartner = BusinessTestHelper.createBPartner("metasfresh");

		final String metasfreshIban = "123456";
		final I_C_BP_BankAccount metasfreshBankAccount = BusinessTestHelper.createBpBankAccount(BPartnerId.ofRepoId(metasfreshBPartner.getC_BPartner_ID()), eurCurrencyId, metasfreshIban);

		final I_C_BankStatement bankStatement = BankStatementTestHelper.createBankStatement(BankAccountId.ofRepoId(metasfreshBankAccount.getC_BP_BankAccount_ID()), "Bank Statement 1", statementDate, beginningBalance);

		final I_C_BP_BankAccount customerBankAccount = BusinessTestHelper.createBpBankAccount(BPartnerId.ofRepoId(metasfreshBPartner.getC_BPartner_ID()), eurCurrencyId, null);
		final Timestamp valutaDate = SystemTime.asTimestamp();

		final BigDecimal lineStmtAmt = BigDecimal.valueOf(123);
		final I_C_BankStatementLine bsl = BankStatementTestHelper.createBankStatementLine(BankStatementId.ofRepoId(bankStatement.getC_BankStatement_ID()),
				BankAccountId.ofRepoId(customerBankAccount.getC_BP_BankAccount_ID()),
				10,
				statementDate,
				valutaDate,
				lineStmtAmt,
				eurCurrencyId
		);


		//
		// begin testing
		//

		//
		// Call the method
		C_BankStatement_AllocateInvoices process = new C_BankStatement_AllocateInvoices(bankStatement_allocateInvoicesService);
		process.just___DO_IT(bankStatement, bsl, ImmutableList.of());

		/*
		expectations:
		- the Bank Statement Line has 1 Payment created, for the whole statement amount
		- the Payment has the flag "Reconciled" set
		 */
		InterfaceWrapperHelper.refresh(bsl);
		assertNotEquals(0, bsl.getC_Payment_ID());
		final I_C_Payment payment1 = InterfaceWrapperHelper.load(bsl.getC_Payment_ID(), I_C_Payment.class);
		assertNotNull(payment1);
		assertEquals(lineStmtAmt, payment1.getPayAmt());

	}
}
