/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.entityreborn.chservercommunication;

/**
 *
 * @author import
 */
public class Util {
    public static boolean isValidChannel(String chan) {
        if (chan.contains("\0")) {
            return false;
        } else if (chan.isEmpty()){
            return false;
        }
        
        return true;
    }
    
    public static boolean isValidName(String name) {
        if (name.contains("\0")) {
            return false;
        } else if (name.isEmpty()){
            return false;
        }
        
        return true;
    }
}