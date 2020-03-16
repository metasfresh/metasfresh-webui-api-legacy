package de.metas.ui.web.bankstatement_reconciliation;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.adempiere.exceptions.AdempiereException;

import com.google.common.collect.ImmutableList;

import de.metas.banking.model.BankStatementLineId;
import de.metas.i18n.IMsgBL;
import de.metas.process.RelatedProcessDescriptor;
import de.metas.ui.web.view.CreateViewRequest;
import de.metas.ui.web.view.DefaultViewsRepositoryStorage;
import de.metas.ui.web.view.IView;
import de.metas.ui.web.view.IViewFactory;
import de.metas.ui.web.view.IViewsIndexStorage;
import de.metas.ui.web.view.IViewsRepository;
import de.metas.ui.web.view.ViewCloseAction;
import de.metas.ui.web.view.ViewFactory;
import de.metas.ui.web.view.ViewId;
import de.metas.ui.web.view.ViewProfileId;
import de.metas.ui.web.view.descriptor.IncludedViewLayout;
import de.metas.ui.web.view.descriptor.ViewLayout;
import de.metas.ui.web.view.json.JSONViewDataType;
import de.metas.ui.web.window.datatypes.WindowId;
import de.metas.util.Check;
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

@ViewFactory(windowId = BanksStatementReconciliationViewFactory.WINDOW_ID_String)
public class BanksStatementReconciliationViewFactory implements IViewFactory, IViewsIndexStorage
{
	static final String WINDOW_ID_String = "bankStatementReconciliation";
	public static final WindowId WINDOW_ID = WindowId.fromJson(WINDOW_ID_String);

	private final IMsgBL msgBL = Services.get(IMsgBL.class);
	private final BankStatementLineAndPaymentsToReconcileRepository rowsRepo;
	private final DefaultViewsRepositoryStorage views = new DefaultViewsRepositoryStorage();

	public BanksStatementReconciliationViewFactory(
			@NonNull final BankStatementLineAndPaymentsToReconcileRepository rowsRepo)
	{
		this.rowsRepo = rowsRepo;
	}

	@Override
	public void setViewsRepository(final IViewsRepository viewsRepository)
	{
	}

	@Override
	public ViewLayout getViewLayout(final WindowId windowId, final JSONViewDataType viewDataType, final ViewProfileId profileId)
	{
		Check.assumeEquals(windowId, WINDOW_ID, "windowId");

		return ViewLayout.builder()
				.setWindowId(WINDOW_ID)
				.setCaption(msgBL.translatable("BankStatementReconciliation"))
				.setAllowOpeningRowDetails(false)
				.allowViewCloseAction(ViewCloseAction.DONE)
				.setIncludedViewLayout(IncludedViewLayout.builder()
						.openOnSelect(true)
						.blurWhenOpen(false)
						.build())
				.addElementsFromViewRowClass(BankStatementLineRow.class, viewDataType)
				.build();
	}

	@Override
	public BanksStatementReconciliationView createView(final CreateViewRequest request)
	{
		final ViewId bankStatementViewId = request.getViewId();
		bankStatementViewId.assertWindowId(WINDOW_ID);

		final Set<BankStatementLineId> bankStatementLineIds = BankStatementLineId.fromIntSet(request.getFilterOnlyIds());
		if (bankStatementLineIds.isEmpty())
		{
			throw new AdempiereException("@NoSelection@");
		}

		final BankStatementLineAndPaymentsRows rows = null; // TODO

		return BanksStatementReconciliationView.builder()
				.bankStatementViewId(bankStatementViewId)
				.rows(rows)
				.paymentToReconcilateProcesses(getPaymentToReconcilateProcesses())
				.build();
	}

	@Override
	public WindowId getWindowId()
	{
		return WINDOW_ID;
	}

	@Override
	public void put(final IView view)
	{
		views.put(view);
	}

	@Override
	public BanksStatementReconciliationView getByIdOrNull(final ViewId viewId)
	{
		return BanksStatementReconciliationView.cast(views.getByIdOrNull(viewId));
	}

	@Override
	public void closeById(final ViewId viewId, final ViewCloseAction closeAction)
	{
		views.closeById(viewId, closeAction);
	}

	@Override
	public Stream<IView> streamAllViews()
	{
		return views.streamAllViews();
	}

	@Override
	public void invalidateView(final ViewId viewId)
	{
		views.invalidateView(viewId);
	}

	private List<RelatedProcessDescriptor> getPaymentToReconcilateProcesses()
	{
		// TODO Auto-generated method stub
		return ImmutableList.of();
	}
}
