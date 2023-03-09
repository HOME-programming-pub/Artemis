import { Component, Input, OnDestroy } from '@angular/core';
import { Theme, ThemeService } from 'app/core/theme/theme.service';
import { EmojiUtils } from 'app/shared/metis/emoji/emoji.utils';
import { Subscription } from 'rxjs';

@Component({
    selector: 'jhi-emoji',
    templateUrl: './emoji.component.html',
    styleUrls: ['./emoji.component.scss'],
})
export class EmojiComponent implements OnDestroy {
    utils = EmojiUtils;

    @Input() emoji: string;

    dark = false;
    themeSubscription: Subscription;

    constructor(private themeService: ThemeService) {
        this.themeSubscription = themeService.getCurrentThemeObservable().subscribe((theme) => {
            this.dark = theme === Theme.DARK;
        });
    }

    ngOnDestroy(): void {
        this.themeSubscription.unsubscribe();
    }
}
