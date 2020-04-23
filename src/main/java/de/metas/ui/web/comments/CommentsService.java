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

package de.metas.ui.web.comments;

import com.sun.istack.internal.NotNull;
import de.metas.comments.CommentsRepository;
import de.metas.comments.RecordComment;
import de.metas.ui.web.comments.json.JSONComment;
import de.metas.ui.web.comments.json.JSONCommentCreateRequest;
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
public class CommentsService
{
	private final CommentsRepository commentsRepository;
	final IUserDAO userDAO = Services.get(IUserDAO.class);

	public CommentsService(final CommentsRepository commentsRepository)
	{
		this.commentsRepository = commentsRepository;
	}

	@NonNull
	public List<JSONComment> getCommentsFor(@NonNull final TableRecordReference tableRecordReference)
	{

		final List<RecordComment> comments = commentsRepository.retrieveLastComments(tableRecordReference, 100);

		return comments.stream()
				.map(comment -> toJsonComment(comment, userDAO))
				.sorted(Comparator.comparing(JSONComment::getCreated))
				.collect(GuavaCollectors.toImmutableList());
	}

	@NonNull
	private static JSONComment toJsonComment(@NotNull final RecordComment comment, @NotNull final IUserDAO userDAO)
	{
		final String text = comment.getText();
		final ZonedDateTime created = TimeUtil.asZonedDateTime(comment.getCreated(), SystemTime.zoneId());
		final String createdBy = userDAO.retrieveUserFullname(comment.getCreatedBy());

		return JSONComment.builder()
				.text(text)
				.created(created)
				.createdBy(createdBy)
				.build();
	}

	public void addComment(@NonNull final TableRecordReference tableRecordReference, @NotNull final JSONCommentCreateRequest jsonCommentCreateRequest)
	{
		commentsRepository.createComment(jsonCommentCreateRequest.getText(), tableRecordReference);
	}
}
