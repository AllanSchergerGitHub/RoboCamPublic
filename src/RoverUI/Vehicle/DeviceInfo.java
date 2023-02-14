package RoverUI.Vehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DeviceInfo {
    public static final String DUTY_CYCLE = "Duty cycle";
    private static ScheduledExecutorService sCommonExecutor = Executors.newScheduledThreadPool(1);
    
    private String mName;
    private Boolean mEngaged = null;
    private EngageStatus mEngageStatus = EngageStatus.NONE;
    private boolean mDoReEngage = false;
    private final HashMap<String, Object> mParams = new HashMap<>();
    
    public static interface ChangeListener {
        public void onEngage(DeviceInfo deviceInfo, boolean enaged);
        public void onParamChange(DeviceInfo deviceInfo, String paramName);
    }

    public static interface ChangeManager {
        public void onEngageRequest(DeviceInfo deviceInfo, boolean engaged);
    }
    
    private final ArrayList<ChangeListener> mChangeListeners = new ArrayList<>();
    private ChangeManager mChangeManager;
    
    public DeviceInfo(String name) {
        mName = name;
    }
    
    public void setName(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }
    
    public EngageStatus getEngageStatus() {
        return mEngageStatus;
    }
    
    public boolean getIsReEngaging() {
        return mDoReEngage;
    }
    
    public void setEngaged(boolean engaged) {
        mEngaged = engaged;
        mEngageStatus = engaged ? EngageStatus.ENGAGED : EngageStatus.DISENGAGED;

        if (mDoReEngage && mEngaged == true) {
            mDoReEngage = false;
        }
        for(ChangeListener listener: mChangeListeners) {
            listener.onEngage(this, engaged);
        }
        if (mDoReEngage && mEngaged == false) {
            sCommonExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    requestToSetEngaged(true);            
                }
            }, 2000, TimeUnit.MILLISECONDS);
        }
    }
    
    public void setChangeManager(ChangeManager manager) {
        mChangeManager = manager;
    }
    
    public void requestToSetEngaged(boolean engaged) {
        mEngageStatus = engaged ? EngageStatus.ENGAGING : EngageStatus.DISENGAGING;
        if (mChangeManager != null) {
            mChangeManager.onEngageRequest(this, engaged);
        }
    }
    
    public void requestToReEngage() {
        mEngageStatus = EngageStatus.REENGAGING;
        mDoReEngage = true;
        requestToSetEngaged(false);
    }

    public Boolean getEngaged() {
        return mEngaged;
    }

    public void setParam(String paramName, Object paramValue) {
        mParams.put(paramName, paramValue);
        for(ChangeListener listener: mChangeListeners) {
            listener.onParamChange(this, paramName);
        }
    }
    
    public Object getParam(String paramName, Object defaultValue) {
        Object value = mParams.get(paramName);
        return value == null ? defaultValue : value;
    }
    
    public String getParamsInfoString() {
        StringBuilder stringBuilder = new StringBuilder();
        int index = 0;
        for(Map.Entry<String, Object> entry: mParams.entrySet()) {
            if (entry.getValue() == null) continue;
            if (index > 0) {
                stringBuilder.append("\n");
            }
            stringBuilder.append(String.format("%s: %s", entry.getKey(), entry.getValue().toString()));
            index++;
        }
        return stringBuilder.toString();
    }
    
    public void addChangeListener(ChangeListener listener) {
        mChangeListeners.add(listener);
    }
}
