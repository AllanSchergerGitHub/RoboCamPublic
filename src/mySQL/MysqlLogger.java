package mySQL;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MysqlLogger {
    public enum Type {
        INSERT, INSER_TIMELAG, READ, UPDATE, BETTER, DUTYCYCLE
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public static void put(Type logType, Object... args) {
        EXECUTOR.submit(new Task(logType, args));
    }
    
    public static class Task implements Runnable {
        Type mLogType;
        Object[] mArgs;

        public Task(Type logType, Object[] args) {
            mLogType = logType;
            mArgs = args;
        }

        @Override
        public void run() {
            switch(mLogType) {
                case BETTER:
                    //System.out.println(String.format("MusqlLogging Better %s ", mArgs[1]));
                    MySQL_Better mysqlBetter = new MySQL_Better();
                    mysqlBetter.MySQL_BetterInsert(
                            (float)mArgs[0],
                            (String) mArgs[1],
                            (String) mArgs[2],
                            (String) mArgs[3],
                            (String) mArgs[4]
                    );
                    //System.out.println(String.format("MusqlLogging Insert AFTER %s ", mArgs[1])); 
                    break;
                case INSERT:
                    ///System.out.println(String.format("MusqlLogging Insert %s ", mArgs[1]));
                    MySQL_Insert mysqlInsert = new MySQL_Insert();
                    mysqlInsert.MySQL_Insert2(
                            (String)mArgs[0],
                            (String) mArgs[1],
                            (String) mArgs[2],
                            (String) mArgs[3]
                    );
                    break;
                case DUTYCYCLE:
                    //System.out.println("MusqlLogging Insert BEFORE %s "+ mArgs.length+" "+mArgs[15]+" "+mArgs[16]);
                    
                    // mysql coding is very specific about data types - double check to make sure they are all exactly right.
                    
                    MySQL_dutycycle_etc mysqlDutyCycle = new MySQL_dutycycle_etc();
                    mysqlDutyCycle.MySQL_dutycycle_etcInsert(
                            (float)mArgs[0],
                            (String) mArgs[1],
                            (String) mArgs[2],
                            (String) mArgs[3],
                            (String) mArgs[4],
                            (double) mArgs[5],
                            (double) mArgs[6],
                            (double) mArgs[7],
                            (double) mArgs[8],
                            (double) mArgs[9],
                            (double) mArgs[10],
                            (double) mArgs[11],
                            (double) mArgs[12],
                            (String) mArgs[13],
                            (double) mArgs[14],
                            (double) mArgs[15],
                            (double) mArgs[16]
                            
                    );
                    //System.out.println(String.format("MusqlLogging Insert AFTER %s ", mArgs[1]));       
                      
                    break;
            }
        }
    }
}