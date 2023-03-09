import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable, ReplaySubject } from 'rxjs';
import dayjs from 'dayjs/esm';
import { map } from 'rxjs/operators';

import { createRequestOption } from 'app/shared/util/request.util';
import { Params, Router, UrlSerializer } from '@angular/router';
import { AccountService } from 'app/core/auth/account.service';
import { JhiWebsocketService } from 'app/core/websocket/websocket.service';
import { User } from 'app/core/user/user.model';
import { GroupNotification, GroupNotificationType } from 'app/entities/group-notification.model';
import {
    NEW_ANNOUNCEMENT_POST_TITLE,
    NEW_COURSE_POST_TITLE,
    NEW_EXERCISE_POST_TITLE,
    NEW_LECTURE_POST_TITLE,
    NEW_REPLY_FOR_COURSE_POST_TITLE,
    NEW_REPLY_FOR_EXERCISE_POST_TITLE,
    NEW_REPLY_FOR_LECTURE_POST_TITLE,
    Notification,
    QUIZ_EXERCISE_STARTED_TEXT,
    QUIZ_EXERCISE_STARTED_TITLE,
} from 'app/entities/notification.model';
import { Course } from 'app/entities/course.model';
import { CourseManagementService } from 'app/course/manage/course-management.service';
import { QuizExercise, QuizMode } from 'app/entities/quiz/quiz-exercise.model';
import { MetisService } from 'app/shared/metis/metis.service';
import { RouteComponents } from 'app/shared/metis/metis.util';
import { convertDateFromServer } from 'app/utils/date.utils';
import { TutorialGroupsNotificationService } from 'app/course/tutorial-groups/services/tutorial-groups-notification.service';
import { TutorialGroup } from 'app/entities/tutorial-group/tutorial-group.model';
import { translationNotFoundMessage } from 'app/core/config/translation.config';
import { ArtemisTranslatePipe } from 'app/shared/pipes/artemis-translate.pipe';

@Injectable({ providedIn: 'root' })
export class NotificationService {
    public resourceUrl = SERVER_API_URL + 'api/notifications';
    subscribedTopics: string[] = [];
    notificationObserver: ReplaySubject<Notification>;
    cachedNotifications: Observable<HttpResponse<Notification[]>>;

    constructor(
        private jhiWebsocketService: JhiWebsocketService,
        private router: Router,
        private http: HttpClient,
        private accountService: AccountService,
        private courseManagementService: CourseManagementService,
        private serializer: UrlSerializer,
        private tutorialGroupsNotificationService: TutorialGroupsNotificationService,
        private artemisTranslatePipe: ArtemisTranslatePipe,
    ) {
        this.initNotificationObserver();
    }

    /**
     * Query all notifications with respect to the current user's notification settings.
     * @param req request options
     * @return Observable<HttpResponse<Notification[]>>
     */
    queryNotificationsFilteredBySettings(req?: any): Observable<HttpResponse<Notification[]>> {
        const options = createRequestOption(req);
        return this.http
            .get<Notification[]>(this.resourceUrl, { params: options, observe: 'response' })
            .pipe(map((res: HttpResponse<Notification[]>) => this.convertNotificationResponseArrayDateFromServer(res)));
    }

    /**
     * Navigate to notification target or build router components and params for post related notifications
     * @param {GroupNotification} notification
     */
    interpretNotification(notification: GroupNotification): void {
        if (notification.target) {
            const target = JSON.parse(notification.target);
            const targetCourseId = target.course || notification.course?.id;

            if (notification.title === QUIZ_EXERCISE_STARTED_TITLE) {
                this.router.navigate([target.mainPage, targetCourseId, 'quiz-exercises', target.id, 'live']);
            } else if (
                // check with plain strings is needed to support legacy notifications that were created before it was possible to translate notifications
                notification.title === NEW_ANNOUNCEMENT_POST_TITLE ||
                notification.title === 'New announcement' ||
                notification.title === NEW_COURSE_POST_TITLE ||
                notification.title === 'New course-wide post' ||
                notification.title === NEW_REPLY_FOR_COURSE_POST_TITLE ||
                notification.title === 'New reply for course-wide post'
            ) {
                const queryParams: Params = MetisService.getQueryParamsForCoursePost(target.id);
                const routeComponents: RouteComponents = MetisService.getLinkForCoursePost(targetCourseId);
                this.navigateToNotificationTarget(targetCourseId, routeComponents, queryParams);
            } else if (
                notification.title === NEW_EXERCISE_POST_TITLE ||
                notification.title === 'New exercise post' ||
                notification.title === NEW_REPLY_FOR_EXERCISE_POST_TITLE ||
                notification.title === 'New reply for exercise post'
            ) {
                const queryParams: Params = MetisService.getQueryParamsForLectureOrExercisePost(target.id);
                const routeComponents: RouteComponents = MetisService.getLinkForExercisePost(targetCourseId, target.exercise ?? target.exerciseId);
                this.navigateToNotificationTarget(targetCourseId, routeComponents, queryParams);
            } else if (
                notification.title === NEW_LECTURE_POST_TITLE ||
                notification.title === 'New lecture post' ||
                notification.title === NEW_REPLY_FOR_LECTURE_POST_TITLE ||
                notification.title === 'New reply for lecture post'
            ) {
                const queryParams: Params = MetisService.getQueryParamsForLectureOrExercisePost(target.id);
                const routeComponents: RouteComponents = MetisService.getLinkForLecturePost(targetCourseId, target.lecture ?? target.lectureId);
                this.navigateToNotificationTarget(targetCourseId, routeComponents, queryParams);
            } else {
                this.router.navigate([target.mainPage, targetCourseId, target.entity, target.id]);
            }
        }
    }

