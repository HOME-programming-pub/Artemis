import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { combineLatest } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { AlertService } from 'app/core/util/alert.service';
import { TextUnit } from 'app/entities/lecture-unit/textUnit.model';
import { TextUnitFormData } from 'app/lecture/lecture-unit/lecture-unit-management/text-unit-form/text-unit-form.component';
import { TextUnitService } from 'app/lecture/lecture-unit/lecture-unit-management/textUnit.service';
import { onError } from 'app/shared/util/global.utils';

@Component({
    selector: 'jhi-create-text-unit',
    templateUrl: './create-text-unit.component.html',
    styles: [],
})
export class CreateTextUnitComponent implements OnInit {
    textUnitToCreate: TextUnit = new TextUnit();
    isLoading: boolean;
    lectureId: number;
    courseId: number;
    constructor(private activatedRoute: ActivatedRoute, private router: Router, private textUnitService: TextUnitService, private alertService: AlertService) {}

    ngOnInit(): void {
        const lectureRoute = this.activatedRoute.parent!.parent!;
        combineLatest([lectureRoute.paramMap, lectureRoute.parent!.paramMap]).subscribe(([params, parentParams]) => {
            this.lectureId = Number(params.get('lectureId'));
            this.courseId = Number(parentParams.get('courseId'));
        });
        this.textUnitToCreate = new TextUnit();
    }

    createTextUnit(formData: TextUnitFormData) {
        if (!formData?.name) {
            return;
        }

        const { name, releaseDate, content, learningGoals } = formData;

        this.textUnitToCreate.name = name;
        this.textUnitToCreate.releaseDate = releaseDate;
        this.textUnitToCreate.content = content;
        this.textUnitToCreate.learningGoals = learningGoals || [];

        this.isLoading = true;

        this.textUnitService
            .create(this.textUnitToCreate!, this.lectureId)
            .pipe(
                finalize(() => {
                    this.isLoading = false;
                }),
            )
            .subscribe({
                next: () => {
                    this.router.navigate(['../../'], { relativeTo: this.activatedRoute });
                },
                error: (res: HttpErrorResponse) => onError(this.alertService, res),
            });
    }
}
