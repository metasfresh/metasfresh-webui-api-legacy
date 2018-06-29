package de.metas.ui.web.picking.pickingslot.process;

import java.util.List;

import org.adempiere.util.Services;
import org.adempiere.util.collections.ListUtils;
import org.compiere.model.I_C_OrderLine;
import org.springframework.beans.factory.annotation.Autowired;

import de.metas.handlingunits.picking.IHUPickingSlotBL;
import de.metas.order.OrderLineId;
import de.metas.process.ProcessPreconditionsResolution;
import de.metas.ui.web.document.filter.DocumentFilter;
import de.metas.ui.web.handlingunits.HUIdsFilterHelper;
import de.metas.ui.web.order.sales.hu.reservation.HUsReservationViewFactory;
import de.metas.ui.web.picking.husToPick.HUsToPickViewFactory;
import de.metas.ui.web.picking.pickingslot.PickingSlotRow;
import de.metas.ui.web.picking.pickingslot.PickingSlotRowId;
import de.metas.ui.web.view.CreateViewRequest;
import de.metas.ui.web.view.IView;
import de.metas.ui.web.view.IViewsRepository;
import de.metas.ui.web.view.ViewId;
import de.metas.ui.web.view.json.JSONViewDataType;
import lombok.NonNull;

/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2017 metas GmbH
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

/**
 * This process opens the HUsToPick view.
 *
 * @author metas-dev <dev@metasfresh.com>
 */
public class WEBUI_Picking_HUEditor_Launcher extends PickingSlotViewBasedProcess
{
	@Autowired
	private IViewsRepository viewsRepo;

	private final transient IHUPickingSlotBL huPickingSlotBL = Services.get(IHUPickingSlotBL.class);

	@Autowired
	private HUsToPickViewFactory husToPickViewFactory;

	@Override
	protected ProcessPreconditionsResolution checkPreconditionsApplicable()
	{
		if (!getSelectedRowIds().isSingleDocumentId())
		{
			return ProcessPreconditionsResolution.rejectBecauseNotSingleSelection();
		}
		return ProcessPreconditionsResolution.accept();
	}

	@Override
	protected String doIt()
	{
		final PickingSlotRowId pickingSlotRowId = getSingleSelectedRow().getPickingSlotRowId();
		final ViewId pickingSlotViewId = getView().getViewId();
		final int shipmentScheduleId = getView().getCurrentShipmentScheduleId();;

		final CreateViewRequest createViewRequest = husToPickViewFactory.createViewRequest(pickingSlotViewId, pickingSlotRowId, shipmentScheduleId);

		final DocumentFilter stickyFilters = createViewRequest.getStickyFilters().get(0); // TODO

		final IView view = viewsRepo
				.createView(CreateViewRequest
						.builder(HUsToPickViewFactory.WINDOW_ID)
						.addStickyFilters(stickyFilters)
						// .setParameter(VIEW_PARAM_PARENT_SALES_ORDER_LINE_ID, orderLineId)
						.build());

		// final List<Integer> availableHUIdsToPick = retrieveAvailableHuIdsForCurrentShipmentScheduleId();

		getResult().setWebuiIncludedViewIdToOpen(view.getViewId().getViewId());
		getResult().setWebuiViewProfileId("husToPick");
		return MSG_OK;
	}



	
}
