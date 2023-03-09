import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Lecture } from 'app/entities/lecture.model';
import { LectureUpdateWizardAttachmentsComponent } from 'app/lecture/wizard-mode/lecture-wizard-attachments.component';
import { ArtemisTranslatePipe } from 'app/shared/pipes/artemis-translate.pipe';
import { MockPipe } from 'ng-mocks';

describe('LectureWizardAttachmentsComponent', () => {
    let wizardAttachmentsComponentFixture: ComponentFixture<LectureUpdateWizardAttachmentsComponent>;
    let wizardAttachmentsComponent: LectureUpdateWizardAttachmentsComponent;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [],
            declarations: [LectureUpdateWizardAttachmentsComponent, MockPipe(ArtemisTranslatePipe)],
            providers: [],
            schemas: [],
        })
            .compileComponents()
            .then(() => {
                wizardAttachmentsComponentFixture = TestBed.createComponent(LectureUpdateWizardAttachmentsComponent);
                wizardAttachmentsComponent = wizardAttachmentsComponentFixture.componentInstance;
                wizardAttachmentsComponent.lecture = new Lecture();
            });
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    it('should initialize', () => {
        wizardAttachmentsComponentFixture.detectChanges();
        expect(wizardAttachmentsComponent).not.toBeNull();
    });
});
