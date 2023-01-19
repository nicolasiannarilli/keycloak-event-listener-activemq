# keycloak-event-listener-activemq

##### A Keycloak SPI plugin that publishes events to a ActiveMq server.  

| Plugin | min Keycloak ver |
| -- | ---- |
| 0.x | 19.x |

For example here is the notification of the user updated by administrator

* published to queue name : `KC.EVENT.ADMIN.MYREALM.USER`
* content: 


```
{
  "@class" : "org.niannarilli.keycloak.events.model.EventAdminNotificationMqMsg",
  "time" : 1674124056092,
  "realmId" : "MYREALM",
  "authDetails" : {
    "realmId" : "master",
    "clientId" : "********-****-****-****-**********",
    "userId" : "********-****-****-****-**********",
    "ipAddress" : "192.168.1.1"
  },
  "resourceType" : "USER",
  "operationType" : "UPDATE",
  "resourcePath" : "users/********-****-****-****-**********",
  "representation" : "representation details here....",
}
```

The queue name is calculated as follows:
* admin events: `KC.EVENT.ADMIN.<REALM>.<RESOURCE>`
* client events: `KC.EVENT.CLIENT.<REALM>.<CLIENT>`

## USAGE:
1. [Download the latest jar](https://github.com/niannarilli/keycloak-event-listener-activemq/blob/target/keycloak-to-rabbit-0.1.0.jar?raw=true) or build from source: ``mvn clean install``
2. Copy jar into your Keycloak version 19+ (Quarkus) `/opt/keycloak/providers/keycloak-to-activemq-0.1.0.jar` 
3. Configure as described below
4. Restart the Keycloak server
5. Enable logging in Keycloak UI by adding **keycloak-to-activemq**  
 `Manage > Events > Config > Events Config > Event Listeners`

#### Configuration 
###### just configure **ENVIRONMENT VARIABLES**
  - `KC_TO_AMQ_URL` - default: *localhost*
  - `KC_TO_AMQ_PORT` - default: *61616*
  - `KC_TO_AMQ_USERNAME` - default: *admin*
  - `KC_TO_AMQ_PASSWORD` - default: *admin*
  - `KC_TO_AMQ_FILTER_SUCCESS` - default: *true*
  - `KC_TO_AMQ_FILTER_CLIENT` - default: *\**
  - `KC_TO_AMQ_FILTER_RESOURCE` - default: *\**
		