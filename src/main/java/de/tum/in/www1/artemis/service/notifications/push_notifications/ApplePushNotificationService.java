package de.tum.in.www1.artemis.service.notifications.push_notifications;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;

import de.tum.in.www1.artemis.config.RestTemplateConfiguration;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.notification.Notification;
import de.tum.in.www1.artemis.domain.push_notification.PushNotificationDeviceType;
import de.tum.in.www1.artemis.repository.PushNotificationDeviceConfigurationRepository;

/**
 * Handles the sending of iOS Notifications to the Relay Service
 */
@Service
public class ApplePushNotificationService extends PushNotificationService {

    private final PushNotificationDeviceConfigurationRepository repository;

    private final Logger log = LoggerFactory.getLogger(FirebasePushNotificationService.class);

    @Value("${artemis.push-notification-relay:#{Optional.empty()}}")
    private Optional<String> relayServerBaseUrl;

    public ApplePushNotificationService(PushNotificationDeviceConfigurationRepository repository) {
        this.repository = repository;
    }

    @Override
    public void sendNotification(Notification notification, List<User> users, Object notificationSubject) {
        super.sendNotification(notification, users, notificationSubject);
    }

    @Override
    void sendNotificationRequestsToEndpoint(List<RelayNotificationRequest> requests, String relayServerBaseUrl) {
        RestTemplateConfiguration restTemplateConfiguration = new RestTemplateConfiguration();
        RestTemplate restTemplate = restTemplateConfiguration.restTemplate();

        for (RelayNotificationRequest request : requests) {
            sendRelayRequest(restTemplate, request, relayServerBaseUrl);
        }
    }

    @Async
    void sendRelayRequest(RestTemplate restTemplate, RelayNotificationRequest request, String relayServerBaseUrl) {
        RetryTemplate template = RetryTemplate.builder().exponentialBackoff(1000, 2, 60 * 1000).retryOn(RestClientException.class).maxAttempts(40).build();

        try {
            template.execute((RetryCallback<Void, RestClientException>) context -> {
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                String body = new Gson().toJson(request);
                HttpEntity<String> httpEntity = new HttpEntity<>(body, httpHeaders);
                restTemplate.postForObject(relayServerBaseUrl + "/api/push_notification/send_apns", httpEntity, String.class);

                return null;
            });
        }
        catch (RestClientException e) {
            log.error("Could not send APNS notifications", e);
        }
    }

    @Override
    public PushNotificationDeviceConfigurationRepository getRepository() {
        return repository;
    }

    @Override
    PushNotificationDeviceType getDeviceType() {
        return PushNotificationDeviceType.APNS;
    }

    @Override
    Optional<String> getRelayBaseUrl() {
        return relayServerBaseUrl;
    }
}
