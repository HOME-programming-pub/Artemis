import { Routes } from '@angular/router';

import { ExampleTextSubmissionComponent } from './example-text-submission.component';
import { UserRouteAccessService } from 'app/core/auth/user-route-access-service';
import { Authority } from 'app/shared/constants/authority.constants';

export const exampleTextSubmissionRoute: Routes = [
    {
        path: '',
        component: ExampleTextSubmissionComponent,
        data: {
            authorities: [Authority.ADMIN, Authority.INSTRUCTOR, Authority.EDITOR, Authority.TA],
            pageTitle: 'artemisApp.exampleSubmission.home.editor',
        },
        canActivate: [UserRouteAccessService],
    },
];
