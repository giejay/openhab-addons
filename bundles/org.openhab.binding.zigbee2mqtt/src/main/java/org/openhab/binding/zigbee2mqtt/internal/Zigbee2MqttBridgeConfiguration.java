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

/**
 * The {@link Zigbee2MqttBridgeConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Nils - Initial contribution
 */
public class Zigbee2MqttBridgeConfiguration {

    private String mqttbrokerIpAddress;
    private Integer mqttbrokerPort;
    private String mqttbrokerUsername;
    private String mqttbrokerPassword;
    private String z2mBaseTopic;
    private String z2mDiscoveryTopic;

    public String getMqttbrokerIpAddress() {
        return mqttbrokerIpAddress;
    }

    public void setMqttbrokerIpAddress(String ipAddress) {
        this.mqttbrokerIpAddress = ipAddress;
    }

    public Integer getMqttbrokerPort() {
        return mqttbrokerPort;
    }

    public void setMqttbrokerPort(Integer port) {
        this.mqttbrokerPort = port;
    }

    public String getMqttbrokerUsername() {
        return mqttbrokerUsername;
    }

    public void setMqttbrokerUsername(String username) {
        this.mqttbrokerUsername = username;
    }

    public String getMqttbrokerPassword() {
        return mqttbrokerPassword;
    }

    public void setMqttbrokerPassword(String password) {
        this.mqttbrokerPassword = password;
    }

    public String getZ2mBaseTopic() {
        return z2mBaseTopic;
    }

    public void setZ2mBaseTopic(String z2mBaseTopic) {
        this.z2mBaseTopic = z2mBaseTopic;
    }

    public String getZ2mDiscoveryTopic() {
        return z2mDiscoveryTopic;
    }

    public void setZ2mDiscoveryTopic(String z2mDiscoveryTopic) {
        this.z2mDiscoveryTopic = z2mDiscoveryTopic;
    }

    // @Override
    // public String toString() {
    // return new ToStringBuilder(this).append("mqttbrokerIpAddress", this.getMqttbrokerIpAddress())
    // .append("mqttbrokerPort", this.getMqttbrokerPort())
    // .append("mqttbrokerBaseTopic", this.getMqttbrokerBaseTopic())
    // .append("mqttbrokerUsername", this.getMqttbrokerUsername()).append("mqttbrokerPassword", "*****")
    // .toString();
    // }

    @Override
    public String toString() {
        return "Zigbee2MqttBridgeConfiguration [mqttbrokerIpAddress=" + mqttbrokerIpAddress + ", mqttbrokerPort="
                + mqttbrokerPort + ", mqttbrokerUsername=" + mqttbrokerUsername + ", z2mBaseTopic=" + z2mBaseTopic
                + ", z2mDiscoveryTopic=" + z2mDiscoveryTopic + "]";
    }

}
