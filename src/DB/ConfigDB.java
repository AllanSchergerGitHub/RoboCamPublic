package DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.sqlite.SQLiteException;

public class ConfigDB {
    private static final String PARAM_SELECT_QUERY =
        "SELECT value, value_type FROM parameters  WHERE name = '%s';";
    private static final String PARAM_INSERT_QUERY =
        "INSERT OR IGNORE INTO parameters " +
            "(value, value_type, name) " +
            "VALUES ('%s', '%s', '%s');";
    private static final String PARAM_UPDATE_QUERY =
        "UPDATE parameters SET value"
            + " = '%s', value_type='%s' " +
            "WHERE name = '%s';";
    Connection mConnection;
    ArrayList<ParamValueListener> mParamValueListeners = new ArrayList<>();
    HashMap<String, String> mCachedValues = new HashMap<>();
    private final ReentrantLock mOpLock = new ReentrantLock();

    public ConfigDB(String filePath) throws SQLException {
        mConnection =  DriverManager.getConnection(
                    String.format("jdbc:sqlite:%s", filePath));
        if (mConnection == null) return;
        createTables();
    }

    public void addParamValueListener(ParamValueListener listener) {
        mParamValueListeners.add(listener);
    }

    private void createTables() throws SQLException {
        try (Statement statement = mConnection.createStatement()) {
            //statement.executeUpdate(
            //    "DROP TABLE IF EXISTS parameters;");
            String sql = "CREATE TABLE IF NOT EXISTS parameters (" +
                    "   name  String PRIMARY KEY NOT NULL, " +
                    "   value String NOT NULL, " +
                    "   value_type VARCHAR(50) " +
                    ")";
            statement.executeUpdate(sql);
        }
    }

    public double getValue(String paramName, double defaultValue) {
        double finalValue = defaultValue;
        synchronized(mOpLock) {
            String value = mCachedValues.get(paramName);
            if (value == null) {
                try {
                    Statement statement;
                    statement = mConnection.createStatement();
                    String sql = String.format(PARAM_SELECT_QUERY, paramName);
                    ResultSet resultSet;
                    resultSet = statement.executeQuery(sql);
                    if (resultSet.next()) {
                        value = resultSet.getString("value");
                    }
                    resultSet.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConfigDB.class.getName()).log(Level.SEVERE, null, ex);
                    value =  null;
                }
            }
            if (value != null && !value.isEmpty()) {
                mCachedValues.put(paramName, value);
                try {
                    finalValue = Double.parseDouble(value);
                } catch (NumberFormatException ex) {
                    finalValue = defaultValue;
                }
            }
        }
        return finalValue;
    }

    public int getValue(String paramName, int defaultValue) {
        int finalValue = defaultValue;
        synchronized(mOpLock) {
            String value = mCachedValues.get(paramName);
            if (value == null) {
                try {
                    Statement statement;
                    statement = mConnection.createStatement();
                    String sql = String.format(PARAM_SELECT_QUERY, paramName);
                    ResultSet resultSet;
                    resultSet = statement.executeQuery(sql);
                    if (resultSet.next()) {
                        value = resultSet.getString("value");
                    }
                    resultSet.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConfigDB.class.getName()).log(Level.SEVERE, null, ex);
                    return defaultValue;
                }
            }
            if (value != null && !value.isEmpty()) {
                mCachedValues.put(paramName, value);
                try {
                    finalValue = Integer.parseInt(value);
                } catch (NumberFormatException ex) {
                    finalValue = defaultValue;
                }
            }
        }
        return finalValue;
    }

    public String getValue(String paramName, String defaultValue) {
        String finalValue = defaultValue;
        synchronized(mOpLock) {
            String value = mCachedValues.get(paramName);
            if (value == null) {
                try {
                    Statement statement;
                    statement = mConnection.createStatement();
                    String sql = String.format(PARAM_SELECT_QUERY, paramName);
                    ResultSet resultSet = null;
                    try{
                    resultSet = statement.executeQuery(sql);
                    }
                    catch( SQLiteException e) {
                         System.err.println("SQLiteException - check into this if it keeps happening");
                    }
                    if (resultSet.next()) {
                        value = resultSet.getString("value");
                    }
                    resultSet.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConfigDB.class.getName()).log(Level.SEVERE, null, ex);
                    return defaultValue;
                }
            }
            if (value != null) {
                mCachedValues.put(paramName, value);
                finalValue = value;
                //System.out.println("getValue String from db: "+finalValue + " for " + paramName);
            }
        }
        return finalValue;
    }

    public void setValue(String paramName, Object value) {
        if (value == null) return;
        synchronized(mOpLock) {
            String valueType = value.getClass().getSimpleName();
            setValue(paramName, value, valueType);
            //System.out.println("setValue2 param value: "+value);
        }
    }

    public void setValue(String paramName, Object value, String valueType) {
        synchronized(mOpLock) {
            try {
                Statement statement;
                statement = mConnection.createStatement();
                String sql = String.format(PARAM_SELECT_QUERY, paramName);
                statement.executeUpdate(
                        String.format(PARAM_INSERT_QUERY, value, valueType, paramName));
                statement.executeUpdate(
                        String.format(PARAM_UPDATE_QUERY, value, valueType, paramName));
            } catch (SQLException ex) {
                Logger.getLogger(ConfigDB.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
            for (ParamValueListener listener: mParamValueListeners) {
                listener.onUpdate(paramName);
                System.out.println("paramNameSetValue: "+paramName);
                }
            mCachedValues.put(paramName, value.toString());
        }
    }
}
