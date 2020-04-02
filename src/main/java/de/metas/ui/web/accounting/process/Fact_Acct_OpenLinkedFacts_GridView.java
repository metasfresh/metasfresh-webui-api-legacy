package de.metas.ui.web.accounting.process;

import org.adempiere.util.lang.impl.TableRecordReference;
import org.compiere.model.I_Fact_Acct;

import com.google.common.collect.ImmutableList;

import static org.adempiere.model.InterfaceWrapperHelper.getTableId;

import de.metas.acct.api.IFactAcctDAO;
import de.metas.document.engine.IDocument;
import de.metas.document.engine.IDocumentBL;
import de.metas.process.IProcessPrecondition;
import de.metas.process.ProcessExecutionResult.RecordsToOpen;
import de.metas.process.ProcessPreconditionsResolution;
import de.metas.ui.web.process.adprocess.ViewBasedProcessTemplate;
import de.metas.ui.web.view.IViewRow;
import de.metas.ui.web.window.datatypes.DocumentId;
import de.metas.util.Services;

public class Fact_Acct_OpenLinkedFacts_GridView extends ViewBasedProcessTemplate implements IProcessPrecondition
{
	final IFactAcctDAO factAcctDAO = Services.get(IFactAcctDAO.class);
	final IDocumentBL documentBL = Services.get(IDocumentBL.class);

	@Override
	public ProcessPreconditionsResolution checkPreconditionsApplicable()
	{
		if (getSelectedRowIds().isEmpty())
		{
			return ProcessPreconditionsResolution.rejectBecauseNoSelection();
		}
		if (!getSelectedRowIds().isSingleDocumentId())
		{
			return ProcessPreconditionsResolution.rejectBecauseNotSingleSelection();
		}

		return ProcessPreconditionsResolution.accept();
	}

	@Override
	protected String doIt() throws Exception
	{
		final IViewRow singleSelectedRow = getSingleSelectedRow();

		final DocumentId factAcctId = singleSelectedRow.getId();

		final I_Fact_Acct factAcct = factAcctDAO.getById(factAcctId.toInt());

		final int factAcctTableId = getTableId(I_Fact_Acct.class);

		final TableRecordReference documentReference = TableRecordReference.of(factAcct.getAD_Table_ID(), factAcct.getRecord_ID());

		final IDocument document = documentBL.getDocument(documentReference);

		ImmutableList<TableRecordReference> linkedFactAccts = factAcctDAO.retrieveQueryForDocument(document)
				.create()
				.listIds()
				.stream()
				.map(recordId -> TableRecordReference.of(factAcctTableId, recordId))
				.collect(ImmutableList.toImmutableList());

		getResult().setRecordToOpen(RecordsToOpen.builder()
				.records(linkedFactAccts)
				.automaticallySetReferencingDocumentPaths(false)
				.build());

		return MSG_OK;
	}

}
