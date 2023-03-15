import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NgbPopoverModule } from '@ng-bootstrap/ng-bootstrap';
import { BehaviorSubject } from 'rxjs';

import { TranslatePipeMock } from '../../helpers/mocks/service/mock-translate.service';
import { ArtemisTestModule } from '../../test.module';
import { ConnectionState, JhiWebsocketService } from 'app/core/websocket/websocket.service';
import { CloseCircleComponent } from 'app/shared/close-circle/close-circle.component';
import { JhiConnectionWarningComponent } from 'app/shared/connection-warning/connection-warning.component';

describe('ConnectionWarning', () => {
    let fixture: ComponentFixture<JhiConnectionWarningComponent>;
    let component: JhiConnectionWarningComponent;
    let subject: BehaviorSubject<ConnectionState>;

    beforeEach(() => {
        subject = new BehaviorSubject<ConnectionState>(new ConnectionState(true, true, false));
        TestBed.configureTestingModule({
            imports: [ArtemisTestModule, NgbPopoverModule],
            declarations: [JhiConnectionWarningComponent, CloseCircleComponent, TranslatePipeMock],
            providers: [
                {
                    provide: JhiWebsocketService,
                    useValue: {
                        connectionState: subject.asObservable(),
                    },
                },
            ],
        })
            .compileComponents()
            .then(() => {
                fixture = TestBed.createComponent(JhiConnectionWarningComponent);
                component = fixture.componentInstance;
            });
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    it('should show the indicator and popover on dropped connection', fakeAsync(() => {
        fixture.detectChanges();

        expect(component.disconnected).toBeFalse();
        expect(component.popover.isOpen()).toBeFalse();

        const warningDiv = fixture.debugElement.query(By.css('.connection-warning'));
        expect(warningDiv).not.toBeNull();
        expect(warningDiv.classes).not.toContainEntry(['disconnected', true]);

        subject.next(new ConnectionState(false, true, false));
        fixture.detectChanges();

        expect(component.disconnected).toBeTrue();
        expect(warningDiv.classes).toContainEntry(['disconnected', true]);

        tick(500);
        expect(component.popover.isOpen()).toBeTrue();

        subject.next(new ConnectionState(true, true, false));
        fixture.detectChanges();

        tick(100);
        expect(component.disconnected).toBeFalse();
        expect(component.popover.isOpen()).toBeFalse();
        expect(warningDiv.classes).not.toContainEntry(['disconnected', true]);
    }));
});
