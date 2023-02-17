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

import com.google.common.collect.Lists;
import com.google.gson.Gson;

import de.tum.in.www1.artemis.config.RestTemplateConfiguration;
import de.tum.in.www1.artemis.domain.push_notification.PushNotificationDeviceType;
import de.tum.in.www1.artemis.repository.PushNotificationDeviceConfigurationRepository;

/**
 * Handles the sending of Android Notifications to the Relay Service
 */
@Service
public class FirebasePushNotificationService extends PushNotificationService {

    private final Logger log = LoggerFactory.getLogger(FirebasePushNotificationService.class);

    private final PushNotificationDeviceConfigurationRepository repository;

    @Value("${artemis.push-notification-relay:#{Optional.empty()}}")
    private Optional<String> relayServerBaseUrl;

    public FirebasePushNotificationService(PushNotificationDeviceConfigurationRepository pushNotificationDeviceConfigurationRepository) {
        repository = pushNotificationDeviceConfigurationRepository;
    }

    @Override
    void sendNotificationRequestsToEndpoint(List<RelayNotificationRequest> requests, String relayBaseUrl) {
        // The relay server accepts at most 500 messages per batch
        List<List<RelayNotificationRequest>> batches = Lists.partition(requests, 500);
        for (List<RelayNotificationRequest> batch : batches) {
            scheduleSendBatch(batch, relayBaseUrl);
        }
    }

    @Async
    void scheduleSendBatch(List<RelayNotificationRequest> batch, String relayBaseUrl) {
        RetryTemplate template = RetryTemplate.builder().exponentialBackoff(1000, 2, 60 * 1000).retryOn(RestClientException.class).maxAttempts(40).build();

        try {
            template.execute((RetryCallback<Void, RestClientException>) context -> {
                RestTemplateConfiguration restTemplateConfiguration = new RestTemplateConfiguration();
                RestTemplate restTemplate = restTemplateConfiguration.restTemplate();

                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);

                String body = new Gson().toJson(new FirebaseRelayNotificationRequests(batch));
                HttpEntity<String> httpEntity = new HttpEntity<>(body, httpHeaders);
                restTemplate.postForObject(relayBaseUrl + "/api/push_notification/send_firebase", httpEntity, String.class);

                return null;
            });
        }
        catch (RestClientException e) {
            log.error("Could not send FIREBASE notifications", e);
        }
    }

    @Override
    public PushNotificationDeviceConfigurationRepository getRepository() {
        return repository;
    }

    @Override
    PushNotificationDeviceType getDeviceType() {
        return PushNotificationDeviceType.FIREBASE;
    }

    @Override
    Optional<String> getRelayBaseUrl() {
        return relayServerBaseUrl;
    }
}
