package de.metas.ui.web.accounting.process;

import org.adempiere.util.lang.impl.TableRecordReference;
import org.compiere.model.I_Fact_Acct;

import com.google.common.collect.ImmutableList;

import static org.adempiere.model.InterfaceWrapperHelper.getTableId;

import de.metas.acct.api.IFactAcctDAO;
import de.metas.document.engine.IDocument;
import de.metas.document.engine.IDocumentBL;
import de.metas.process.IProcessPrecondition;
import de.metas.process.IProcessPreconditionsContext;
import de.metas.process.JavaProcess;
import de.metas.process.ProcessExecutionResult.RecordsToOpen;
import de.metas.process.ProcessPreconditionsResolution;
import de.metas.util.Services;
import lombok.NonNull;

public class Fact_Acct_OpenLinkedFacts_SingleView extends JavaProcess implements IProcessPrecondition
{

	final IFactAcctDAO factAcctDAO = Services.get(IFactAcctDAO.class);
	final IDocumentBL documentBL = Services.get(IDocumentBL.class);

	@Override
	public ProcessPreconditionsResolution checkPreconditionsApplicable(@NonNull final IProcessPreconditionsContext context)
	{
		if (!context.isSingleSelection())
		{
			return ProcessPreconditionsResolution.rejectBecauseNotSingleSelection();
		}

		return ProcessPreconditionsResolution.accept();
	}

	@Override
	protected String doIt() throws Exception
	{
		final I_Fact_Acct factAcct = factAcctDAO.getById(getRecord_ID());

		final int factAcctTableId = getTableId(I_Fact_Acct.class);
		
		final TableRecordReference documentReference = TableRecordReference.of(factAcct.getAD_Table_ID(), factAcct.getRecord_ID());
		
		final IDocument document = documentBL.getDocument(documentReference);
		
		final  ImmutableList<TableRecordReference> linkedFactAccts = factAcctDAO.retrieveQueryForDocument(document)
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
