package org.niannarilli.keycloak.events;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.niannarilli.keycloak.events.model.EventAdminNotificationMqMsg;
import org.niannarilli.keycloak.events.model.EventClientNotificationMqMsg;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerTransaction;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
    

public class ActiveMqEventListenerProvider implements EventListenerProvider {

	private static final Logger log = Logger.getLogger(ActiveMqEventListenerProvider.class);
	
	private final ActiveMqConfig cfg;

	private final EventListenerTransaction tx = new EventListenerTransaction(this::publishAdminEvent, this::publishEvent);

	public ActiveMqEventListenerProvider(KeycloakSession session, ActiveMqConfig cfg) {
		this.cfg = cfg;
		session.getTransactionManager().enlistAfterCompletion(tx);
	}

	@Override
	public void close() {

	}

	@Override
	public void onEvent(Event event) {
		log.tracef("onEvent : %s%n", event.getType().name());
		if (!cfg.isPublish(event)) {
			tx.addEvent(event.clone());
		}
	}

	@Override
	public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
		log.tracef("onAdminEvent : %s%n", adminEvent.getOperationType().name());
		if (cfg.isPublish(adminEvent)) {
			tx.addAdminEvent(adminEvent, includeRepresentation);
		}
	}
	
	private void publishEvent(Event event) {
		log.tracef("publishEvent : %s%n", event.getType().name());
		EventClientNotificationMqMsg msg = EventClientNotificationMqMsg.create(event);
		String queueName = cfg.queueName(event.getRealmId(), event.getClientId(), false);
		String messageString = cfg.writeAsJson(msg, true);
		
		this.publishNotification(messageString, queueName);
	}
	
	private void publishAdminEvent(AdminEvent adminEvent, boolean includeRepresentation) {
		log.tracef("publishAdminEvent : %s%n", adminEvent.getOperationType().name());
		EventAdminNotificationMqMsg msg = EventAdminNotificationMqMsg.create(adminEvent);
		String queueName = cfg.queueName(adminEvent.getRealmId(), adminEvent.getResourceTypeAsString(), true);
		String messageString = cfg.writeAsJson(msg, true);

		this.publishNotification(messageString, queueName);
	}

	private void publishNotification(String messageString, String queueName) {
		Connection connection = null;
		Session session = null;
		MessageProducer producer = null;

		try {
			log.tracef("keycloak-to-activemq publishNotification");
			// Establish a connection for the producer.
			connection = cfg.newConnectionFactory().createConnection();
			connection.start();

			// Create a session.
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

			// Create a queue named with prefix "KC.EVENT".
			final Destination destination = session.createQueue(queueName);

			// Create a producer from the session to the queue.
			producer = session.createProducer(destination);
			producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

			// Create a message.
			final TextMessage message = session.createTextMessage(messageString);

			// Send the message.
			producer.send(message);
			log.debugf("keycloak-to-activemq SUCCESS sending message: %s%n", queueName);
		} catch (JMSException jmsex) {
			log.errorf(jmsex, "keycloak-to-activemq ERROR sending message: %s%n", queueName);
		} finally {
			// Clean up the producer.
			try {
				if (producer != null) producer.close();
			} catch (JMSException jmsex) {
				log.errorf(jmsex, "keycloak-to-activemq ERROR closing producer");
			}
			try {
				if (session != null) session.close();
			} catch (JMSException jmsex) {
				log.errorf(jmsex, "keycloak-to-activemq ERROR closing session");
			}
			try {
				if (connection != null) connection.close();
			} catch (JMSException jmsex) {
				log.errorf(jmsex, "keycloak-to-activemq ERROR closing connection");
			}
		}
	}

}