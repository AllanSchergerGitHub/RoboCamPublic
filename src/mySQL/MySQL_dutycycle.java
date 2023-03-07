/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.

//  https://dev.mysql.com/downloads/windows/installer/5.7.html
//  if the installer locks try this -> https://stackoverflow.com/questions/48412727/unable-to-install-mysql-on-windows-10-installer-hangs-indefinitely
//  it worked July 9 2018
 */
package mySQL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MySQL_dutycycle {
    private double dutycycle;
    private String measureName;
    private String Batch_time_stamp_into_mysql;// = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(new Date());
    private String device;
    private String sendingID;
    private double motor0dutycycle;
    private double motor1dutycycle;
    private double delta;
    private double ratio;
    private double ratioPreTrunc;
    private double dutyCycleRatioAvgShortArrayListsize;
    private double grandTotalNumInstances;
    private double ratioFactorToFixDutyCycle;
    private String recalcRatioFlag;
    private double mTargetPosition;
    private double getBLCDCPosAtIndex_0;
    private double getBLCDCPosAtIndex_1;

    private String mySQL_connection_string = "jdbc:mysql://localhost:3306/test?useSSL=false";
    private String mySQL_username = "root"; //test_laptop";
    private String mySQL_password = "password";
    private Connection con = null;
    private Statement stmt = null;
    private String mySQL_schema = "test";
    private String mySQL_table = "dutycycle";
    private String insert = "initialized value";

    public void MySQL_dutycycleInsert(
            double dutycycle,
            String measureName,
            String Batch_time_stamp_into_mysql,
            String device,
            String sendingID,
            double motor0dutycycle,
            double motor1dutycycle,
            double delta,
            double ratio,
            double ratioPreTrunc,
            double dutyCycleRatioAvgShortArrayListsize,
            double grandTotalNumInstances,
            double ratioFactorToFixDutyCycle,
            String recalcRatioFlag,
            double mTargetPosition,
            double getBLCDCPosAtIndex_0,
            double getBLCDCPosAtIndex_1
    ) {
        this.dutycycle = dutycycle;
        this.measureName = measureName;
        this.Batch_time_stamp_into_mysql = Batch_time_stamp_into_mysql;
        this.device = device;
        this.sendingID = sendingID;
        this.motor0dutycycle = motor0dutycycle;
        this.motor1dutycycle = motor1dutycycle;
        this.delta = delta;
        this.ratio = ratio;
        this.ratioPreTrunc = ratioPreTrunc;
        this.dutyCycleRatioAvgShortArrayListsize = dutyCycleRatioAvgShortArrayListsize;
        this.grandTotalNumInstances = grandTotalNumInstances;
        this.ratioFactorToFixDutyCycle = ratioFactorToFixDutyCycle;
        this.recalcRatioFlag = recalcRatioFlag;
        this.mTargetPosition = mTargetPosition;
        this.getBLCDCPosAtIndex_0 = getBLCDCPosAtIndex_0;
        this.getBLCDCPosAtIndex_1 = getBLCDCPosAtIndex_0;

        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(MySQL_dutycycle.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            con = DriverManager.getConnection(mySQL_connection_string, mySQL_username, mySQL_password);
        } catch (SQLException ex) {
            Logger.getLogger(MySQL_dutycycle.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            stmt = (Statement) con.createStatement();
        } catch (SQLException ex) {
            Logger.getLogger(MySQL_dutycycle.class.getName()).log(Level.SEVERE, null, ex);
        }

        String time_stamp_into_mysql = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(new Date());

        //System.out.println(mySQL_table);

        insert = "INSERT INTO " + mySQL_table + " (dutycycle,"
                + "timestamp,"
                + "measureName,"
                + "Batch_time_stamp,"
                + "device,"
                + "motor0dutycycle,"
                + "motor1dutycycle,"
                + "delta,"
                + "ratio,"
                + "ratioPreTrunc,"
                + "dutyCycleRatioAvgShortArrayListsize,"
                + "grandTotalNumInstances,"
                + "ratioFactorToFixDutyCycle,"
                + "this.recalcRatioFlag,"
                + "mTargetPosition,"
                + "getBLCDCPosAtIndex_0,"
                + "getBLCDCPosAtIndex_1"
                + ") values ('" + this.dutycycle + "',"
                + "'" + time_stamp_into_mysql + "',"
                + "'" + this.measureName + "',"
                + "'" + Batch_time_stamp_into_mysql + "',"
                + "'" + this.device + "',"
                + "'" + this.motor0dutycycle + "',"
                + "'" + this.motor1dutycycle + "',"
                + "'" + this.delta + "',"
                + "'" + this.ratio + "',"
                + "'" + this.ratioPreTrunc + "',"
                + "'" + this.dutyCycleRatioAvgShortArrayListsize + "',"
                + "'" + this.grandTotalNumInstances + "',"
                + "'" + this.ratioFactorToFixDutyCycle + "',"
                + "'" + this.recalcRatioFlag + "',"
                + "'" + this.mTargetPosition + "',"
                + "'" + this.getBLCDCPosAtIndex_0 + "',"
                + "'" + this.getBLCDCPosAtIndex_1 + "');";

        try {
            System.out.println(insert);
            //stmt.executeUpdate(insert);
            boolean executeSQLResponse = stmt.execute(insert);
        } catch (SQLException ex) {
            Logger.getLogger(MySQL_dutycycle.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            stmt.close();
            // do i need to close the connection?
            // https://netbeans.org/competition/win-with-netbeans/mysql-client.html#db10
        } catch (SQLException ex) {
            Logger.getLogger(MySQL_dutycycle.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            con.close();
        } catch (SQLException ex) {
            Logger.getLogger(MySQL_dutycycle.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    // http://stackoverflow.com/questions/17405714/java-how-do-you-return-a-value-from-try-catch-and-finally
}