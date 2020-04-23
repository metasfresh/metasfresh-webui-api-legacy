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

import de.metas.comments.ChatEntryId;
import de.metas.comments.ChatId;
import de.metas.comments.CommentsRepository;
import de.metas.comments.RecordComment;
import de.metas.ui.web.comments.json.JSONComment;
import de.metas.ui.web.comments.json.JSONCommentCreateRequest;
import de.metas.user.UserId;
import de.metas.util.time.FixedTimeSource;
import de.metas.util.time.SystemTime;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.test.AdempiereTestHelper;
import org.adempiere.util.lang.impl.TableRecordReference;
import org.assertj.core.api.Assertions;
import org.compiere.model.I_AD_User;
import org.compiere.model.I_CM_Chat;
import org.compiere.model.I_CM_ChatEntry;
import org.compiere.model.I_C_BPartner;
import org.compiere.model.X_CM_ChatEntry;
import org.compiere.util.Env;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class CommentsServiceTest
{
	public static final int AD_USER_ID = 10;
	public static final String THE_USER_NAME = "The User Name";
	private final CommentsRepository commentsRepository = new CommentsRepository();
	private final CommentsService commentsService = new CommentsService(commentsRepository);

	@BeforeEach
	public void init()
	{
		AdempiereTestHelper.get().init();
		SystemTime.setTimeSource(new FixedTimeSource(2020, Month.APRIL.getValue(), 23, 1, 1, 1));

		// all created POs will have this user
		Env.setLoggedUserId(Env.getCtx(), UserId.ofRepoId(AD_USER_ID));

		createDefaultUser();
	}

	@Nested
	class TestCreateComments
	{
		@Test
		void create2Comments()
		{
			// create test data
			final TableRecordReference tableRecordReference = TableRecordReference.of("DummyTable", 1);

			final JSONCommentCreateRequest request1 = new JSONCommentCreateRequest("comment1");
			final JSONCommentCreateRequest request2 = new JSONCommentCreateRequest("comment2");
			commentsService.addComment(tableRecordReference, request1);
			commentsService.addComment(tableRecordReference, request2);

			// check the comments exist
			final List<RecordComment> actual = commentsRepository.retrieveLastComments(tableRecordReference, 4);

			final List<RecordComment> expected = Arrays.asList(
					RecordComment.of(
							UserId.ofRepoId(AD_USER_ID),
							ZonedDateTime.of(2020, Month.APRIL.getValue(), 23, 1, 1, 1, 0, ZoneId.systemDefault()),
							"comment1",
							ChatEntryId.ofRepoId(1)
					),
					RecordComment.of(
							UserId.ofRepoId(AD_USER_ID),
							ZonedDateTime.of(2020, Month.APRIL.getValue(), 23, 1, 1, 1, 0, ZoneId.systemDefault()),
							"comment2",
							ChatEntryId.ofRepoId(1)
					)
			);

			Assertions.assertThat(actual)
					.usingElementComparatorIgnoringFields("id")
					.isEqualTo(expected);
		}
	}

	@Nested
	class TestGetComments
	{
		@Test
		void commentsExist()
		{
			// create test data
			final TableRecordReference tableRecordReference = TableRecordReference.of("DummyTable", 1);
			final ChatId chatId = createChat(tableRecordReference);
			createChatEntry(chatId, "comment1");
			createChatEntry(chatId, "comment2");

			//
			final List<JSONComment> actual = commentsService.getCommentsFor(tableRecordReference);
			System.out.println(actual);

			final List<JSONComment> expected = Arrays.asList(
					JSONComment.builder()
							.created(ZonedDateTime.of(2020, Month.APRIL.getValue(), 23, 1, 1, 1, 0, ZoneId.systemDefault()))
							.text("comment1")
							.createdBy(THE_USER_NAME)
							.build(),

					JSONComment.builder()
							.created(ZonedDateTime.of(2020, Month.APRIL.getValue(), 23, 1, 1, 1, 0, ZoneId.systemDefault()))
							.text("comment2")
							.createdBy(THE_USER_NAME)
							.build()
			);

			Assertions.assertThat(actual).isEqualTo(expected);
		}

		@Test
		void noCommentsExist()
		{
			// create test data
			final TableRecordReference tableRecordReference = TableRecordReference.of("DummyTable", 1);

			//
			final List<JSONComment> actual = commentsService.getCommentsFor(tableRecordReference);
			System.out.println(actual);

			final List<JSONComment> expected = Collections.emptyList();

			Assertions.assertThat(actual).isEqualTo(expected);
		}
	}

	/**
	 * Not necessary, but helpful to have an actual user name.
	 */
	private void createDefaultUser()
	{
		final I_AD_User user = InterfaceWrapperHelper.newInstance(I_AD_User.class);
		user.setAD_User_ID(AD_USER_ID);
		user.setName(THE_USER_NAME);
		InterfaceWrapperHelper.save(user);
	}

	private ChatId createChat(final TableRecordReference tableRecordReference)
	{
		final I_CM_Chat chat = InterfaceWrapperHelper.newInstance(I_CM_Chat.class);
		chat.setDescription("Table name: " + I_C_BPartner.Table_Name);
		chat.setAD_Table_ID(tableRecordReference.getAD_Table_ID());
		chat.setRecord_ID(tableRecordReference.getRecord_ID());
		InterfaceWrapperHelper.save(chat);
		return ChatId.ofRepoId(chat.getCM_Chat_ID());
	}

	private ChatEntryId createChatEntry(final ChatId chatId, final String characterData)
	{
		final I_CM_ChatEntry chatEntry = InterfaceWrapperHelper.newInstance(I_CM_ChatEntry.class);
		chatEntry.setCM_Chat_ID(chatId.getRepoId());
		chatEntry.setConfidentialType(X_CM_ChatEntry.CONFIDENTIALTYPE_PublicInformation);
		chatEntry.setCharacterData(characterData);
		chatEntry.setChatEntryType(X_CM_ChatEntry.CHATENTRYTYPE_NoteFlat);
		InterfaceWrapperHelper.save(chatEntry);
		return ChatEntryId.ofRepoId(chatEntry.getCM_ChatEntry_ID());
	}
}
