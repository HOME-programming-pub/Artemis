import { Course } from '../../../../main/webapp/app/entities/course.model';
import multipleChoiceQuizTemplate from '../../fixtures/exercise/quiz/multiple_choice/template.json';
import { courseManagementRequest, courseOverview } from '../../support/artemis';
import { convertCourseAfterMultiPart } from '../../support/requests/CourseManagementRequests';
import { admin } from '../../support/users';
import { generateUUID } from '../../support/utils';

describe('Course Exercise', () => {
    let course: Course;
    let courseName: string;
    let courseShortName: string;

    before('Create course', () => {
        cy.login(admin);
        const uid = generateUUID();
        courseName = 'Cypress course' + uid;
        courseShortName = 'cypress' + uid;
        courseManagementRequest.createCourse(false, courseName, courseShortName).then((response) => {
            course = convertCourseAfterMultiPart(response);
        });
    });

    describe('Search Exercise', () => {
        let exercise1: any;
        let exercise2: any;
        let exercise3: any;

        before('Create Exercises', () => {
            courseManagementRequest.createQuizExercise({ course }, [multipleChoiceQuizTemplate], 'Course Exercise Quiz 1').then((response) => {
                exercise1 = response.body;
            });
            courseManagementRequest.createQuizExercise({ course }, [multipleChoiceQuizTemplate], 'Course Exercise Quiz 2').then((response) => {
                exercise2 = response.body;
            });
            courseManagementRequest.createQuizExercise({ course }, [multipleChoiceQuizTemplate], 'Course Exercise 3').then((response) => {
                exercise3 = response.body;
            });
        });

        it('should filter exercises based on title', () => {
            cy.visit(`/courses/${course.id}/exercises`);
            courseOverview.getExercise(exercise1.id).should('be.visible');
            courseOverview.getExercise(exercise2.id).should('be.visible');
            courseOverview.getExercise(exercise3.id).should('be.visible');
            courseOverview.search('Course Exercise Quiz');
            courseOverview.getExercise(exercise1.id).should('be.visible');
            courseOverview.getExercise(exercise2.id).should('be.visible');
            courseOverview.getExercise(exercise3.id).should('not.exist');
        });

        after('Delete Exercises', () => {
            courseManagementRequest.deleteQuizExercise(exercise1.id);
            courseManagementRequest.deleteQuizExercise(exercise2.id);
            courseManagementRequest.deleteQuizExercise(exercise3.id);
        });
    });

    after('Delete course', () => {
        if (course) {
            cy.login(admin);
            courseManagementRequest.deleteCourse(course.id!);
        }
    });
});
