/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mySQL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import javax.swing.JOptionPane;

public class MySQL_Update {
    public int x;
public String MySQL_Update2(String mySQL_table, String mySQL_connection_string, String mySQL_username, String mySQL_password, String mySQL_schema, String updateCode) { 
String result2 = "dummy test 2";
  //  result = "";
{
     try
     {
         Class.forName("com.mysql.jdbc.Driver");
         //Connection con = DriverManager.getConnection("jdbc:mysql://ec2-52-7-14-229.compute-1.amazonaws.com:8081/mynewdatabase","test","password");
         Connection con = DriverManager.getConnection(mySQL_connection_string,mySQL_username,mySQL_password);         
         
         
         Statement stmt=(Statement)con.createStatement();

         //for (x=1; x<100; x++){
         float name=2;
         
         System.out.println("code: "+updateCode);
       //updateCode="update "+mySQL_table+" set layer1 = 1000 where columnA = 65;";  //INSERT INTO sample1(column1) values ('"+name+"');";
       stmt.executeUpdate(updateCode);
//
         //}
       // do i need to close the connection?
       // https://netbeans.org/competition/win-with-netbeans/mysql-client.html#db10

     }
     catch(Exception e)
     {
         JOptionPane.showMessageDialog(null, e.getMessage() ,"Error", 1);

     }
     System.out.println("test update done ");
 return result2;  // http://stackoverflow.com/questions/17405714/java-how-do-you-return-a-value-from-try-catch-and-finally

}
} 
}

//
//public class MySQL_Insert 
//{
//    private static int x;
// public static void main(String[] args )
// {
//     try
//     {
//         Class.forName("com.mysql.jdbc.Driver");
//         Connection con = DriverManager.getConnection("jdbc:mysql://ec2-52-7-14-229.compute-1.amazonaws.com:8081/mynewdatabase","test","password");
//         
//         Statement stmt=(Statement)con.createStatement();
//
//         for (x=1; x<100; x++){
//         float name=2;
//
//       String insert="INSERT INTO sample1(column1) values ('"+name+"');";
//       stmt.executeUpdate(insert);
//System.out.println("test insert ");
//         }
//       // do i need to close the connection?
//       // https://netbeans.org/competition/win-with-netbeans/mysql-client.html#db10
//
//     }
//     catch(Exception e)
//     {
//         JOptionPane.showMessageDialog(null, e.getMessage() ,"Error", 1);
//
//     }
// }
//} 