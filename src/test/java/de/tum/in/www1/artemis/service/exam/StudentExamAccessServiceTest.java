package de.tum.in.www1.artemis.service.exam;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import de.tum.in.www1.artemis.AbstractSpringIntegrationBambooBitbucketJiraTest;
import de.tum.in.www1.artemis.domain.Course;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.exam.Exam;
import de.tum.in.www1.artemis.domain.exam.StudentExam;
import de.tum.in.www1.artemis.repository.CourseRepository;
import de.tum.in.www1.artemis.repository.StudentExamRepository;
import de.tum.in.www1.artemis.web.rest.errors.AccessForbiddenException;
import de.tum.in.www1.artemis.web.rest.errors.ConflictException;
import de.tum.in.www1.artemis.web.rest.errors.EntityNotFoundException;

class StudentExamAccessServiceTest extends AbstractSpringIntegrationBambooBitbucketJiraTest {

    private static final String TEST_PREFIX = "seastest"; // only lower case is supported

    @Autowired
    private StudentExamAccessService studentExamAccessService;

    @Autowired
    private StudentExamRepository studentExamRepository;

    @Autowired
    private CourseRepository courseRepository;

    private User student1;

    private Course course1;

    private Course course2;

    private Exam exam1;

    private Exam exam2;

    private StudentExam studentExam1;

