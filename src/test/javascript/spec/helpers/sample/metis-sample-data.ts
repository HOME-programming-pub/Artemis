import { Course } from 'app/entities/course.model';
import { User } from 'app/core/user/user.model';
import { CourseWideContext, VOTE_EMOJI_ID } from 'app/shared/metis/metis.util';
import { Reaction } from 'app/entities/metis/reaction.model';
import { Exercise, ExerciseType } from 'app/entities/exercise.model';
import { Lecture } from 'app/entities/lecture.model';
import { Post } from 'app/entities/metis/post.model';
import { AnswerPost } from 'app/entities/metis/answer-post.model';
import { ConversationParticipant } from 'app/entities/metis/conversation/conversation-details.model';
import { Conversation } from 'app/entities/metis/conversation/conversation.model';

export const metisLecture = { id: 1, title: 'Metis  Lecture' } as Lecture;
export const metisLecture2 = { id: 1, title: 'Second Metis  Lecture' } as Lecture;
export const metisExercise = { id: 1, title: 'Metis  Exercise', type: ExerciseType.TEXT } as Exercise;
export const metisExercise2 = { id: 1, title: 'Second Metis  Exercise', type: ExerciseType.TEXT } as Exercise;

export const metisUser1 = { id: 1, name: 'username1', login: 'login1', groups: ['metisStudents'] } as User;
export const metisUser2 = { id: 2, name: 'username2', login: 'login2', groups: ['metisStudents'] } as User;
export const metisTutor = { id: 3, name: 'username3', login: 'login3', groups: ['metisTutors'] } as User;
export const metisTags = ['Tag1', 'Tag2'];

export const metisUpVoteReactionUser1 = { id: 1, user: metisUser1, emojiId: VOTE_EMOJI_ID } as Reaction;
export const metisReactionUser2 = { id: 2, user: metisUser2, emojiId: 'smile', creationDate: undefined } as Reaction;
export const metisReactionToCreate = { emojiId: 'cheerio', creationDate: undefined } as Reaction;

export const metisCourse = {
    id: 1,
    title: 'Metis Course',
    exercises: [metisExercise, metisExercise2],
    lectures: [metisLecture, metisLecture2],
    postsEnabled: true,
    groups: ['metisTutors', 'metisStudents', 'metisInstructors'],
} as Course;

export const metisResolvingAnswerPostUser1 = {
    id: 1,
    author: metisUser1,
    content: 'metisAnswerPostUser3',
    creationDate: undefined,
    resolvesPost: true,
} as AnswerPost;

export const metisAnswerPostUser2 = {
    id: 2,
    author: metisUser2,
    content: 'metisAnswerPostUser3',
    creationDate: undefined,
} as AnswerPost;

export const metisApprovedAnswerPostTutor = {
    id: 3,
    author: metisTutor,
    content: 'metisApprovedAnswerPostTutor',
    resolvesPost: true,
    creationDate: undefined,
} as AnswerPost;

export const metisAnswerPostToCreateUser1 = {
    author: metisUser1,
    content: 'metisAnswerPostToCreateUser1',
    creationDate: undefined,
} as AnswerPost;

export const metisPostTechSupport = {
    id: 1,
    author: metisUser1,
    courseWideContext: CourseWideContext.TECH_SUPPORT,
    course: metisCourse,
    title: 'title',
    content: 'metisPostTechSupport',
    creationDate: undefined,
} as Post;

export const metisPostRandom = {
    id: 2,
    author: metisUser1,
    courseWideContext: CourseWideContext.RANDOM,
    course: metisCourse,
    title: 'title',
    content: 'metisPostRandom',
    creationDate: undefined,
} as Post;

export const metisPostOrganization = {
    id: 3,
    author: metisUser1,
    courseWideContext: CourseWideContext.ORGANIZATION,
    course: metisCourse,
    title: 'title',
    content: 'metisPostOrganization',
    creationDate: undefined,
} as Post;

export const metisAnnouncement = {
    id: 4,
    author: metisUser1,
    courseWideContext: CourseWideContext.ORGANIZATION,
    course: metisCourse,
    title: 'title',
    content: 'metisPostOrganization',
    creationDate: undefined,
} as Post;

export const metisCoursePostsWithCourseWideContext = [metisPostTechSupport, metisPostRandom, metisPostOrganization];

export const metisPostExerciseUser1 = {
    id: 5,
    author: metisUser1,
    exercise: metisExercise,
    title: 'title',
    content: 'metisPostExerciseUser1',
    creationDate: undefined,
} as Post;

export const metisPostExerciseUser2 = {
    id: 6,
    author: metisUser2,
    exercise: metisExercise,
    title: 'title',
    content: 'metisPostExerciseUser2',
    creationDate: undefined,
} as Post;

export const metisExercisePosts = [metisPostExerciseUser1, metisPostExerciseUser2];

export const metisPostLectureUser1 = {
    id: 7,
    author: metisUser1,
    lecture: metisLecture,
    title: 'title',
    content: 'metisPostLectureUser1',
    creationDate: undefined,
} as Post;

export const metisPostLectureUser2 = {
    id: 8,
    author: metisUser2,
    lecture: metisLecture,
    title: 'title',
    content: 'metisPostLectureUser2',
    creationDate: undefined,
    answers: [metisResolvingAnswerPostUser1],
} as Post;

metisResolvingAnswerPostUser1.post = metisPostLectureUser2;

const conversationParticipantUser1 = { id: 1, user: metisUser1 } as ConversationParticipant;

const conversationParticipantUser2 = { id: 2, user: metisUser2 } as ConversationParticipant;

const conversationParticipantTutor = { id: 3, user: metisTutor } as ConversationParticipant;

export const conversationBetweenUser1User2 = {
    id: 1,
    conversationParticipants: [conversationParticipantUser1, conversationParticipantUser2],
    creationDate: undefined,
    lastMessageDate: undefined,
} as Conversation;

export const conversationBetweenUser2AndTutor = {
    id: 2,
    conversationParticipants: [conversationParticipantUser2, conversationParticipantTutor],
    creationDate: undefined,
    lastMessageDate: undefined,
} as Conversation;

export const directMessageUser1 = {
    id: 9,
    author: metisUser1,
    content: 'user1directMessageToUser2',
    creationDate: undefined,
    conversation: conversationBetweenUser1User2,
} as Post;

export const directMessageUser2 = {
    id: 10,
    author: metisUser1,
    content: 'user2directMessageToUser1',
    creationDate: undefined,
    conversation: conversationBetweenUser1User2,
} as Post;

export const conversationsOfUser1 = [conversationBetweenUser1User2];

export const conversationsOfUser2 = [conversationBetweenUser1User2, conversationBetweenUser2AndTutor];

export const metisLecturePosts = [metisPostLectureUser1, metisPostLectureUser2];

export const metisCoursePosts = metisCoursePostsWithCourseWideContext.concat(metisExercisePosts, metisLecturePosts);

export const messagesBetweenUser1User2 = [directMessageUser1, directMessageUser2];

export const metisPostToCreateUser1 = {
    author: metisUser1,
    content: 'metisAnswerToCreateUser1',
    creationDate: undefined,
} as Post;

export const conversationParticipantToCreateUser2 = {
    user: metisUser2,
    lastRead: undefined,
    closed: false,
} as ConversationParticipant;

export const conversationToCreateUser1 = {
    course: metisCourse,
    conversationParticipants: [conversationParticipantToCreateUser2],
    creationDate: undefined,
    lastMessageDate: undefined,
} as Conversation;

export const metisConversationsOfUser1 = [conversationToCreateUser1];
