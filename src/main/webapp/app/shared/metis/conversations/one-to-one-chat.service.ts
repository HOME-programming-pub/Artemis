import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { OneToOneChatDTO } from 'app/entities/metis/conversation/one-to-one-chat.model';
import { ConversationService } from 'app/shared/metis/conversations/conversation.service';

@Injectable({ providedIn: 'root' })
export class OneToOneChatService {
    public resourceUrl = SERVER_API_URL + '/api/courses/';

    constructor(private http: HttpClient, private conversationService: ConversationService) {}

    create(courseId: number, loginOfChatPartner: string): Observable<HttpResponse<OneToOneChatDTO>> {
        return this.http
            .post<OneToOneChatDTO>(`${this.resourceUrl}${courseId}/one-to-one-chats`, [loginOfChatPartner], { observe: 'response' })
            .pipe(map(this.conversationService.convertDateFromServer));
    }
}
