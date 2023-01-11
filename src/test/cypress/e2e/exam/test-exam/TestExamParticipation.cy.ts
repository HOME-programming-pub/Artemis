import { Exam } from 'app/entities/exam.model';
import { CypressExamBuilder, convertCourseAfterMultiPart } from '../../../support/requests/CourseManagementRequests';
import { artemis } from '../../../support/ArtemisTesting';
import dayjs from 'dayjs/esm';
import allSuccessful from '../../../fixtures/programming_exercise_submissions/all_successful/submission.json';
import buildError from '../../../fixtures/programming_exercise_submissions/build_error/submission.json';
import { Course } from 'app/entities/course.model';
import { generateUUID } from '../../../support/utils';
import { EXERCISE_TYPE } from '../../../support/constants';
import { AdditionalData, Exercise } from 'src/test/cypress/support/pageobjects/exam/ExamParticipation';

// Users
const users = artemis.users;
const admin = users.getAdmin();
const studentOne = users.getStudentOne();
const studentTwo = users.getStudentTwo();

// Requests
const courseManagementRequests = artemis.requests.courseManagement;

// PageObjects
const examParticipation = artemis.pageobjects.exam.participation;
const exerciseGroupCreation = artemis.pageobjects.exam.exerciseGroupCreation;

// Common primitives
const examTitle = 'test-exam' + generateUUID();
const textFixture = 'loremIpsum.txt';

const exerciseArray: Array<Exercise> = [];

describe('Test exam participation', () => {
    let course: Course;
    let exam: Exam;

    before(() => {
        cy.login(admin);
        courseManagementRequests.createCourse(true).then((response) => {
            course = convertCourseAfterMultiPart(response);
            const examContent = new CypressExamBuilder(course)
                .title(examTitle)
                .testExam()
                .visibleDate(dayjs().subtract(3, 'days'))
                .startDate(dayjs().subtract(2, 'days'))
                .endDate(dayjs().add(3, 'days'))
                .examMaxPoints(100)
                .numberOfExercises(10)
                .correctionRounds(0)
                .build();
            courseManagementRequests.createExam(examContent).then((examResponse) => {
                exam = examResponse.body;
                addGroupWithExercise(exam, EXERCISE_TYPE.Text, { textFixture });
                addGroupWithExercise(exam, EXERCISE_TYPE.Text, { textFixture });
                addGroupWithExercise(exam, EXERCISE_TYPE.Text, { textFixture });

                addGroupWithExercise(exam, EXERCISE_TYPE.Programming, { submission: allSuccessful, expectedScore: 100 });
                addGroupWithExercise(exam, EXERCISE_TYPE.Programming, { submission: buildError, expectedScore: 0 });

                addGroupWithExercise(exam, EXERCISE_TYPE.Quiz, { quizExerciseID: 0 });
                addGroupWithExercise(exam, EXERCISE_TYPE.Quiz, { quizExerciseID: 0 });

                addGroupWithExercise(exam, EXERCISE_TYPE.Modeling);
                addGroupWithExercise(exam, EXERCISE_TYPE.Modeling);
            });
        });
    });

    it('Participates as a student in a registered test exam', () => {
        examParticipation.startParticipation(studentOne, course, exam);
        for (let j = 0; j < exerciseArray.length; j++) {
            const exercise = exerciseArray[j];
            examParticipation.openExercise(j);
            examParticipation.makeSubmission(exercise.id, exercise.type, exercise.additionalData);
        }
        examParticipation.handInEarly();
        for (let j = 0; j < exerciseArray.length; j++) {
            const exercise = exerciseArray[j];
            examParticipation.verifyExerciseTitleOnFinalPage(exercise.id, exercise.title);
            if (exercise.type === EXERCISE_TYPE.Text) {
                examParticipation.verifyTextExerciseOnFinalPage(exercise.additionalData!.textFixture!);
            }
        }
        examParticipation.checkExamTitle(examTitle);
    });

    it('Checks if switching test exam tabs works', () => {
        examParticipation.startParticipation(studentTwo, course, exam);
        for (let j = 0; j < 20; j++) {
            const i = between(0, exerciseArray.length - 1);
            examParticipation.openExercise(i);
            examParticipation.checkExerciseTitle(exerciseArray[i].id, exerciseArray[i].title);
        }

        examParticipation.handInEarly();
    });

    after(() => {
        if (course) {
            cy.login(admin);
            courseManagementRequests.deleteCourse(course.id!);
        }
    });
});

function addGroupWithExercise(exam: Exam, exerciseType: EXERCISE_TYPE, additionalData?: AdditionalData) {
    exerciseGroupCreation.addGroupWithExercise(exam, 'Exercise ' + generateUUID(), exerciseType, (response) => {
        if (exerciseType == EXERCISE_TYPE.Quiz) {
            additionalData!.quizExerciseID = response.body.quizQuestions![0].id;
        }
        addExerciseToArray(exerciseArray, exerciseType, response, additionalData);
    });
}

function addExerciseToArray(exerciseArray: Array<Exercise>, type: EXERCISE_TYPE, response: any, additionalData?: AdditionalData) {
    exerciseArray.push({ title: response.body.title, type, id: response.body.id, additionalData });
}

function between(min: number, max: number) {
    return Math.floor(Math.random() * (max - min + 1) + min);
}
