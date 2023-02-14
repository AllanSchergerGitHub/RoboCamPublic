/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mySQL;

import static java.lang.Integer.min;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import javax.swing.JOptionPane;

public class MySQL_Read {
    public String result;

    private static PreparedStatement pst;
    private static ResultSet rs;

public String[][] MySQL_Read2(String mySQL_table, String mySQL_connection_string, String mySQL_username, String mySQL_password, String mySQL_schema) { 

    int list_size_limit = 500;
    String[][] result2 = new String[40][list_size_limit]; // 40 is the number of different buckets created for the group by command - somewhat optional / arbitrary
 
     try
     {
       //Class.forName("com.mysql.jdbc.Driver");
       //Connection con = DriverManager.getConnection("jdbc:mysql://ec2-52-7-14-229.compute-1.amazonaws.com:8081/test","test","password");
        Connection con = DriverManager.getConnection(mySQL_connection_string,mySQL_username,mySQL_password);
       // http://www.tutorialspoint.com/jdbc/viewing-result-sets.htm
       // pst = con.prepareStatement("count column1  FROM sample1 ORDER BY column1 asc;");
    //   pst = con.prepareStatement("select layer1 as list FROM "+mySQL_table+" where columnA='65';");//where left(HITId,2)=39
    // this works   pst = con.prepareStatement("select  columnB, sum(layer1) as sumlist, count(*) as list FROM "+mySQL_table+" where columnA='125';");//where left(HITId,2)=39   sum(layer1) as sulist,


            int row_num = 0;
            result2[0][0] = "sumlist";
            result2[1][0] = "list";
            result2[2][0] = "columnB";
                
        pst = con.prepareStatement("select "+result2[2][0]+", sum(layer1) as sumlist, count(*) as list FROM "+mySQL_table+" group by columnB;");//where left(HITId,2)=39   sum(layer1) as sulist,
       
              System.out.println("starting query:");
              
        rs = pst.executeQuery();  
    
              System.out.println("done with query");
        
            ArrayList<String> list= new ArrayList<String>();
            ArrayList<String> list2= new ArrayList<String>();
            ArrayList<String> list3= new ArrayList<String>();
            String[] TestArray = new String[60000];
           
            

            while (rs.next()) {
                // this section loops over every row retrieved and concatenates the results together iteratively
                list.add(rs.getString(result2[0][0]));
                list2.add(rs.getString(result2[1][0]));
                list3.add(rs.getString(result2[2][0]));
                
                //System.out.println(row_num);
                ////System.out.println(list);
                //TestArray[row_num] = ""+list;
                //System.out.println("list size "+list.size());
                //System.out.println(TestArray[row_num]);
                row_num++;
            }  
            
                //http://stackoverflow.com/questions/14935016/convert-a-result-set-from-sql-array-to-array-of-strings
                //            Array a = rs.getArray("columnB");
                //            String[] nullable = (String[])a.getArray();
                //System.out.println("array "+nullable[0]);
            if(list_size_limit<list.size()){System.out.println("warning - only first "+list_size_limit+" records will be used in java out of "+list.size()+" available.");}
            System.out.println("list.size "+list.size());
            
                System.out.println(result2[0][0]+" "+list);
                System.out.println(result2[1][0]+" "+list2);
                System.out.println(result2[2][0]+" "+list3);

                //System.out.println(TestArray[3]);
                
                list_size_limit = min(list_size_limit, list.size());
                
                for (int x = 1; x < list_size_limit;){            
                String[] result = new String[list.size()];
                result = list.toArray(result);
                result2[0][x] = result[x-1];

                String[] xyz = new String[list2.size()];
                xyz = list2.toArray(xyz);
                result2[1][x] = xyz[x-1];

                String[] xyz2 = new String[list3.size()];
                xyz2 = list3.toArray(xyz2);
                result2[2][x] = xyz2[x-1];
            
                //for (stop_loop=0;stop_loop < 1; stop_loop+=1){ 
                //
                //String[] rollup = new String[list.size()];
                //rollup = list.toArray(rollup);
                //result2[x] = rollup[x]; 
            x++;
            };
//        }
       // do i need to close the connection?
       // https://netbeans.org/competition/win-with-netbeans/mysql-client.html#db10

     }
     catch(Exception e)
     {
         JOptionPane.showMessageDialog(null, e.getMessage() ,"Error in MySQL_Read.java ", 1);
     }
 
 return result2;

 // http://stackoverflow.com/questions/17405714/java-how-do-you-return-a-value-from-try-catch-and-finally
    }
}
 