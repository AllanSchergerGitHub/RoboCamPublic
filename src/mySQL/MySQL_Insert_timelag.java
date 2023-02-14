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
import javax.swing.JOptionPane;

public class MySQL_Insert_timelag {
            private float position;
            private String device; 
            private String measureName;
            private static boolean connectedToMySQL = false;
            static String mySQL_connection_string = "jdbc:mysql://localhost:3306/test?useSSL=false";
            static String mySQL_username = "root"; //test_laptop";
            static String mySQL_password = "password";
            static Connection con;
            String Batch_time_stamp_into_mysql;// = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(new Date());

public String MySQL_Insert2(
        //String mySQL_table, 
        //String mySQL_connection_string, 
        //String mySQL_username, 
        //String mySQL_password, 
        //String mySQL_schema, 
                            float position, 
                            String measureName,
                            String Batch_time_stamp_into_mysql,
                            String device
            )  {
    
        String result2 = " success";
{
    this.position = position;
    this.device = device;
    this.measureName = measureName;
    this.Batch_time_stamp_into_mysql = Batch_time_stamp_into_mysql;
        
     try
     {
        String mySQL_connection_string = "jdbc:mysql://localhost:3306/test?useSSL=false";
        String mySQL_username = "root"; //test_laptop";
        String mySQL_password = "password";
        String mySQL_schema = "test";
        String mySQL_table = "timelag";
        
        Class.forName("com.mysql.jdbc.Driver");
      
        
//            if(connectedToMySQL){
//         }
//         else{
        con = DriverManager.getConnection(mySQL_connection_string, mySQL_username, mySQL_password);         
//         }

        //System.out.println(mySQL_connection_string);
         
        Statement stmt=(Statement)con.createStatement();

        String time_stamp_into_mysql = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(new Date());         

        //System.out.println(mySQL_table);
        
//       String insert="INSERT INTO sample1 (column1,timestamp,uniqueID) values ('"+name+"','12','"+x+"');";
       String insert="INSERT INTO "+mySQL_table +" (position,"
               + "timestamp,"
               + "measureName,"
               + "Batch_time_stamp,"
               + "device) values ('"+this.position+"',"
               + "'"+time_stamp_into_mysql+"',"
               + "'"+this.measureName+"',"
               + "'"+Batch_time_stamp_into_mysql+"',"
               + "'"+this.device+"');";
       
       //System.out.println(insert);
       
        stmt.executeUpdate(insert);

        
        
              // do i need to close the connection?
       // https://netbeans.org/competition/win-with-netbeans/mysql-client.html#db10
     
     }
     catch(Exception e)
     {
         JOptionPane.showMessageDialog(null, e.getMessage() ,"MySQL Error?", 1);

     }
     finally{
         /*This block should be added to your code
          * You need to release the resources like connections
          */
         if(con!=null)
          try {
              con.close();
         } catch (SQLException ex) {
             System.err.println("-----------------error here--------------------mysql");
             Logger.getLogger(MySQL_Insert_timelag.class.getName()).log(Level.SEVERE, null, ex);
         }
} 
 return result2;  // http://stackoverflow.com/questions/17405714/java-how-do-you-return-a-value-from-try-catch-and-finally

}
}
} 

