import { NgModule } from '@angular/core';
import { GitDiffFilePanelComponent } from 'app/exercises/programming/hestia/git-diff-report/git-diff-file-panel.component';
import { GitDiffFileComponent } from 'app/exercises/programming/hestia/git-diff-report/git-diff-file.component';
import { GitDiffLineStatComponent } from 'app/exercises/programming/hestia/git-diff-report/git-diff-line-stat.component';
import { GitDiffReportModalComponent } from 'app/exercises/programming/hestia/git-diff-report/git-diff-report-modal.component';
import { AceEditorModule } from 'app/shared/markdown-editor/ace-editor/ace-editor.module';
import { ArtemisSharedModule } from 'app/shared/shared.module';
import { GitDiffReportComponent } from './git-diff-report.component';

@NgModule({
    imports: [ArtemisSharedModule, AceEditorModule],
    declarations: [GitDiffFilePanelComponent, GitDiffReportComponent, GitDiffFileComponent, GitDiffReportModalComponent, GitDiffLineStatComponent],
    exports: [GitDiffReportComponent, GitDiffReportModalComponent, GitDiffLineStatComponent],
})
export class GitDiffReportModule {}
