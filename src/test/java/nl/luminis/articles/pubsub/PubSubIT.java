package nl.luminis.articles.pubsub;

import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import nl.luminis.articles.pubsub.dto.DummyMessage;
import nl.luminis.articles.pubsub.publisher.DummyMessagePublisher;
import nl.luminis.articles.pubsub.subscriber.DummyMessageSubscriber;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.GenericContainer;

@Slf4j
@SpringBootTest
@ActiveProfiles({"test"})
@RunWith(SpringRunner.class)
public class PubSubIT {

    @ClassRule
    public static final GenericContainer PUBSUB_CONTAINER = TestContainerFactory.createPubSubContainer();

    @BeforeClass
    public static void setUp() {
        System.setProperty("gcloud.pubsub.url", PUBSUB_CONTAINER.getContainerIpAddress() + ":" + PUBSUB_CONTAINER.getMappedPort(
            TestContainerFactory.PUBSUB_PORT));
    }

    @Autowired
    private DummyMessagePublisher publisher;
    @Autowired
    private DummyMessageSubscriber subscriber;
    @Autowired
    private PubSubConfig pubSubConfig;
    @Autowired
    private PubSubTopicService pubSubTopicService;

    @Test
    public void testPublishAndSubscribe() {
        DummyMessage dummyMessage = DummyMessage.builder().id(1L).message("message").build();
        publisher.publish(dummyMessage);

        // It may takes some time for the subscriber to receive the message, wait to ensure we don't have flaky tests.
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> subscriber.getMessages().size() == 1);
    }

    @After
    public void tearDown() {
        // The Spring context only loads the subscriber once and reuses it. Make sure any state from previous tests is cleared out.
        subscriber.getMessages().clear();
        // The Docker container is reused and may contain state from previous tests. Seek subscribers ahead to the current timestamp.
        pubSubTopicService.reset(pubSubConfig.getProjectSubscriptionName());
    }
}
