import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ArtemisDatePipe } from 'app/shared/pipes/artemis-date.pipe';
import { ArtemisTranslatePipe } from 'app/shared/pipes/artemis-translate.pipe';
import { MockComponent, MockDirective, MockPipe } from 'ng-mocks';
import { ArtemisTestModule } from '../../../../test.module';
import { TranslateDirective } from 'app/shared/language/translate.directive';
import { QuizPoolComponent } from 'app/exercises/quiz/manage/quiz-pool.component';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { QuizPoolService } from 'app/exercises/quiz/manage/quiz-pool.service';
import { of, throwError } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { QuizPool } from 'app/entities/quiz/quiz-pool.model';
import { MultipleChoiceQuestion } from 'app/entities/quiz/multiple-choice-question.model';
import { QuizGroup } from 'app/entities/quiz/quiz-group.model';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { ExamManagementService } from 'app/exam/manage/exam-management.service';
import { Exam } from 'app/entities/exam.model';
import dayjs from 'dayjs/esm';
import { QuizPoolMappingComponent } from 'app/exercises/quiz/manage/quiz-pool-mapping.component';
import { QuizQuestionListEditComponent } from 'app/exercises/quiz/manage/quiz-question-list-edit.component';

describe('QuizPoolComponent', () => {
    let fixture: ComponentFixture<QuizPoolComponent>;
    let component: QuizPoolComponent;
    let quizPoolService: QuizPoolService;
    let examService: ExamManagementService;

    const courseId = 1;
    const examId = 2;
    const route = { snapshot: { paramMap: convertToParamMap({ courseId, examId }) }, queryParams: of({}) } as any as ActivatedRoute;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [ArtemisTestModule, HttpClientTestingModule, NgbModule],
            declarations: [
                QuizPoolComponent,
                MockComponent(QuizPoolMappingComponent),
                MockComponent(QuizQuestionListEditComponent),
                MockPipe(ArtemisTranslatePipe),
                MockPipe(ArtemisDatePipe),
                MockDirective(TranslateDirective),
            ],
            providers: [{ provide: ActivatedRoute, useValue: route }],
        })
            .compileComponents()
            .then(() => {
                fixture = TestBed.createComponent(QuizPoolComponent);
                component = fixture.componentInstance;
                quizPoolService = fixture.debugElement.injector.get(QuizPoolService);
                examService = fixture.debugElement.injector.get(ExamManagementService);
                fixture.detectChanges();
            });
    });

    it('should initialize quiz pool', () => {
        const quizPool = new QuizPool();
        const quizGroup = new QuizGroup('Test Group');
        const quizQuestion = new MultipleChoiceQuestion();
        quizQuestion.quizGroup = quizGroup;
        quizPool.id = 1;
        quizPool.quizQuestions = [quizQuestion];
        quizPool.quizGroups = [quizGroup];
        jest.spyOn(quizPoolService, 'find').mockReturnValue(of(new HttpResponse<QuizPool>({ body: quizPool })));
        component.ngOnInit();
        expect(component.quizPool).toEqual(quizPool);
    });

    it('should initialize quiz pool with new object if existing quiz pool is not found', () => {
        jest.spyOn(quizPoolService, 'find').mockReturnValue(throwError(() => ({ status: 404 })));
        component.ngOnInit();
        expect(component.quizPool.quizGroups).toBeArrayOfSize(0);
        expect(component.quizPool.quizQuestions).toBeArrayOfSize(0);
    });

    it('should set isExamStarted to true', () => {
        const exam = new Exam();
        exam.startDate = dayjs().subtract(1, 'hour');
        jest.spyOn(examService, 'find').mockReturnValue(of(new HttpResponse<Exam>({ body: exam })));
        component.ngOnInit();
        expect(component.isExamStarted).toBeTrue();
    });

    it('should set isExamStarted to false', () => {
        const exam = new Exam();
        exam.startDate = dayjs().add(1, 'hour');
        jest.spyOn(examService, 'find').mockReturnValue(of(new HttpResponse<Exam>({ body: exam })));
        component.ngOnInit();
        expect(component.isExamStarted).toBeFalse();
    });

    it('should call QuizGroupQuestionMappingComponent.addQuestion when there is a new question', () => {
        component.quizPool = new QuizPool();
        component.quizPoolMappingComponent = new QuizPoolMappingComponent();
        const addQuestionSpy = jest.spyOn(component.quizPoolMappingComponent, 'addQuestion');
        const quizQuestion = new MultipleChoiceQuestion();
        component.handleQuestionAdded(quizQuestion);
        expect(addQuestionSpy).toHaveBeenCalledOnceWith(quizQuestion);
    });

    it('should call QuizGroupQuestionMappingComponent.deleteQuestion when a question is deleted', () => {
        component.quizPool = new QuizPool();
        component.quizPoolMappingComponent = new QuizPoolMappingComponent();
        const deleteQuestionSpy = jest.spyOn(component.quizPoolMappingComponent, 'deleteQuestion');
        const quizQuestion = new MultipleChoiceQuestion();
        component.handleQuestionDeleted(quizQuestion);
        expect(deleteQuestionSpy).toHaveBeenCalledOnceWith(quizQuestion);
    });

    it('should call QuizPoolService.update when saving', () => {
        const quizPool = new QuizPool();
        component.courseId = courseId;
        component.examId = examId;
        component.quizPool = quizPool;
        component.hasPendingChanges = true;
        component.isValid = true;
        component.quizQuestionsEditComponent = new QuizQuestionListEditComponent();
        component.quizPoolMappingComponent = new QuizPoolMappingComponent();
        const parseAllQuestionsSpy = jest.spyOn(component.quizQuestionsEditComponent, 'parseAllQuestions').mockImplementation();
        const getMaxPointsSpy = jest.spyOn(component.quizPoolMappingComponent, 'getMaxPoints').mockImplementation();
        const updateQuizPoolSpy = jest.spyOn(quizPoolService, 'update').mockReturnValue(of(new HttpResponse<QuizPool>({ body: quizPool })));
        component.save();
        expect(parseAllQuestionsSpy).toHaveBeenCalledOnce();
        expect(getMaxPointsSpy).toHaveBeenCalledOnce();
        expect(updateQuizPoolSpy).toHaveBeenCalledOnce();
    });

    it('should not call QuizPoolService.update if there is no pending changes or is not valid', () => {
        const quizPool = new QuizPool();
        component.courseId = courseId;
        component.examId = examId;
        component.quizPool = quizPool;
        component.hasPendingChanges = false;
        component.isValid = false;
        const updateQuizPoolSpy = jest.spyOn(quizPoolService, 'update').mockImplementation();
        component.save();
        expect(updateQuizPoolSpy).toHaveBeenCalledTimes(0);
    });
});