package org.niannarilli.keycloak.events;

import org.keycloak.Config.Scope;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import org.jboss.logging.Logger;


public class ActiveMqEventListenerProviderFactory implements EventListenerProviderFactory {

    private static final Logger log = Logger.getLogger(ActiveMqEventListenerProviderFactory.class);
    private ActiveMqConfig cfg;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
    	try {
	    	log.trace("create activemq provider");
	        return new ActiveMqEventListenerProvider(session, cfg);
    	} catch (Exception e) {
    		log.error("create activemq provider");
    		return null;
    	}
    }

    @Override
    public void init(Scope config) {
    	try {
	    	log.trace("init activemq provider");
	        cfg = ActiveMqConfig.createFromScope(config);
    	} catch (Exception e) {
    		log.error("init activemq provider");
    	}
    }
    
    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "keycloak-to-activemq";
    }
}
