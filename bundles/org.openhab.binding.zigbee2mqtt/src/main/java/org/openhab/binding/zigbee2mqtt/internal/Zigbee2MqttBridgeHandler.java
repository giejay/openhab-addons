/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.zigbee2mqtt.internal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
//import guru.nidi.graphviz.attribute.validate.ValidatorEngine;
//import guru.nidi.graphviz.attribute.validate.ValidatorFormat;
//import guru.nidi.graphviz.engine.Format;
//import guru.nidi.graphviz.engine.Graphviz;
//import guru.nidi.graphviz.model.MutableGraph;
//import guru.nidi.graphviz.parse.Parser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.RawType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.io.transport.mqtt.MqttBrokerConnection;
import org.eclipse.smarthome.io.transport.mqtt.MqttConnectionObserver;
import org.eclipse.smarthome.io.transport.mqtt.MqttConnectionState;
import org.openhab.binding.zigbee2mqtt.internal.discovery.Zigbee2MqttDiscoveryService;
import org.openhab.binding.zigbee2mqtt.internal.mqtt.Zigbee2MqttMessageSubscriber;
import org.openhab.binding.zigbee2mqtt.internal.mqtt.Zigbee2MqttTopicHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.openhab.binding.zigbee2mqtt.internal.Zigbee2MqttBindingConstants.*;

/**
 * The {@link Zigbee2MqttBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Nils - Initial contribution
 */
