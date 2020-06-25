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

/**
 * The {@link DiscoveryTopic} class...TODO
 *
 * @author Nils - Initial contribution
 *
 */
public class DiscoveryTopic {

    private String topic = null;
    private String ieeeAddr = null;
    private String objectId = null;
    private String type = null;

    public DiscoveryTopic(String topic) {

        super();
        this.topic = topic;

        // example: homeassistant/sensor/0x00158d0002320b4f/battery/config
        String[] topicParts = topic.split("/");
        this.ieeeAddr = topicParts[2];
        this.objectId = topicParts[3];
        this.type = topicParts[1];
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getIeeeAddr() {
        return ieeeAddr;
    }

    public String getObjectId() {
        return objectId;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "TopicHomeassistant [ieeeAddr=" + getIeeeAddr() + ", topic=" + getTopic() + ", objectId=" + objectId
                + ", type=" + type + "]";
    }
}
