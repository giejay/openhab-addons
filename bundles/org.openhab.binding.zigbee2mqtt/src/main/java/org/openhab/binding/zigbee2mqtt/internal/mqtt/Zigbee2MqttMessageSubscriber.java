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
package org.openhab.binding.zigbee2mqtt.internal.mqtt;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.io.transport.mqtt.MqttMessageSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link Zigbee2MqttMessageSubscriber} class...TODO
 *
 * @author Nils - Initial contribution
 *
 */
public interface Zigbee2MqttMessageSubscriber extends MqttMessageSubscriber {

    final Logger logger = LoggerFactory.getLogger(Zigbee2MqttMessageSubscriber.class);

    @Override
    public default void processMessage(@NonNull String topic, byte @NonNull [] payload) {

        String message = new String(payload);
        logger.debug("incoming message for topic: {} -> {}", topic, message);

        JsonParser parser = new JsonParser();
        JsonElement msgJsonElement = null;
        JsonObject msgJsonObject = null;

        try {
            msgJsonElement = parser.parse(message);
        } catch (Exception e) {
            msgJsonElement = stringToJsonElement(message);
        }

        if (!msgJsonElement.isJsonObject()) {
            msgJsonObject = new JsonObject();
            msgJsonObject.add("message", msgJsonElement);
        } else {
            msgJsonObject = msgJsonElement.getAsJsonObject();
        }

        if (!msgJsonObject.isJsonNull()) {
            processMessage(topic, msgJsonObject);
        } else {
            logger.warn("no valid message for topic: {} -> {}", topic, message);
        }
    }

    /**
     * Replaces all special characters and creates a JsonPrimitiv with the formatted jsonString.
     *
     *
     * @param jsonString
     * @return
     */
    default JsonElement stringToJsonElement(@NonNull String jsonString) {
        JsonParser parser = new JsonParser();
        String formatted = jsonString;
        formatted = formatted.replaceAll("\"", "\\\\\"");
        formatted = formatted.replaceAll("\r\n", " ");
        formatted = formatted.replaceAll("\t", " ");
        formatted = "\"" + formatted + "\"";
        return parser.parse(formatted);
    }

    /**
     * @param topic
     * @param jsonMessage
     */
    abstract void processMessage(@NonNull String topic, @NonNull JsonObject jsonMessage);
}
