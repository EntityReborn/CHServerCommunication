/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.entityreborn.chservercommunication;

/**
 *
 * @author import
 */
public class Exceptions {
    public static class InvalidNameException extends Exception {
        public InvalidNameException(String message) {
            super(message);
        }
    }
    public static class InvalidChannelException extends Exception {
        public InvalidChannelException(String message) {
            super(message);
        }
    }
}