    @BeforeEach
    void init() {
        database.addUsers(TEST_PREFIX, 2, 0, 0, 0);
        course1 = database.addEmptyCourse();
        course2 = database.addEmptyCourse();
        course2.setStudentGroupName("another-group");
        courseRepository.save(course2);
        student1 = database.getUserByLogin(TEST_PREFIX + "student1");
        exam1 = database.addActiveExamWithRegisteredUser(course1, student1);
        studentExam1 = database.addStudentExam(exam1);
        studentExam1.setUser(student1);
        studentExamRepository.save(studentExam1);
        exam2 = database.addExam(course2);
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "student1", roles = "USER")
    void testIsAtLeastStudentInCourse() {
        assertThrows(AccessForbiddenException.class, () -> studentExamAccessService.checkCourseAndExamAccessElseThrow(course2.getId(), exam2.getId(), student1, false, true));
        assertThrows(AccessForbiddenException.class, () -> studentExamAccessService.checkStudentExamAccessElseThrow(course2.getId(), exam2.getId(), studentExam1.getId()));
        assertThrows(AccessForbiddenException.class, () -> studentExamAccessService.checkStudentExamAccessElseThrow(course2.getId(), exam2.getId(), studentExam1, student1));
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "student1", roles = "USER")
    void testExamExists() {
        assertThrows(EntityNotFoundException.class, () -> studentExamAccessService.checkCourseAndExamAccessElseThrow(course1.getId(), -1L, student1, false, true));
        assertThrows(EntityNotFoundException.class, () -> studentExamAccessService.checkStudentExamAccessElseThrow(course1.getId(), -1L, studentExam1.getId()));
        assertThrows(EntityNotFoundException.class, () -> studentExamAccessService.checkStudentExamAccessElseThrow(course1.getId(), -1L, studentExam1, student1));
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "student1", roles = "USER")
    void testExamBelongsToCourse() {
        assertThrows(ConflictException.class, () -> studentExamAccessService.checkCourseAndExamAccessElseThrow(course1.getId(), exam2.getId(), student1, false, true));
        assertThrows(ConflictException.class, () -> studentExamAccessService.checkStudentExamAccessElseThrow(course1.getId(), exam2.getId(), studentExam1.getId()));
        assertThrows(ConflictException.class, () -> studentExamAccessService.checkStudentExamAccessElseThrow(course1.getId(), exam2.getId(), studentExam1, student1));
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "student1", roles = "USER")
    void testExamIsLive() {
        // Exam is not visible.
        Exam examNotStarted = database.addExam(course1, student1, ZonedDateTime.now().plusHours(1), ZonedDateTime.now().plusHours(2), ZonedDateTime.now().plusHours(3));
        assertThrows(AccessForbiddenException.class,
                () -> studentExamAccessService.checkCourseAndExamAccessElseThrow(course1.getId(), examNotStarted.getId(), student1, false, true));
        assertThrows(AccessForbiddenException.class, () -> studentExamAccessService.checkStudentExamAccessElseThrow(course1.getId(), examNotStarted.getId(), studentExam1.getId()));
        assertThrows(AccessForbiddenException.class,
                () -> studentExamAccessService.checkStudentExamAccessElseThrow(course1.getId(), examNotStarted.getId(), studentExam1, student1));

        // Exam has ended. After exam has ended, it should still be retrievable by the students to see their participation
        Exam examEnded = database.addExam(course1, student1, ZonedDateTime.now().minusHours(4), ZonedDateTime.now().minusHours(3), ZonedDateTime.now().minusHours(1));
        StudentExam studentExamEnded = database.addStudentExam(examEnded);
        studentExamEnded.setUser(student1);
        studentExamRepository.save(studentExamEnded);
        // does not throw
        studentExamAccessService.checkCourseAndExamAccessElseThrow(course1.getId(), examEnded.getId(), student1, false, true);
        // does not throw
        studentExamAccessService.checkStudentExamAccessElseThrow(course1.getId(), examEnded.getId(), studentExamEnded.getId());
        // does not throw
        studentExamAccessService.checkStudentExamAccessElseThrow(course1.getId(), examEnded.getId(), studentExamEnded, student1);

    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "student1", roles = "USER")
    void testUserIsRegisteredForExam() {
        var student2 = database.getUserByLogin(TEST_PREFIX + "student2");
        Exam examNotRegistered = database.addExam(course1, student2, ZonedDateTime.now().minusHours(4), ZonedDateTime.now().minusHours(1), ZonedDateTime.now().plusHours(1));
        assertThrows(AccessForbiddenException.class,
                () -> studentExamAccessService.checkCourseAndExamAccessElseThrow(course1.getId(), examNotRegistered.getId(), student1, false, true));
        assertThrows(ConflictException.class, () -> studentExamAccessService.checkStudentExamAccessElseThrow(course1.getId(), examNotRegistered.getId(), studentExam1.getId()));
        assertThrows(ConflictException.class, () -> studentExamAccessService.checkStudentExamAccessElseThrow(course1.getId(), examNotRegistered.getId(), studentExam1, student1));
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "student1", roles = "USER")
    void testUserStudentExamExists() {
        assertThrows(EntityNotFoundException.class, () -> studentExamAccessService.checkStudentExamAccessElseThrow(course1.getId(), exam1.getId(), Long.MAX_VALUE));
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "student1", roles = "USER")
    void testExamIdEqualsExamOfStudentExam() {
        StudentExam studentExamNotRelatedToExam1 = database.addStudentExam(exam2);
        assertThrows(ConflictException.class,
                () -> studentExamAccessService.checkStudentExamAccessElseThrow(course1.getId(), exam1.getId(), studentExamNotRelatedToExam1, student1));
        assertThrows(ConflictException.class, () -> studentExamAccessService.checkStudentExamAccessElseThrow(course1.getId(), exam1.getId(), studentExamNotRelatedToExam1.getId()));
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "student1", roles = "USER")
    void testCurrentUserIsUserOfStudentExam() {
        StudentExam studentExamWithOtherUser = database.addStudentExam(exam1);
        var student2 = database.getUserByLogin(TEST_PREFIX + "student2");
        studentExamWithOtherUser.setUser(student2);
        studentExamRepository.save(studentExamWithOtherUser);
        assertThrows(AccessForbiddenException.class,
                () -> studentExamAccessService.checkStudentExamAccessElseThrow(course1.getId(), exam1.getId(), studentExamWithOtherUser, student1));
        assertThrows(AccessForbiddenException.class,
                () -> studentExamAccessService.checkStudentExamAccessElseThrow(course1.getId(), exam1.getId(), studentExamWithOtherUser.getId()));
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "student1", roles = "USER")
    void testCurrentUserHasCourseAccess() {
        assertDoesNotThrow(() -> studentExamAccessService.checkCourseAccessForStudentElseThrow(course1.getId(), student1));
        assertThrows(AccessForbiddenException.class, () -> studentExamAccessService.checkCourseAccessForStudentElseThrow(course2.getId(), student1));
    }

}
