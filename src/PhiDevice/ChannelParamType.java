/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PhiDevice;

/**
 * @author sujoy
 */
public enum ChannelParamType {
    VELOCITY("Velocity"),
    POSITION("Position");

    private final String mName;

    ChannelParamType(String name) {
        mName = name;
    }

    @Override
    public String toString() {
        return mName;
    }
}
