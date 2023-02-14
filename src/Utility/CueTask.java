package Utility;

import java.util.concurrent.TimeUnit;

/**
* This abstract class is to hold
* task that can be executed later with time-out limitation
* and also based on a ready-condition.
*/
public abstract class CueTask {
   protected long mTimeOutMillisecond = TimeUnit.SECONDS.toMillis(1);
   private final long mStartedAt;

   public CueTask() {
       mStartedAt = System.currentTimeMillis();
   }

   public abstract boolean isReady();
   public abstract void run();

   public boolean isTimedOut() {
       return (System.currentTimeMillis()-mStartedAt) > mTimeOutMillisecond;
   }
}