public class Zigbee2MqttBridgeHandler extends BaseBridgeHandler
        implements Zigbee2MqttMessageSubscriber, MqttConnectionObserver {

    private final Logger logger = LoggerFactory.getLogger(Zigbee2MqttBridgeHandler.class);

    private MqttBrokerConnection mqttBrokerConnection;

//    private Parser parser = new Parser().forEngine(ValidatorEngine.DOT).forFormat(ValidatorFormat.MAP);

    @Nullable
    private Zigbee2MqttDiscoveryService discoveryService;

    @NonNull
    private Zigbee2MqttBridgeConfiguration config = new Zigbee2MqttBridgeConfiguration();

    @NonNull
    private Zigbee2MqttTopicHandler topicHandler = new Zigbee2MqttTopicHandler();

    public Zigbee2MqttBridgeHandler(Bridge thing) {
        super(thing);
    }

    @Override
    public void initialize() {

        try {

            config = getConfigAs(Zigbee2MqttBridgeConfiguration.class);

            topicHandler.setBaseTopic(config.getZ2mBaseTopic());
            topicHandler.setDiscoveryTopic(config.getZ2mDiscoveryTopic());

            mqttBrokerConnection = createBrokerConnection(config);
            mqttBrokerConnection.addConnectionObserver(this);

            if (!mqttBrokerConnection.start().get().booleanValue()) {

                logger.error("Cannot connect to MQTT broker: {}", config.toString());
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Cannot connect to MQTT broker");
            }

        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error while initializing bridge", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }

    }

    public void setDiscoveryService(Zigbee2MqttDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @Override
    public void dispose() {

        try {

            mqttBrokerConnection.stop().get().booleanValue();
            super.dispose();

        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error in stopping", e);
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    /**
     * Creates a broker connection based on the configuration of {@link #config}.
     *
     * @param config
     *
     * @return Returns a valid MqttBrokerConnection
     * @throws IllegalArgumentException If the configuration is invalid, this exception is thrown.
     */
    protected MqttBrokerConnection createBrokerConnection(@NonNull Zigbee2MqttBridgeConfiguration config)
            throws IllegalArgumentException {

        String host = config.getMqttbrokerIpAddress();
        if (StringUtils.isBlank(host) || host == null) {
            throw new IllegalArgumentException("MqttbrokerIpAddress is empty!");
        }

        MqttBrokerConnection c = new MqttBrokerConnection(host, config.getMqttbrokerPort(), false,
                CLIENTIDPRAEFIX + getThing().getUID().getId());

        String username = config.getMqttbrokerUsername();
        String password = config.getMqttbrokerPassword();
        if (StringUtils.isNotBlank(username) && password != null) {
            c.setCredentials(username, password); // Empty passwords are allowed
        }

        return c;
    }

    @Override
    public void updateStatus(ThingStatus status) {
        super.updateStatus(status);

        if (ThingStatus.ONLINE.equals(status)) {
            if (discoveryService != null) {
                discoveryService.discover();
            }
        }
    }

    public void setDiscovery(Zigbee2MqttDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        if (command instanceof RefreshType) {
            switch (channelUID.getId()) {
                case CHANNEL_NAME_NETWORKMAP:
                  logger.info("Publishing to get new map");
                    publish(topicHandler.getTopicBridgeNetworkmap(), "graphviz");
                    return;

                default:
                    return;
            }
        }

        switch (channelUID.getId()) {
            case CHANNEL_NAME_PERMITJOIN:
                String permitjoin = OnOffType.ON.toString().equals(command.toString()) ? "true" : "false";
                publish(topicHandler.getTopicBridgePermitjoin(), permitjoin);
                break;

            case CHANNEL_NAME_LOGLEVEL:
                String loglevel = command.toString();
                publish(topicHandler.getTopicBridgeLoglevel(), loglevel);
                break;

            default:
                logger.debug("command for ChannelUID not supported: {}", channelUID.getAsString());
                break;
        }

    }

    @Override
    public void processMessage(@NonNull String topic, @NonNull JsonObject jsonMessage) {

        String action = topicHandler.getActionFromTopic(topic);

        switch (action) {
            case "networkmap/graphviz":
                handleActionNetworkmap(jsonMessage.get("message").getAsString());
                break;

            case "state":
                ThingStatus status = ThingStatus.valueOf(jsonMessage.get("message").getAsString().toUpperCase());
                updateStatus(status);
                break;

            case "config":
                handleConfigValues(jsonMessage);
                break;

            case "log":
                String type = jsonMessage.get("type").getAsString();
                JsonElement message = jsonMessage.get("message");
                if (message.isJsonPrimitive()) {

                    handleActionLog(type, message.getAsString());
                }

                break;

            default:
                break;
        }

    }

    /**
     * @return
     */
    public @NonNull Zigbee2MqttTopicHandler getTopicHandler() {

        return topicHandler;
    }

    @Override
    public void connectionStateChanged(MqttConnectionState state, @Nullable Throwable error) {

        logger.debug("Broker connection changed to: {}", state.toString());

        switch (state.toString()) {
            case "DISCONNECTED":
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "disconnected from broker");
                break;
            case "CONNECTION":
                updateStatus(ThingStatus.UNKNOWN, ThingStatusDetail.NONE, "connecting to broker");
                break;
            case "CONNECTED":
                updateStatus(ThingStatus.UNKNOWN, ThingStatusDetail.NONE, "connected to broker");
                subscribeTopics();
                break;

            default:
                break;
        }
    }

    /**
     * Add new message consumers to all topics.
     */
    private void subscribeTopics() {

        subscribe(topicHandler.getTopicBridgeState(), this);
        subscribe(topicHandler.getTopicBridgeConfig(), this);
        subscribe(topicHandler.getTopicBridgeLog(), this);
        subscribe(topicHandler.getTopicBridgeNetworkmapGraphviz(), this);

    }

    /**
     * Add a new message consumer.
     *
     * @param topic      The topic to subscribe to.
     * @param subscriber The callback listener for received messages for the given topic.
     * @return Completes with true if successful. Completes with false if not connected yet. Exceptionally otherwise.
     */
    public CompletableFuture<Boolean> subscribe(String topic, Zigbee2MqttMessageSubscriber subscriber) {
        logger.debug("subsribe to topic -> {}", topic);
        return mqttBrokerConnection.subscribe(topic, subscriber);
    }

    /**
     * Publish a message to the broker.
     *
     * @param topic   The topic
     * @param message The message
     * @return Returns a future that completes with a result of true if the publishing succeeded and completes
     *         exceptionally on an error or with a result of false if no broker connection is established.
     */
    public CompletableFuture<Boolean> publish(String topic, String message) {
        logger.debug("publish messeage to topic -> {}", topic);
        return mqttBrokerConnection.publish(topic, message.getBytes());
    }

    /**
     * Remove a previously registered.
     *
     * @param topic      The topic to unsubscribe from.
     * @param subscriber The callback listener to remove.
     * @return Completes with true if successful. Exceptionally otherwise.
     */
    public CompletableFuture<Boolean> unsubscribe(String topic, Zigbee2MqttMessageSubscriber subscriber) {
        logger.debug("unsubsribe from topic -> {}", topic);
        return mqttBrokerConnection.unsubscribe(topic, subscriber);
    }

    /**
     * Creates an image from networkmap message.
     *
     * @param message
     * @throws IOException
     */
    private void handleActionNetworkmap(String message) {
       try {
         logger.info("Getting network map " + message);

           FileUtils.writeStringToFile(new File("/tmp/graph.dot"), message);
           Runtime.getRuntime().exec("/usr/bin/dot /tmp/graph.dot  -Tpng -o /openhab/conf/html/graph.png");
//           final byte[] bytes = IOUtils.toByteArray(new FileReader("/openhab/conf/html/graph.png"));

//            Channel channelImage = getThing().getChannel(CHANNEL_NAME_NETWORKMAP);
//            if (channelImage != null) {
//              logger.info("Writing channel map");
//                updateState(channelImage.getUID(), new RawType(bytes, "image/png"));
//            }

        } catch (IOException e) {

            logger.error("error while rendering networkmap: {}", e.getMessage());
        }
    }

    /**
     * Handles the log message:
     * - device_connected
     * - zigbee_publish_error
     * - device_removed
     * - device_banned
     *
     * @param type
     * @param message
     */
    private void handleActionLog(String type, String message) {
        switch (type) {

            case "device_connected":
                logger.info("log message - type={}, message={}", type, message);
                updateDeviceStatus(message.replaceAll("\r\n", ""), ThingStatus.ONLINE,
                        "device paired again to controller");
                if (discoveryService != null) {
                    discoveryService.discover();
                }
                break;

            case "zigbee_publish_error":
                logger.error("log message - type={}, message={}", type, message);
                break;

            case "device_removed":
            case "device_banned":
                logger.warn("log message - type={}, message={}", type, message);
                updateDeviceStatus(message.replaceAll("\r\n", ""), ThingStatus.OFFLINE,
                        "device removed from controller");
                break;

            default:
                logger.info("log message - type={}, message={}", type, message);
                break;
        }
    }

    /**
     * Handles the log message:
     * - log_level
     * - permit_join
     * - add all other jsonvalues to the bridge properties
     *
     * @param jsonMessage
     */
    private void handleConfigValues(JsonObject jsonMessage) {

        Map<@NonNull String, @NonNull String> props = new HashMap<String, String>();

        for (Entry<String, JsonElement> entry : jsonMessage.entrySet()) {
            String entryKey = entry.getKey();
            JsonElement entryValue = entry.getValue();

            logger.debug("Property received: key={}, value={}", entryKey, entryValue.getAsString());

            switch (entryKey) {
                case "log_level":
                    Channel channelLogLevel = getThing().getChannel(CHANNEL_NAME_LOGLEVEL);
                    if (channelLogLevel != null) {
                        updateState(channelLogLevel.getUID(), StringType.valueOf(entryValue.getAsString()));
                    }
                    break;

                case "permit_join":
                    Channel channelPermitJoin = getThing().getChannel(CHANNEL_NAME_PERMITJOIN);
                    if (channelPermitJoin != null) {
                        updateState(channelPermitJoin.getUID(),
                                entryValue.getAsBoolean() ? OnOffType.ON : OnOffType.OFF);
                    }
                    break;

                default:
                    props.put(entryKey, entryValue.getAsString());
                    break;
            }
        }

        updateThing(editThing().withProperties(props).build());
    }

    /**
     * Search the thing by given ieeeAddr and updates the status.
     *
     * @param ieeeAddr
     * @param thingStatus
     * @param description
     */
    private void updateDeviceStatus(String ieeeAddr, ThingStatus thingStatus, String description) {

        Thing thing = getThingByUID(new ThingUID(THING_TYPE_DEVICE, getThing().getUID(), ieeeAddr));
        if (thing != null) {
            Zigbee2MqttDeviceHandler handler = (Zigbee2MqttDeviceHandler) thing.getHandler();
            if (handler != null) {
                handler.updateStatus(thingStatus, ThingStatusDetail.NONE, description);
            }
        }
    }

}
