package GamePad;

/**
 * In order to listen to different GamePad
 * controller events, an object needs to be created
 * from a class that implements this interface
 */
public interface GamePadControllerEvent {
    public void valueChanged(Object source, double value);
    
    public void updateUI();
}
