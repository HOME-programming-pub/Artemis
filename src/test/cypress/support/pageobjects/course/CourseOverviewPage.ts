import { BASE_API, GET } from '../../constants';

/**
 * A class which encapsulates UI selectors and actions for the course overview page (/courses/*).
 */
export class CourseOverviewPage {
    readonly participationRequestId = 'participateInExerciseQuery';

    search(term: string): void {
        cy.get('#exercise-search-input').type(term);
        cy.get('#exercise-search-button').click();
    }

    startExercise(exerciseID: number) {
        cy.reloadUntilFound(`#start-exercise-${exerciseID}`);
        cy.get(`#start-exercise-${exerciseID}`).click();
    }

    getExercise(exerciseID: number) {
        return cy.get(`#exercise-card-${exerciseID}`);
    }

    openRunningExercise(exerciseID: number) {
        cy.reloadUntilFound(`#open-exercise-${exerciseID}`);
        cy.get('#open-exercise-' + exerciseID).click();
    }

    openRunningProgrammingExercise(exerciseID: number) {
        cy.intercept(GET, BASE_API + 'programming-exercise-participations/*/student-participation-with-latest-result-and-feedbacks').as('initialQuery');
        this.openRunningExercise(exerciseID);
        cy.wait('@initialQuery');
    }

    openExamsTab() {
        cy.get('#exam-tab').click();
    }

    openExam(examId: number) {
        cy.get('#exam-' + examId).click();
    }
}