    /**
     * Navigate to post related targets, decide if reload is required, i.e. when switching course context
     * @param {number} targetCourseId
     * @param {RouteComponents} routeComponents
     * @param {Params} queryParams
     */
    navigateToNotificationTarget(targetCourseId: number, routeComponents: RouteComponents, queryParams: Params): void {
        const currentCourseId = NotificationService.getCurrentCourseId();
        // determine if reload is required when notification is clicked
        // by comparing the id of the course the user is currently in and the course the post associated with the notification belongs to
        if (currentCourseId === undefined || currentCourseId !== targetCourseId) {
            const tree = this.router.createUrlTree(routeComponents, { queryParams });
            // navigate by string url to force reload when switching the course context
            window.location.href = this.serializer.serialize(tree);
        } else {
            // navigate with router when staying in same course context when clicking on notification
            this.router.navigate(routeComponents, { queryParams });
        }
    }

    private static getCurrentCourseId(): number | undefined {
        // read course id from url
        const matchCourseIdInURL = window.location.pathname.match(/.*\/courses\/(\d+)\/.*/);
        return matchCourseIdInURL ? +matchCourseIdInURL[1] : undefined;
    }

    /**
     * Init new observer for notifications and reset topics.
     */
    cleanUp(): void {
        this.cachedNotifications = new Observable<HttpResponse<Notification[]>>();
        this.initNotificationObserver();
        this.subscribedTopics = [];
    }

    /**
     * Subscribe to single user notification, group notification and quiz updates if it was not already subscribed.
     * Then it returns a BehaviorSubject the calling component can listen on to actually receive the notifications.
     * @returns {ReplaySubject<Notification>}
     */
    subscribeToNotificationUpdates(): ReplaySubject<Notification> {
        this.subscribeToSingleUserNotificationUpdates();
        this.courseManagementService.getCoursesForNotifications().subscribe((courses) => {
            if (courses) {
                this.subscribeToGroupNotificationUpdates(courses);
                this.subscribeToQuizUpdates(courses);
            }
        });
        this.tutorialGroupsNotificationService.getTutorialGroupsForNotifications().subscribe((tutorialGroups) => {
            if (tutorialGroups) {
                this.subscribeToTutorialGroupNotificationUpdates(tutorialGroups);
            }
        });
        return this.notificationObserver;
    }

    private subscribeToSingleUserNotificationUpdates(): void {
        this.accountService.identity().then((user: User | undefined) => {
            if (user) {
                const userTopic = `/topic/user/${user.id}/notifications`;
                if (!this.subscribedTopics.includes(userTopic)) {
                    this.subscribedTopics.push(userTopic);
                    this.jhiWebsocketService.subscribe(userTopic);
                    this.jhiWebsocketService.receive(userTopic).subscribe((notification: Notification) => {
                        this.addNotificationToObserver(notification);
                    });
                }
            }
        });
    }

    private subscribeToGroupNotificationUpdates(courses: Course[]): void {
        courses.forEach((course) => {
            let courseTopic = `/topic/course/${course.id}/${GroupNotificationType.STUDENT}`;
            if (this.accountService.isAtLeastInstructorInCourse(course)) {
                courseTopic = `/topic/course/${course.id}/${GroupNotificationType.INSTRUCTOR}`;
            } else if (this.accountService.isAtLeastEditorInCourse(course)) {
                courseTopic = `/topic/course/${course.id}/${GroupNotificationType.EDITOR}`;
            } else if (this.accountService.isAtLeastTutorInCourse(course)) {
                courseTopic = `/topic/course/${course.id}/${GroupNotificationType.TA}`;
            }
            if (!this.subscribedTopics.includes(courseTopic)) {
                this.subscribedTopics.push(courseTopic);
                this.jhiWebsocketService.subscribe(courseTopic);
                this.jhiWebsocketService.receive(courseTopic).subscribe((notification: Notification) => {
                    this.addNotificationToObserver(notification);
                });
            }
        });
    }

