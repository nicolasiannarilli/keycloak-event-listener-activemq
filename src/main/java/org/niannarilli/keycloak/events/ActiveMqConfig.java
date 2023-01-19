package org.niannarilli.keycloak.events;


import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;

import org.keycloak.Config.Scope;
import org.keycloak.events.Event;
import org.keycloak.events.admin.AdminEvent;

import org.keycloak.util.JsonSerialization;

import org.jboss.logging.Logger;

public class ActiveMqConfig {

	private static final Logger log = Logger.getLogger(ActiveMqConfig.class);
	public static final String QUEUE_NAME_PREFIX = "KC.EVENT";
	private static final Pattern SPECIAL_CHARACTERS = Pattern.compile("[^*#a-zA-Z0-9 _.-]");
	private static final Pattern SPACE = Pattern.compile(" ");
	private static final Pattern DOT = Pattern.compile("\\.");

	private String hostUrl;
	private Integer port;
	private String username;
	private String password;

	private boolean filterSuccess;
	private Optional<List<String>> filterClient;
	private Optional<List<String>> filterResource;
	
	public String queueName(String realmId, String resourceType, boolean isAdmin) {
		String queueName = QUEUE_NAME_PREFIX
			+ (isAdmin ? ".ADMIN" : ".CLIENT")
			+ "." + removeDots(realmId)
			+ "." + removeDots(resourceType)
			;
		return normalizeKey(queueName);
	}

	//Remove all characters apart a-z, A-Z, 0-9, space, underscore, eplace all spaces and hyphens with underscore
	public static String normalizeKey(CharSequence stringToNormalize) {
		return SPACE.matcher(SPECIAL_CHARACTERS.matcher(stringToNormalize).replaceAll(""))
				.replaceAll("_");
	}
	
	public static String removeDots(String stringToNormalize) {
		if(stringToNormalize != null) {
			return DOT.matcher(stringToNormalize).replaceAll("");
		}
		return stringToNormalize;
	}
	
	public String writeAsJson(Object object, boolean isPretty) {
		try {
			if(isPretty) {
				return JsonSerialization.writeValueAsPrettyString(object);
			}
			return JsonSerialization.writeValueAsString(object);

		} catch (Exception e) {
			log.error("Could not serialize to JSON", e);
		}
		return "unparsable";
	}
	
	
	public static ActiveMqConfig createFromScope(Scope config) {
		ActiveMqConfig cfg = new ActiveMqConfig();
		cfg.hostUrl = resolveConfigVar(config, "url", "activemq.localhost");
		cfg.port = Integer.valueOf(resolveConfigVar(config, "port", "61616"));
		cfg.username = resolveConfigVar(config, "username", "admin");
		cfg.password = resolveConfigVar(config, "password", "admin");
		
		cfg.filterSuccess = Boolean.valueOf(resolveConfigVar(config, "filter_success", "true"));
		cfg.filterClient = toList(resolveConfigVar(config, "filter_client", "*"));
		cfg.filterResource = toList(resolveConfigVar(config, "filter_resource", "*"));
		return cfg;	
	}
	
	private static Optional<List<String>> toList(String resolveConfigVar) {
		if ("*".equals(resolveConfigVar)) {
			return Optional.empty();
		}
		return Optional.of(Arrays.asList(resolveConfigVar.split(";")));
	}

	private static String resolveConfigVar(Scope config, String variableName, String defaultValue) {
		String value = defaultValue;
		if(config != null && config.get(variableName) != null) {
			value = config.get(variableName);
		} else {
			//try from env variables eg: KK_TO_AMQ_URL:
			String envVariableName = "KC_TO_AMQ_" + variableName.toUpperCase(Locale.ENGLISH);
			String env = System.getenv(envVariableName);
			if(env != null) {
				value = env;
			}
		}
		if (!"password".equals(variableName)) {
			log.infof("keycloak-to-activemq configuration: %s=%s%n", variableName, value);
		}
		return value;
		
	}

	public boolean isPublish(AdminEvent adminEvent) {
		return (!filterSuccess || adminEvent.getError() == null) // error null = success
			&& filterResource
				.map(lr -> lr.contains(adminEvent.getResourceTypeAsString()))
				.orElse(true) // case "*"
		;
	}

    public boolean isPublish(Event event) {
        return (!filterSuccess || event.getError() == null) // error null = success
			&& filterClient
				.map(lc -> lc.contains(event.getClientId()))
				.orElse(true) // case "*"
		;
    }

	public ConnectionFactory newConnectionFactory() {
        // Create a connection factory.
        final ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(username, password, "tcp://" + hostUrl + ":" + port);
        connectionFactory.setTrustedPackages(Arrays.asList("org.efalia.keycloak.events.model"));
        return connectionFactory;
    }
}
