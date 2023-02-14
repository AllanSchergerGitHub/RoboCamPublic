package Utility;

import com.fazecast.jSerialComm.SerialPort;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.marineapi.nmea.event.AbstractSentenceListener;
import net.sf.marineapi.nmea.io.SentenceReader;
import net.sf.marineapi.nmea.sentence.GGASentence;
import net.sf.marineapi.nmea.sentence.GLLSentence;
import net.sf.marineapi.nmea.util.Position;
import net.sf.marineapi.nmea.util.GpsFixQuality;

public class GpsReader {
    public interface GpsListener {
        public void onRead(Position position, GpsFixQuality fixQuality);
    }
    
    InputStream mReadStream ;
    SentenceReader mSentenceReader;
    ArrayList<GpsListener> mGpsListeners = new ArrayList<>();
    
    public GpsReader() {}
    
    public void addListener(GpsListener listener) {
        mGpsListeners.add(listener);
    }
    
    public void fireListeners(Position position, GpsFixQuality fixQuality) {
        for(GpsListener listener: mGpsListeners) {
            listener.onRead(position, fixQuality);
        }
    }            
    
    public boolean connectToCommPort(String comPort) {
        for (SerialPort serialPort: SerialPort.getCommPorts()) {
            System.out.println(String.format(
                    "%s: %s", 
                    serialPort.toString(),  serialPort.getDescriptivePortName()
            ));
            if (serialPort.toString().contains(comPort)) {
                System.out.println("Found GPS device!");
                serialPort.openPort();
                serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
                mReadStream = serialPort.getInputStream();
                return true;
            }
        }
        return false;
    }
    
    public boolean connectToFile(String filePath) {
        try {
            mReadStream = new FileInputStream(new File(filePath));
        } catch (FileNotFoundException ex) {
            return false;
        }
        return true;
    }
    
    public void startReading() {
        if (mReadStream == null) return;
        mSentenceReader =  new SentenceReader(mReadStream);       
        mSentenceReader.addSentenceListener(new GLLListener());
        mSentenceReader.addSentenceListener(new GGAListener());
        mSentenceReader.start();
    }
    
    class GLLListener extends  AbstractSentenceListener<GLLSentence> {
        @Override
        public void sentenceRead(GLLSentence sent) {
            
        }
    }
    
    class GGAListener extends  AbstractSentenceListener<GGASentence> { // https://ktuukkan.github.io/marine-api/0.10.0/javadoc/net/sf/marineapi/nmea/sentence/GGASentence.html
        @Override
        public void sentenceRead(GGASentence sent) {
            fireListeners(sent.getPosition(), sent.getFixQuality());
            }
    }
}
