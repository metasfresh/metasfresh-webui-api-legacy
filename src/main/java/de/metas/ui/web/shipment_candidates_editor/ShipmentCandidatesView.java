package de.metas.ui.web.shipment_candidates_editor;

import javax.annotation.Nullable;

import de.metas.i18n.ITranslatableString;
import de.metas.ui.web.document.filter.provider.NullDocumentFilterDescriptorsProvider;
import de.metas.ui.web.view.AbstractCustomView;
import de.metas.ui.web.view.IEditableView;
import de.metas.ui.web.view.ViewId;
import de.metas.ui.web.window.datatypes.DocumentId;
import de.metas.ui.web.window.datatypes.LookupValuesList;
import lombok.Builder;
import lombok.NonNull;

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

public final class ShipmentCandidatesView extends AbstractCustomView<ShipmentCandidateRow> implements IEditableView
{
	@Builder
	private ShipmentCandidatesView(
			@NonNull final ViewId viewId,
			@Nullable final ITranslatableString description,
			@NonNull final ShipmentCandidateRows rows)
	{
		super(viewId, description, rows, NullDocumentFilterDescriptorsProvider.instance);
	}

	@Override
	public String getTableNameOrNull(final DocumentId documentId)
	{
		return null;
	}

	@Override
	public LookupValuesList getFieldTypeahead(final RowEditingContext ctx, final String fieldName, final String query)
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public LookupValuesList getFieldDropdown(final RowEditingContext ctx, final String fieldName)
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
}