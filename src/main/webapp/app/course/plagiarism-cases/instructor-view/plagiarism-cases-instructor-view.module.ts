import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { PlagiarismCasesInstructorViewComponent } from 'app/course/plagiarism-cases/instructor-view/plagiarism-cases-instructor-view.component';
import { PlagiarismCaseInstructorDetailViewComponent } from 'app/course/plagiarism-cases/instructor-view/detail-view/plagiarism-case-instructor-detail-view.component';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access-service';
import { ArtemisPlagiarismCasesSharedModule } from 'app/course/plagiarism-cases/shared/plagiarism-cases-shared.module';
import { ArtemisCoursesModule } from 'app/overview/courses.module';
import { CourseManagementResolve } from 'app/course/manage/course-management-resolve.service';

const routes: Routes = [
    {
        path: '',
        component: PlagiarismCasesInstructorViewComponent,
        resolve: {
            course: CourseManagementResolve,
        },
        data: {
            authorities: [Authority.INSTRUCTOR, Authority.ADMIN],
            pageTitle: 'artemisApp.plagiarism.cases.pageTitle',
        },
        canActivate: [UserRouteAccessService],
    },
    {
        path: ':plagiarismCaseId',
        component: PlagiarismCaseInstructorDetailViewComponent,
        data: {
            authorities: [Authority.INSTRUCTOR, Authority.ADMIN],
            pageTitle: 'artemisApp.plagiarism.cases.pageTitle',
        },
        canActivate: [UserRouteAccessService],
    },
];

@NgModule({
    imports: [RouterModule.forChild(routes), ArtemisPlagiarismCasesSharedModule, ArtemisCoursesModule],
    declarations: [PlagiarismCasesInstructorViewComponent, PlagiarismCaseInstructorDetailViewComponent],
})
export class ArtemisPlagiarismCasesInstructorViewModule {}