    private subscribeToTutorialGroupNotificationUpdates(tutorialGroups: TutorialGroup[]): void {
        tutorialGroups.forEach((tutorialGroup) => {
            const tutorialGroupTopic = '/topic/tutorial-group/' + tutorialGroup.id + '/notifications';
            if (!this.subscribedTopics.includes(tutorialGroupTopic)) {
                this.subscribedTopics.push(tutorialGroupTopic);
                this.jhiWebsocketService.subscribe(tutorialGroupTopic);
                this.jhiWebsocketService.receive(tutorialGroupTopic).subscribe((notification: Notification) => {
                    this.addNotificationToObserver(notification);
                });
            }
        });
    }

    private subscribeToQuizUpdates(courses: Course[]): void {
        courses.forEach((course) => {
            const quizExerciseTopic = '/topic/courses/' + course.id + '/quizExercises';
            if (!this.subscribedTopics.includes(quizExerciseTopic)) {
                this.subscribedTopics.push(quizExerciseTopic);
                this.jhiWebsocketService.subscribe(quizExerciseTopic);
                this.jhiWebsocketService.receive(quizExerciseTopic).subscribe((quizExercise: QuizExercise) => {
                    if (
                        quizExercise.visibleToStudents &&
                        quizExercise.quizMode === QuizMode.SYNCHRONIZED &&
                        quizExercise.quizBatches?.[0]?.started &&
                        !quizExercise.isOpenForPractice
                    ) {
                        this.addNotificationToObserver(NotificationService.createNotificationFromStartedQuizExercise(quizExercise));
                    }
                });
            }
        });
    }

    private static createNotificationFromStartedQuizExercise(quizExercise: QuizExercise): GroupNotification {
        return {
            title: QUIZ_EXERCISE_STARTED_TITLE,
            text: QUIZ_EXERCISE_STARTED_TEXT,
            textIsPlaceholder: true,
            placeholderValues: '["' + quizExercise.title + '"]',
            notificationDate: dayjs(),
            target: JSON.stringify({
                course: quizExercise.course!.id,
                mainPage: 'courses',
                entity: 'exercises',
                id: quizExercise.id,
            }),
        } as GroupNotification;
    }

    private addNotificationToObserver(notification: Notification): void {
        if (notification && notification.notificationDate) {
            notification.notificationDate = dayjs(notification.notificationDate);
            this.notificationObserver.next(notification);
        }
    }

    private convertNotificationResponseArrayDateFromServer(res: HttpResponse<Notification[]>): HttpResponse<Notification[]> {
        if (res.body) {
            res.body.forEach((notification: Notification) => {
                notification.notificationDate = convertDateFromServer(notification.notificationDate);
            });
        }
        return res;
    }

    /**
     * Set new notification observer.
     */
    private initNotificationObserver(): void {
        this.notificationObserver = new ReplaySubject<Notification>();
    }

    // we use this method instead of the regular translation pipe to be able to also display legacy notifications that were created
    // before it was possible to translate notifications
    getNotificationTitleTranslation(notification: Notification): string {
        if (notification.textIsPlaceholder) {
            let translation = this.artemisTranslatePipe.transform(notification.title);
            if (translation.includes(translationNotFoundMessage)) {
                return notification.title ? notification.title : 'No title found';
            }
            return translation;
        } else {
            return notification.title ? notification.title : 'No title found';
        }
    }

    // we use this method instead of the regular translation pipe to be able to also display legacy notifications that were created
    // before it was possible to translate notifications
    getNotificationTextTranslation(notification: Notification): string {
        if (notification.textIsPlaceholder) {
            let translation = this.artemisTranslatePipe.transform(notification.text, this.getParsedPlaceholderValues(notification));
            if (translation.includes(translationNotFoundMessage)) {
                return notification.text ? notification.text : 'No text found';
            }
            return translation;
        } else {
            return notification.text ? notification.text : 'No text found';
        }
    }

    private getParsedPlaceholderValues(notification: Notification): string[] {
        if (notification.placeholderValues) {
            return JSON.parse(notification.placeholderValues);
        }
        return [];
    }
}
