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

package de.metas.ui.web.notes;

import com.sun.istack.internal.NotNull;
import de.metas.notes.NotesRepository;
import de.metas.notes.UserRecordNote;
import de.metas.ui.web.notes.json.JSONNote;
import de.metas.ui.web.notes.json.JSONNoteCreateRequest;
import de.metas.user.api.IUserDAO;
import de.metas.util.GuavaCollectors;
import de.metas.util.Services;
import de.metas.util.time.SystemTime;
import lombok.NonNull;
import org.adempiere.util.lang.impl.TableRecordReference;
import org.compiere.util.TimeUtil;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class NotesService
{
	private final NotesRepository notesRepository;
	final IUserDAO userDAO = Services.get(IUserDAO.class);

	public NotesService(final NotesRepository notesRepository)
	{
		this.notesRepository = notesRepository;
	}

	@NonNull
	public List<JSONNote> getNotesFor(@NonNull final TableRecordReference tableRecordReference)
	{

		final List<UserRecordNote> notes = notesRepository.retrieveLastNotes(tableRecordReference, 100);

		return notes.stream()
				.map(note -> toJsonNote(note, userDAO))
				.sorted(Comparator.comparing(JSONNote::getCreated))
				.collect(GuavaCollectors.toImmutableList());
	}

	@NonNull
	private static JSONNote toJsonNote(@NotNull final UserRecordNote note, @NotNull final IUserDAO userDAO)
	{
		final String text = note.getText();
		final ZonedDateTime created = TimeUtil.asZonedDateTime(note.getCreated(), SystemTime.zoneId());
		final String createdBy = userDAO.retrieveUserFullname(note.getCreatedBy());

		return JSONNote.builder()
				.text(text)
				.created(created)
				.createdBy(createdBy)
				.build();
	}

	public void addNote(@NonNull final TableRecordReference tableRecordReference, @NotNull final JSONNoteCreateRequest jsonNoteCreateRequest)
	{
		notesRepository.createNote(jsonNoteCreateRequest.getText(), tableRecordReference);
	}
}
