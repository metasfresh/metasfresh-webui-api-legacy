package de.metas.ui.web.bankstatement_reconciliation;

import java.util.List;
import java.util.Map;

import org.adempiere.util.lang.SynchronizedMutable;
import org.adempiere.util.lang.impl.TableRecordReferenceSet;

import com.google.common.collect.ImmutableSet;

import de.metas.banking.model.BankStatementLineId;
import de.metas.banking.model.I_C_BankStatementLine;
import de.metas.ui.web.view.template.IRowsData;
import de.metas.ui.web.view.template.ImmutableRowsIndex;
import de.metas.ui.web.window.datatypes.DocumentId;
import de.metas.ui.web.window.datatypes.DocumentIdsSelection;
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

public class BankStatementLineRows implements IRowsData<BankStatementLineRow>
{
	public static BankStatementLineRows cast(final IRowsData<BankStatementLineRow> rowsData)
	{
		return (BankStatementLineRows)rowsData;
	}

	private final BankStatementLineAndPaymentsToReconcileRepository repository;
	private final SynchronizedMutable<ImmutableRowsIndex<BankStatementLineRow>> rowsHolder;

	@Builder
	private BankStatementLineRows(
			@NonNull final BankStatementLineAndPaymentsToReconcileRepository repository,
			@NonNull final List<BankStatementLineRow> rows)
	{
		this.repository = repository;
		this.rowsHolder = SynchronizedMutable.of(ImmutableRowsIndex.of(rows));
	}

	@Override
	public Map<DocumentId, BankStatementLineRow> getDocumentId2TopLevelRows()
	{
		return rowsHolder.getValue().getDocumentId2TopLevelRows();
	}

	@Override
	public DocumentIdsSelection getDocumentIdsToInvalidate(@NonNull final TableRecordReferenceSet recordRefs)
	{
		final ImmutableRowsIndex<BankStatementLineRow> rows = rowsHolder.getValue();
		return recordRefs.streamIds(I_C_BankStatementLine.Table_Name, BankStatementLineId::ofRepoId)
				.map(BankStatementLineRow::convertBankStatementLineIdToDocumentId)
				.filter(rows::isRelevantForRefreshing)
				.collect(DocumentIdsSelection.toDocumentIdsSelection());
	}

	@Override
	public void invalidateAll()
	{
		invalidate(DocumentIdsSelection.ALL);
	}

	@Override
	public void invalidate(final DocumentIdsSelection rowIds)
	{
		final ImmutableSet<BankStatementLineId> bankStatementLineIds = rowsHolder
				.getValue()
				.getRecordIdsToRefresh(rowIds, BankStatementLineRow::convertDocumentIdToBankStatementLineId);

		final List<BankStatementLineRow> newRows = repository.getBankStatementLineRowsByIds(bankStatementLineIds);
		rowsHolder.compute(rows -> rows.replacingRows(rowIds, newRows));
	}
}
