import { Exercise } from 'app/entities/exercise.model';
import { BASE_API, DELETE } from '../../constants';

/**
 * A class which encapsulates UI selectors and actions for the course management exercises page.
 */
export class CourseManagementExercisesPage {
    getExercise(exerciseID: number) {
        return cy.get(`#exercise-card-${exerciseID}`);
    }

    clickDeleteExercise(exerciseID: number) {
        this.getExercise(exerciseID).find('#delete-exercise').click();
    }

    clickExampleSubmissionsButton() {
        cy.get('#example-submissions-button').click();
    }

    getExerciseTitle() {
        return cy.get('#exercise-detail-title');
    }

    deleteTextExercise(exercise: Exercise) {
        this.getExercise(exercise.id!).find('#delete-exercise').click();
        cy.get('#confirm-exercise-name').type(exercise.title!);
        cy.intercept(DELETE, BASE_API + 'text-exercises/*').as('deleteTextExercise');
        cy.get('#delete').click();
        cy.wait('@deleteTextExercise');
    }

    deleteModelingExercise(exercise: Exercise) {
        this.getExercise(exercise.id!).find('#delete-exercise').click();
        cy.get('#confirm-exercise-name').type(exercise.title!);
        cy.intercept(DELETE, BASE_API + 'modeling-exercises/*').as('deleteModelingExercise');
        cy.get('#delete').click();
        cy.wait('@deleteModelingExercise');
    }

    deleteQuizExercise(exercise: Exercise) {
        this.getExercise(exercise.id!).find(`#delete-quiz-${exercise.id}`).click();
        cy.get('#confirm-exercise-name').type(exercise.title!);
        cy.intercept(DELETE, BASE_API + 'quiz-exercises/*').as('deleteQuizExercise');
        cy.get('#delete').click();
        cy.wait('@deleteQuizExercise');
    }

    deleteProgrammingExercise(exercise: Exercise) {
        this.getExercise(exercise.id!).find('#delete-exercise').click();
        cy.get('#additional-check-0').check();
        cy.get('#additional-check-1').check();
        cy.get('#confirm-exercise-name').type(exercise.title!);
        cy.intercept(DELETE, BASE_API + 'programming-exercises/*').as('deleteProgrammingExercise');
        cy.get('#delete').click();
        cy.wait('@deleteProgrammingExercise');
    }

    clickCreateProgrammingExerciseButton() {
        cy.get('#jh-create-entity').click();
    }

    shouldContainExerciseWithName(exerciseID: number) {
        this.getExercise(exerciseID).scrollIntoView().should('be.visible');
    }

    createModelingExercise() {
        cy.get('#modeling-exercise-create-button').click();
    }

    createTextExercise() {
        cy.get('#create-text-exercise').click();
    }

    createQuizExercise() {
        cy.get('#create-quiz-button').click();
    }

    getModelingExerciseTitle(exerciseID: number) {
        return cy.get(`#exercise-card-${exerciseID}`).find(`#modeling-exercise-${exerciseID}-title`);
    }

    getModelingExerciseMaxPoints(exerciseID: number) {
        return cy.get(`#exercise-card-${exerciseID}`).find(`#modeling-exercise-${exerciseID}-maxPoints`);
    }
}
