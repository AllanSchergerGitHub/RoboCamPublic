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

public class MySQL_Insert {
    private float position;
    private String device;
    private String measureName;
    private boolean connectedToMySQL = false;
    static String mySQL_connection_string = "jdbc:mysql://localhost:3306/test?useSSL=false";
    static String mySQL_username = "root"; //test_laptop";
    static String mySQL_password = "password";
    static Connection con;
    String Batch_time_stamp_into_mysql;// = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(new Date());
    String sendingID;

    public String MySQL_Insert2(
            String measureName,
            String Batch_time_stamp_into_mysql,
            String device,
            String sendingID
    ) {

        String result2 = " success";
        String insert = "initialized value";
        Boolean executeSQLResponse = false;
        {
            this.position = position;
            this.device = device;
            this.measureName = measureName;
            this.Batch_time_stamp_into_mysql = Batch_time_stamp_into_mysql;
            this.sendingID = sendingID;

            String mySQL_connection_string = "jdbc:mysql://localhost:3306/test?useSSL=false";
            String mySQL_username = "root"; //test_laptop";
            String mySQL_password = "password";
            String mySQL_schema = "test";
            String mySQL_table = "phidget_position";

            try {
                Class.forName("com.mysql.jdbc.Driver");
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(MySQL_Insert.class.getName()).log(Level.SEVERE, null, ex);
            }


            try {
                con = DriverManager.getConnection(mySQL_connection_string, mySQL_username, mySQL_password);
            } catch (SQLException ex) {
                Logger.getLogger(MySQL_Insert.class.getName()).log(Level.SEVERE, null, ex);
            }


            Statement stmt = null;
            try {
                stmt = (Statement) con.createStatement();
            } catch (SQLException ex) {
                Logger.getLogger(MySQL_Insert.class.getName()).log(Level.SEVERE, null, ex);
            }

            String time_stamp_into_mysql = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(new Date());


//       String insert="INSERT INTO sample1 (column1,timestamp,uniqueID) values ('"+name+"','12','"+x+"');";
            insert = "INSERT INTO " + mySQL_table + " (position,"
                    + "timestamp,"
                    + "measureName,"
                    + "Batch_time_stamp,"
                    + "device) values ('" + this.position + "',"
                    + "'" + time_stamp_into_mysql + "',"
                    + "'" + this.measureName + "',"
                    + "'" + Batch_time_stamp_into_mysql + "',"
                    + "'" + this.device + "');";

            try {
                //System.out.println(insert);

                //stmt.executeUpdate(insert);

                executeSQLResponse = stmt.execute(insert);
            } catch (SQLException ex) {
                Logger.getLogger(MySQL_Insert.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                stmt.close();
                // do i need to close the connection?
                // https://netbeans.org/competition/win-with-netbeans/mysql-client.html#db10
            } catch (SQLException ex) {
                Logger.getLogger(MySQL_Insert.class.getName()).log(Level.SEVERE, null, ex);
            }

            try {
                con.close();
            } catch (SQLException ex) {
                Logger.getLogger(MySQL_Insert.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
//     catch(Exception e)
//     {
//         //JOptionPane.showMessageDialog(null, e.getMessage() ,"MySQL Error?", 1);
//         System.err.println(executeSQLResponse+" -----------------error here--------------------mysql_insert.java-------- "+sendingID+" "+insert+" ");
//         // Sept 22 2018 - this was showing 'socket closed' errors even though the insert statement was executing.
//     }
//     finally{
//         /*This block should be added to your code
//          * You need to release the resources like connections
//          */
//         if(con!=null)
//          try {
//              con.close();
//         } catch (SQLException ex) {
//             System.err.println("-----------------error here--------------------mysql");
//             Logger.getLogger(MySQL_Insert.class.getName()).log(Level.SEVERE, null, ex);
//         }
//} 
        return result2;  // http://stackoverflow.com/questions/17405714/java-how-do-you-return-a-value-from-try-catch-and-finally

    }
}
//} 

