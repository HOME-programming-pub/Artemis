package de.tum.in.www1.artemis.web.rest.admin;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import de.tum.in.www1.artemis.config.Constants;
import de.tum.in.www1.artemis.domain.Course;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.repository.CourseRepository;
import de.tum.in.www1.artemis.repository.UserRepository;
import de.tum.in.www1.artemis.security.annotations.EnforceAdmin;
import de.tum.in.www1.artemis.service.*;
import de.tum.in.www1.artemis.web.rest.errors.BadRequestAlertException;
import de.tum.in.www1.artemis.web.rest.errors.CourseShortnameAlreadyExistsException;
import de.tum.in.www1.artemis.web.rest.util.HeaderUtil;

/**
 * REST controller for managing Course.
 */
@RestController
@RequestMapping("api/admin/")
public class AdminCourseResource {

    private final Logger log = LoggerFactory.getLogger(AdminCourseResource.class);

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final UserRepository userRepository;

    private final CourseService courseService;

    private final CourseRepository courseRepository;

    private final AuditEventRepository auditEventRepository;

    private final FileService fileService;

    private final OnlineCourseConfigurationService onlineCourseConfigurationService;

    public AdminCourseResource(UserRepository userRepository, CourseService courseService, CourseRepository courseRepository, AuditEventRepository auditEventRepository,
            FileService fileService, OnlineCourseConfigurationService onlineCourseConfigurationService) {
        this.courseService = courseService;
        this.courseRepository = courseRepository;
        this.auditEventRepository = auditEventRepository;
        this.userRepository = userRepository;
        this.fileService = fileService;
        this.onlineCourseConfigurationService = onlineCourseConfigurationService;
    }

    /**
     * POST /courses : create a new course.
     *
     * @param course the course to create
     * @param file the optional course icon file
     * @return the ResponseEntity with status 201 (Created) and with body the new course
     * @throws URISyntaxException if the Location URI syntax is incorrect
     * @throws BadRequestAlertException {@code 400 (Bad Request)} if the course already has an ID
     * @throws BadRequestAlertException {@code 400 (Bad Request)} if the course date is invalid
     * @throws CourseShortnameAlreadyExistsException {@code 400 (Bad Request)} if the course shortname already exists
     */
    @PostMapping(value = "courses", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @EnforceAdmin
    public ResponseEntity<Course> createCourse(@RequestPart Course course, @RequestPart(required = false) MultipartFile file) throws URISyntaxException {
        log.debug("REST request to save Course : {}", course);

        Course result = courseService.createCourse(course, file);

        return ResponseEntity.created(new URI("/api/courses/" + result.getId())).body(result);
    }

    /**
     * DELETE /courses/:courseId : delete the "id" course.
     *
     * @param courseId the id of the course to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("courses/{courseId}")
    @EnforceAdmin
    public ResponseEntity<Void> deleteCourse(@PathVariable long courseId) {
        log.info("REST request to delete Course : {}", courseId);
        Course course = courseRepository.findByIdWithExercisesAndLecturesAndLectureUnitsAndLearningGoalsElseThrow(courseId);
        User user = userRepository.getUserWithGroupsAndAuthorities();
        var auditEvent = new AuditEvent(user.getLogin(), Constants.DELETE_COURSE, "course=" + course.getTitle());
        auditEventRepository.add(auditEvent);
        log.info("User {} has requested to delete the course {}", user.getLogin(), course.getTitle());

        courseService.delete(course);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, Course.ENTITY_NAME, course.getTitle())).build();
    }
}
