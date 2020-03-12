package com.tuielectronics.aruco_demo;

public class UIEvent {
    public final String type;
    public final String topic;
    public final String payload1;
    public final String payload2;
    public UIEvent(String type, String topic, String payload1,String payload2) {
        this.type = type;
        this.topic = topic;
        this.payload1 = payload1;
        this.payload2 = payload2;
    }
}