package RoboCam;

public class IPCamSetting {
    private String mName;
    private String mIpAddress;
    private String mPictureUrl;
    private String mTemplateUrl;

    public IPCamSetting(String name, Config config) {
        mName = name;
        mIpAddress = config.getIPCamAddress(name);
        mPictureUrl = config.getIPCamUrl(name);
        mTemplateUrl = config.getIPCamUrl(name, "EXTRA");
    }

    public String getIpAddress() {
        return mIpAddress;
    }

    public String getPictureUrl() {
        return  mPictureUrl;
    }
}
