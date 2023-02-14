package PhiDevice;

import java.util.ArrayList;
import java.util.EnumSet;

public enum ChannelType {
    DC_MOTOR("DC Motor Controller", EnumSet.of(ChannelParamType.VELOCITY)),
    ENCODER("Encoder Input", EnumSet.of(ChannelParamType.POSITION)),
    STEPPER("Bipolar Stepper Controller", EnumSet.of(ChannelParamType.POSITION));

    String mChannelName;
    EnumSet<ChannelParamType> mParamTypes;

    ChannelType (String channelName, EnumSet<ChannelParamType> paramTypes) {
        mChannelName = channelName;
        mParamTypes = paramTypes;
    }

    public String[] getParamNames() {
        Object[] array = mParamTypes.toArray();
        String[] names = new String[array.length];
        for (int i = 0; i < array.length; i++) {
            names[i] = ((ChannelParamType) array[i]).toString();
        }
        return names;
    }

    public ChannelParamType getChannelParamTypeByName(String channelParamTypeName) {
        for(ChannelParamType cpType: mParamTypes) {
            if (cpType.toString().equals(channelParamTypeName)) {
                return cpType;
            }
        }
        return null;
    }

    public static ChannelType getChannelTypeByName(String channelName) {
        for(ChannelType channelType: ChannelType.values()) {
            if (channelType.mChannelName.equals(channelName)) return channelType;
        }
        return null;
    }
}
