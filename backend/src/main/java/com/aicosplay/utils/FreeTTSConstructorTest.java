package com.aicosplay.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class FreeTTSConstructorTest {
    public static void main(String[] args) {
        try {
            // Get the class
            Class<?> singleFileAudioPlayerClass = Class.forName("com.sun.speech.freetts.audio.SingleFileAudioPlayer");
            
            System.out.println("SingleFileAudioPlayer class found.");
            
            // Print all constructors
            System.out.println("Available constructors:");
            Constructor<?>[] constructors = singleFileAudioPlayerClass.getConstructors();
            for (Constructor<?> constructor : constructors) {
                System.out.println(constructor.toString());
            }
            
            // Print all methods
            System.out.println("\nAvailable methods:");
            Method[] methods = singleFileAudioPlayerClass.getMethods();
            for (Method method : methods) {
                // Just print methods related to file or audio handling
                if (method.getName().contains("File") || method.getName().contains("audio") || 
                    method.getName().contains("getPath") || method.getName().contains("getName")) {
                    System.out.println(method.toString());
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println("SingleFileAudioPlayer class not found: " + e.getMessage());
        }
    }
}