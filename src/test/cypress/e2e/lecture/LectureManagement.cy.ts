import { Lecture } from 'app/entities/lecture.model';
import { Course } from 'app/entities/course.model';
import { generateUUID } from '../../support/utils';
import dayjs from 'dayjs/esm';
import { convertCourseAfterMultiPart } from '../../support/requests/CourseManagementRequests';
import { courseManagementRequest, lectureCreation, lectureManagement } from '../../support/artemis';
import { admin, instructor } from '../../support/users';

describe('Lecture management', () => {
    let course: Course;

    before('Create course', () => {
        cy.login(admin);
        courseManagementRequest.createCourse().then((response) => {
            course = convertCourseAfterMultiPart(response);
            courseManagementRequest.addInstructorToCourse(course, instructor);
        });
    });

    it('creates a lecture', () => {
        const lectureTitle = 'lecture' + generateUUID();
        cy.login(instructor, '/course-management/' + course.id);
        cy.get('#lectures').click();
        lectureManagement.clickCreateLecture();
        lectureCreation.setTitle(lectureTitle);
        cy.fixture('loremIpsum.txt').then((text) => {
            lectureCreation.typeDescription(text);
        });
        lectureCreation.setStartDate(dayjs());
        lectureCreation.setEndDate(dayjs().add(1, 'hour'));
        lectureCreation.save().then((lectureResponse) => {
            expect(lectureResponse.response!.statusCode).to.eq(201);
        });
    });

    describe('Handle existing lecture', () => {
        let lecture: Lecture;

        beforeEach('Create a lecture', () => {
            cy.login(instructor, '/course-management/' + course.id + '/lectures');
            courseManagementRequest.createLecture(course).then((lectureResponse) => {
                lecture = lectureResponse.body;
            });
        });

        it('Deletes an existing lecture', () => {
            lectureManagement.deleteLecture(lecture).then((resp) => {
                expect(resp.response!.statusCode).to.eq(200);
                lectureManagement.getLecture(lecture.id!).should('not.exist');
            });
        });

        it('Adds a text unit to the lecture', () => {
            lectureManagement.openUnitsPage(lecture.id!);
            cy.fixture('loremIpsum.txt').then((text) => {
                lectureManagement.addTextUnit('Text unit', text);
            });
            cy.contains('Text unit').should('be.visible');
        });

        it('Adds a exercise unit to the lecture', () => {
            courseManagementRequest.createModelingExercise({ course }).then((model) => {
                const exercise = model.body;
                lectureManagement.openUnitsPage(lecture.id!);
                lectureManagement.addExerciseUnit(exercise.id!);
                cy.contains(exercise.title!);
            });
        });
    });

    after('Delete course', () => {
        if (course) {
            cy.login(admin);
            courseManagementRequest.deleteCourse(course.id!);
        }
    });
});
