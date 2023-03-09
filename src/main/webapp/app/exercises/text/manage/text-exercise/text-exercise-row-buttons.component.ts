import { HttpErrorResponse } from '@angular/common/http';
import { Component, Input } from '@angular/core';
import { faListAlt } from '@fortawesome/free-regular-svg-icons';
import { faBook, faTable, faTimes, faUsers, faWrench } from '@fortawesome/free-solid-svg-icons';
import { EventManager } from 'app/core/util/event-manager.service';
import { TextExercise } from 'app/entities/text-exercise.model';
import { TextExerciseService } from 'app/exercises/text/manage/text-exercise/text-exercise.service';
import { Subject } from 'rxjs';

@Component({
    selector: 'jhi-text-exercise-row-buttons',
    templateUrl: './text-exercise-row-buttons.component.html',
})
export class TextExerciseRowButtonsComponent {
    @Input() courseId: number;
    @Input() exercise: TextExercise;
    private dialogErrorSource = new Subject<string>();
    dialogError$ = this.dialogErrorSource.asObservable();

    // Icons
    faTimes = faTimes;
    faBook = faBook;
    faWrench = faWrench;
    faUsers = faUsers;
    faTable = faTable;
    farListAlt = faListAlt;

    constructor(private textExerciseService: TextExerciseService, private eventManager: EventManager) {}

    deleteExercise() {
        this.textExerciseService.delete(this.exercise.id!).subscribe({
            next: () => {
                this.eventManager.broadcast({
                    name: 'textExerciseListModification',
                    content: 'Deleted a textExercise',
                });
                this.dialogErrorSource.next('');
            },
            error: (error: HttpErrorResponse) => this.dialogErrorSource.next(error.message),
        });
    }
}
