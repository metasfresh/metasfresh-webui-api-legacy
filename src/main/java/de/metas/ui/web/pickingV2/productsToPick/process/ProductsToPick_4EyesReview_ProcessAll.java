package de.metas.ui.web.pickingV2.productsToPick.process;

import java.util.List;
import java.util.Set;

import de.metas.shipping.model.ShipperTransportationId;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_M_Shipper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import de.metas.handlingunits.HuId;
import de.metas.handlingunits.IHandlingUnitsDAO;
import de.metas.handlingunits.model.I_M_HU;
import de.metas.handlingunits.picking.PickingCandidate;
import de.metas.handlingunits.picking.PickingCandidateId;
import de.metas.handlingunits.picking.PickingCandidateService;
import de.metas.handlingunits.shipmentschedule.api.HUShippingFacade;
import de.metas.handlingunits.shipmentschedule.async.GenerateInOutFromHU.BillAssociatedInvoiceCandidates;
import de.metas.process.IProcessDefaultParameter;
import de.metas.process.IProcessDefaultParametersProvider;
import de.metas.process.IProcessParametersCallout;
import de.metas.process.Param;
import de.metas.process.ProcessPreconditionsResolution;
import de.metas.shipping.ShipperId;
import de.metas.shipping.api.IShipperTransportationDAO;
import de.metas.shipping.model.I_M_ShipperTransportation;
import de.metas.ui.web.pickingV2.productsToPick.rows.ProductsToPickRow;
import de.metas.util.Services;

/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2018 metas GmbH
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

public class ProductsToPick_4EyesReview_ProcessAll extends ProductsToPickViewBasedProcess implements IProcessDefaultParametersProvider, IProcessParametersCallout
{
	private final IHandlingUnitsDAO handlingUnitsRepo = Services.get(IHandlingUnitsDAO.class);
	private final IShipperTransportationDAO shipperTransportationRepo = Services.get(IShipperTransportationDAO.class);

	@Autowired
	private PickingCandidateService pickingCandidatesService;

	@Param(parameterName = I_M_Shipper.COLUMNNAME_M_Shipper_ID, mandatory = true)
	private int shipperRecordId;

	@Param(parameterName = I_M_ShipperTransportation.COLUMNNAME_M_ShipperTransportation_ID, mandatory = true)
	private int shipperTransportationId;

	@Override
	protected ProcessPreconditionsResolution checkPreconditionsApplicable()
	{
		if (!isPickerProfile())
		{
			return ProcessPreconditionsResolution.rejectWithInternalReason("only picker shall be allowed to process");
		}

		if (!getView().isApproved())
		{
			return ProcessPreconditionsResolution.rejectWithInternalReason("not all rows were approved");
		}

		final List<ProductsToPickRow> rows = getRowsNotAlreadyProcessed();
		if (rows.isEmpty())
		{
			return ProcessPreconditionsResolution.rejectWithInternalReason("no unprocessed rows");
		}
		if (!rows.stream().allMatch(ProductsToPickRow::isEligibleForProcessing))
		{
			return ProcessPreconditionsResolution.rejectWithInternalReason("not all rows eligible for processing");
		}

		return ProcessPreconditionsResolution.accept();
	}

	@Override
	public Object getParameterDefaultValue(final IProcessDefaultParameter parameter)
	{
		final String parameterName = parameter.getColumnName();
		if (I_M_Shipper.COLUMNNAME_M_Shipper_ID.equals(parameterName))
		{
			final List<ProductsToPickRow> rows = getRowsNotAlreadyProcessed();
			return rows.stream()
					.map(ProductsToPickRow::getShipperId)
					.filter(Objects::nonNull)
					.findFirst()
					.map(ShipperId::getRepoId)
					.orElse(-1);
		}

		if (I_M_ShipperTransportation.COLUMNNAME_M_ShipperTransportation_ID.equals(parameterName))
		{
			return getNextTransportationOrderId();
		}

		return IProcessDefaultParametersProvider.DEFAULT_VALUE_NOTAVAILABLE;
	}

	@Override
	public void onParameterChanged(final String parameterName)
	{
		if (I_M_Shipper.COLUMNNAME_M_Shipper_ID.equals(parameterName))
		{
			shipperTransportationId = getNextTransportationOrderId();
		}
	}

	private int getNextTransportationOrderId()
	{
		final ShipperId shipperId = ShipperId.ofRepoIdOrNull(shipperRecordId);
		final ShipperTransportationId nextShipperTransportationForShipper = shipperTransportationRepo.retrieveNextOpenShipperTransportationIdOrNull(shipperId);

		return nextShipperTransportationForShipper == null ? -1 : nextShipperTransportationForShipper.getRepoId();
	}

	@Override
	protected String doIt()
	{
		if (!getView().isApproved())
		{
			throw new AdempiereException("Not all rows were approved");
		}

		final List<PickingCandidate> pickingCandidates = processAllPickingCandidates();
		deliverAndInvoice(pickingCandidates);

		return MSG_OK;
	}

	private ImmutableList<PickingCandidate> processAllPickingCandidates()
	{
		final ImmutableSet<PickingCandidateId> pickingCandidateIdsToProcess = getRowsNotAlreadyProcessed()
				.stream()
				.filter(ProductsToPickRow::isEligibleForProcessing)
				.map(ProductsToPickRow::getPickingCandidateId)
				.filter(Objects::nonNull)
				.collect(ImmutableSet.toImmutableSet());

		return pickingCandidatesService
				.process(pickingCandidateIdsToProcess)
				.getPickingCandidates();
	}

	private void deliverAndInvoice(final List<PickingCandidate> pickingCandidates)
	{
		final Set<HuId> huIdsToDeliver = pickingCandidates
				.stream()
				.filter(PickingCandidate::isPacked)
				.map(PickingCandidate::getPackedToHuId)
				.collect(ImmutableSet.toImmutableSet());

		final List<I_M_HU> husToDeliver = handlingUnitsRepo.getByIds(huIdsToDeliver);

		HUShippingFacade.builder()
				.hus(husToDeliver)
				.addToShipperTransportationId(shipperTransportationId)
				.completeShipments(true)
				.failIfNoShipmentCandidatesFound(true)
				.invoiceMode(BillAssociatedInvoiceCandidates.IF_INVOICE_SCHEDULE_PERMITS)
				.createShipperDeliveryOrders(true)
				.build()
				.generateShippingDocuments();
	}

	private List<ProductsToPickRow> getRowsNotAlreadyProcessed()
	{
		return streamAllRows()
				.filter(row -> !row.isProcessed())
				.collect(ImmutableList.toImmutableList());
	}
}
