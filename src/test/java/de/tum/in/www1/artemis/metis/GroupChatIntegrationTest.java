package de.tum.in.www1.artemis.metis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;

import de.tum.in.www1.artemis.util.ModelFactory;
import de.tum.in.www1.artemis.web.rest.metis.conversation.dtos.GroupChatDTO;
import de.tum.in.www1.artemis.web.websocket.dto.metis.MetisCrudAction;

class GroupChatIntegrationTest extends AbstractConversationTest {

    private static final String TEST_PREFIX = "grtest";

    @BeforeEach
    void setupTestScenario() throws Exception {
        super.setupTestScenario();
        this.database.addUsers(TEST_PREFIX, 11, 0, 0, 0);
        if (userRepository.findOneByLogin(testPrefix + "student42").isEmpty()) {
            userRepository.save(ModelFactory.generateActivatedUser(testPrefix + "student42"));
        }
    }

    @Override
    String getTestPrefix() {
        return TEST_PREFIX;
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "student1", roles = "USER")
    void startGroupChat_asStudent1WithStudent2AndStudent3_shouldCreateGroupChat() throws Exception {
        // when
        var chat = request.postWithResponseBody("/api/courses/" + exampleCourseId + "/group-chats/", List.of(testPrefix + "student2", testPrefix + "student3"), GroupChatDTO.class,
                HttpStatus.CREATED);
        // then
        assertThat(chat).isNotNull();
        assertParticipants(chat.getId(), 3, "student1", "student2", "student3");
        // all conversation participants should be notified that the conversation has been "created"
        verifyMultipleParticipantTopicWebsocketSent(MetisCrudAction.CREATE, chat.getId(), "student2", "student3");
        verifyNoParticipantTopicWebsocketSentExceptAction(MetisCrudAction.CREATE);

        // cleanup
        var conversation = groupChatRepository.findById(chat.getId()).get();
        conversationRepository.delete(conversation);
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "student1", roles = "USER")
    void startGroupChat_invalidNumberOfChatPartners_shouldReturnBadRequest() throws Exception {
        // chat with too many users
        // then
        var loginList = new ArrayList<String>();
        for (int i = 2; i <= 11; i++) {
            loginList.add(testPrefix + "student" + i);
        }
        request.postWithResponseBody("/api/courses/" + exampleCourseId + "/group-chats/", loginList, GroupChatDTO.class, HttpStatus.BAD_REQUEST);
        // chat with too few users
        // then
        request.postWithResponseBody("/api/courses/" + exampleCourseId + "/group-chats/", List.of(), GroupChatDTO.class, HttpStatus.BAD_REQUEST);
        verifyNoParticipantTopicWebsocketSent();
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "student42", roles = "USER")
    void startGroupChat_notAllowedAsNotStudentInCourse_shouldReturnBadRequest() throws Exception {
        // then
        request.postWithResponseBody("/api/courses/" + exampleCourseId + "/group-chats/", List.of(testPrefix + "student2", testPrefix + "student3"), GroupChatDTO.class,
                HttpStatus.FORBIDDEN);
        verifyNoParticipantTopicWebsocketSent();
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "student1", roles = "USER")
    void postInGroupChat_firstPost_noWebsocketDTOSent() throws Exception {
        // given
        GroupChatDTO chat = createGroupChatWithStudent1To3();
        // when
        var post = this.postInConversation(chat.getId(), "student1");
        // then
        // send conversation with updated last message date to participants. This is necessary to show the unread messages badge in the client
        verifyMultipleParticipantTopicWebsocketSent(MetisCrudAction.NEW_MESSAGE, chat.getId(), "student2", "student3");
        verifyNoParticipantTopicWebsocketSentExceptAction(MetisCrudAction.NEW_MESSAGE);

        // cleanup
        var conversation = groupChatRepository.findById(chat.getId()).get();
        conversationMessageRepository.deleteById(post.getId());
        conversationRepository.delete(conversation);
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "student1", roles = "USER")
    void updateGroupChat_updateName_shouldUpdateName() throws Exception {
        // given
        GroupChatDTO chat = createGroupChatWithStudent1To3();
        // when
        chat.setName("updated");
        request.putWithResponseBody("/api/courses/" + exampleCourseId + "/group-chats/" + chat.getId(), chat, GroupChatDTO.class, HttpStatus.OK);
        // then
        var updatedGroupChat = groupChatRepository.findById(chat.getId()).get();
        assertParticipants(updatedGroupChat.getId(), 3, "student1", "student2", "student3");
        assertThat(updatedGroupChat.getName()).isEqualTo("updated");
        verifyMultipleParticipantTopicWebsocketSent(MetisCrudAction.UPDATE, chat.getId(), "student1", "student2", "student3");
        verifyNoParticipantTopicWebsocketSentExceptAction(MetisCrudAction.UPDATE);

        // cleanup
        var conversation = groupChatRepository.findById(chat.getId()).get();
        conversationRepository.delete(conversation);
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "student1", roles = "USER")
    void updateGroupChat_notAMemberOfGroupChat_shouldReturnForbidden() throws Exception {
        // given
        GroupChatDTO chat = createGroupChatWithStudent1To3();
        // when
        chat.setName("updated");
        database.changeUser(testPrefix + "student42");
        request.putWithResponseBody("/api/courses/" + exampleCourseId + "/group-chats/" + chat.getId(), chat, GroupChatDTO.class, HttpStatus.FORBIDDEN);
        // then
        var groupChat = groupChatRepository.findById(chat.getId()).get();
        assertParticipants(groupChat.getId(), 3, "student1", "student2", "student3");
        assertThat(groupChat.getName()).isNotEqualTo("updated");
        verifyNoParticipantTopicWebsocketSent();

        // cleanup
        var conversation = groupChatRepository.findById(chat.getId()).get();
        conversationRepository.delete(conversation);
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "student1", roles = "USER")
    void registerUsersToGroupChat_memberOfGroupChat_shouldAddUsers() throws Exception {
        // given
        GroupChatDTO chat = createGroupChatWithStudent1To3();
        // when
        // Note: adding student1 and student2 again should not be a problem (silently ignored)
        request.postWithoutResponseBody("/api/courses/" + exampleCourseId + "/group-chats/" + chat.getId() + "/register",
                List.of(testPrefix + "student1", testPrefix + "student2", testPrefix + "student4", testPrefix + "student5"), HttpStatus.OK);
        // then
        assertParticipants(chat.getId(), 5, "student1", "student2", "student3", "student4", "student5");
        verifyMultipleParticipantTopicWebsocketSent(MetisCrudAction.CREATE, chat.getId(), "student4", "student5");
        verifyMultipleParticipantTopicWebsocketSent(MetisCrudAction.UPDATE, chat.getId(), "student1", "student2", "student3");
        verifyNoParticipantTopicWebsocketSentExceptAction(MetisCrudAction.CREATE, MetisCrudAction.UPDATE);

        // cleanup
        var conversation = groupChatRepository.findById(chat.getId()).get();
        conversationRepository.delete(conversation);
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "student1", roles = "USER")
    void registerUsersToGroupChat_asNonMember_shouldReturnForbidden() throws Exception {
        // given
        GroupChatDTO chat = createGroupChatWithStudent1To3();
        // when
        database.changeUser(testPrefix + "student42");
        request.postWithoutResponseBody("/api/courses/" + exampleCourseId + "/group-chats/" + chat.getId() + "/register",
                List.of(testPrefix + "student1", testPrefix + "student2", testPrefix + "student4", testPrefix + "student5"), HttpStatus.FORBIDDEN);

        // then
        assertParticipants(chat.getId(), 3, "student1", "student2", "student3");
        verifyNoParticipantTopicWebsocketSent();
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "student1", roles = "USER")
    void registerUsersToGroupChat_wouldBreakGroupChatLimit_shouldReturnBadRequest() throws Exception {
        // given
        GroupChatDTO chat = createGroupChatWithStudent1To3();
        // when
        var loginList = new ArrayList<String>();
        for (int i = 4; i <= 11; i++) {
            loginList.add(testPrefix + "student" + i);
        }
        request.postWithoutResponseBody("/api/courses/" + exampleCourseId + "/group-chats/" + chat.getId() + "/register", loginList, HttpStatus.BAD_REQUEST);
        assertParticipants(chat.getId(), 3, "student1", "student2", "student3");
        verifyNoParticipantTopicWebsocketSent();

        // cleanup
        var conversation = groupChatRepository.findById(chat.getId()).get();
        conversationRepository.delete(conversation);
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "student1", roles = "USER")
    void deregisterUsersFromGroupChat_asMember_shouldRemoveMembers() throws Exception {
        // given
        GroupChatDTO chat = createGroupChatWithStudent1To3();
        // when
        request.postWithoutResponseBody("/api/courses/" + exampleCourseId + "/group-chats/" + chat.getId() + "/deregister", List.of(testPrefix + "student2"), HttpStatus.OK);
        // then
        assertParticipants(chat.getId(), 2, "student1", "student3");
        verifyMultipleParticipantTopicWebsocketSent(MetisCrudAction.DELETE, chat.getId(), "student2");
        verifyMultipleParticipantTopicWebsocketSent(MetisCrudAction.UPDATE, chat.getId(), "student1", "student3");
        verifyNoParticipantTopicWebsocketSentExceptAction(MetisCrudAction.DELETE, MetisCrudAction.UPDATE);

        // cleanup
        var conversation = groupChatRepository.findById(chat.getId()).get();
        conversationRepository.delete(conversation);
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "student1", roles = "USER")
    void deregisterUsersFromGroupChat_selfDeregistration_shouldRemoveRequestingMember() throws Exception {
        // given
        GroupChatDTO chat = createGroupChatWithStudent1To3();
        // when
        request.postWithoutResponseBody("/api/courses/" + exampleCourseId + "/group-chats/" + chat.getId() + "/deregister", List.of(testPrefix + "student1"), HttpStatus.OK);
        // then
        assertParticipants(chat.getId(), 2, "student2", "student3");
        verifyMultipleParticipantTopicWebsocketSent(MetisCrudAction.DELETE, chat.getId(), "student1");
        verifyMultipleParticipantTopicWebsocketSent(MetisCrudAction.UPDATE, chat.getId(), "student2", "student3");
        verifyNoParticipantTopicWebsocketSentExceptAction(MetisCrudAction.DELETE, MetisCrudAction.UPDATE);

        // cleanup
        var conversation = groupChatRepository.findById(chat.getId()).get();
        conversationRepository.delete(conversation);

    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "student1", roles = "USER")
    void deregisterUsersFromGroupChat_asNonMember_shouldReturnForbidden() throws Exception {
        // given
        GroupChatDTO chat = createGroupChatWithStudent1To3();
        // when
        database.changeUser(testPrefix + "student42");
        request.postWithoutResponseBody("/api/courses/" + exampleCourseId + "/group-chats/" + chat.getId() + "/deregister", List.of(testPrefix + "student2"), HttpStatus.FORBIDDEN);
        // then
        assertParticipants(chat.getId(), 3, "student1", "student2", "student3");
        verifyNoParticipantTopicWebsocketSent();

        // cleanup
        var conversation = groupChatRepository.findById(chat.getId()).get();
        conversationRepository.delete(conversation);
    }

    private GroupChatDTO createGroupChatWithStudent1To3() throws Exception {
        return this.createGroupChat("student2", "student3");
    }
}