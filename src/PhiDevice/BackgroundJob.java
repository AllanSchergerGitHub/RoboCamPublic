/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PhiDevice;

import javax.swing.*;
import java.util.ArrayList;


/**
 * @author sujoy
 */
public abstract class BackgroundJob {
    private static ArrayList<BackgroundJob> mJobs = new ArrayList<>();

    private final long mTimeOutPeriod;
    private final Worker mWorker;
    private boolean mFinished = false;

    private boolean mTimedOut = false;

    public BackgroundJob(long timeOutPeriod) {
        mJobs.add(this);
        mTimeOutPeriod = timeOutPeriod;
        mWorker = new Worker();
        mWorker.execute();
    }

    public abstract boolean isReady();

    public abstract void runInBackground();

    public void cleanUp() {
    }

    ;

    public void updateGUI() {
    }

    ;

    public void onTimeOut() {
    }

    ;

    private void removeThisJob() {
        cleanUp();
        mJobs.remove(this);
    }

    public class Worker extends SwingWorker<Void, Void> {
        @Override
        protected Void doInBackground() throws InterruptedException {
            long startedAt = System.currentTimeMillis();
            while (!isReady()) {
                if ((System.currentTimeMillis() - startedAt) > mTimeOutPeriod) {
                    mTimedOut = true;
                    break;
                }
                Thread.sleep(10);
            }
            if (mTimedOut) {
                onTimeOut();
                removeThisJob();
                mFinished = true;
            } else {
                runInBackground();
            }
            return null;
        }

        @Override
        protected void done() {
            updateGUI();
            if (!mFinished) removeThisJob();
        }
    }
}
