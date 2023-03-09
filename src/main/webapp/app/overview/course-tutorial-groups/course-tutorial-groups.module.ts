import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { ArtemisTutorialGroupsSharedModule } from 'app/course/tutorial-groups/shared/tutorial-groups-shared.module';
import { CourseTutorialGroupsComponent } from 'app/overview/course-tutorial-groups/course-tutorial-groups.component';
import { ArtemisSharedModule } from 'app/shared/shared.module';
import { CourseTutorialGroupCardComponent } from './course-tutorial-group-card/course-tutorial-group-card.component';
import { CourseTutorialGroupsOverviewComponent } from './course-tutorial-groups-overview/course-tutorial-groups-overview.component';
import { CourseTutorialGroupsRegisteredComponent } from './course-tutorial-groups-registered/course-tutorial-groups-registered.component';
import { routes } from './course-tutorial-groups.route';

@NgModule({
    imports: [RouterModule.forChild(routes), ArtemisSharedModule, ArtemisTutorialGroupsSharedModule],
    declarations: [CourseTutorialGroupsComponent, CourseTutorialGroupCardComponent, CourseTutorialGroupsOverviewComponent, CourseTutorialGroupsRegisteredComponent],
})
export class CourseTutorialGroupsModule {}
