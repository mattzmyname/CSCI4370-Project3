 
/*****************************************************************************************
 * @file  TestTupleGenerator.java
 *
 * @author   Sadiq Charaniya, John Miller
 */

import static java.lang.System.out;
import java.util.*;
import java.io.File;
import java.io.IOException;
import jxl.*;
import jxl.write.*;
import jxl.write.Boolean;
import jxl.write.Number;
import jxl.write.biff.RowsExceededException;


/*****************************************************************************************
 * This class tests the TupleGenerator on the Student Registration Database defined in the
 * Kifer, Bernstein and Lewis 2006 database textbook (see figure 3.6).  The primary keys
 * (see figure 3.6) and foreign keys (see example 3.2.2) are as given in the textbook.
 */
public class TestTupleGenerator
{
    /*************************************************************************************
     * The main method is the driver for TestGenerator.
     * @param args  the command-line arguments
     */
    public static void main (String [] args)
    {
        TupleGenerator test = new TupleGeneratorImpl ();
        Table Student = new Table ("Student", "id name address status", "Integer String String String", "id");
        test.addRelSchema ("Student",
                           "id name address status",
                           "Integer String String String",
                           "id",
                           null);
        Table Professor = new Table("Professor" , "id name deptID" , "Integer String String", "id");
        test.addRelSchema ("Professor",
                           "id name deptId",
                           "Integer String String",
                           "id",
                           null);
        Table Course = new Table ("Course", "crsCode deptID crsName descr", "String String String String", "crsCode");
        test.addRelSchema ("Course",
                           "crsCode deptId crsName descr",
                           "String String String String",
                           "crsCode",
                           null);
        Table Teaching = new Table ("Teaching", "crsCode semester profId", "String String Integer", "crsCode semester");
        test.addRelSchema ("Teaching",
                           "crsCode semester profId",
                           "String String Integer",
                           "crcCode semester",
                           new String [][] {{ "profId", "Professor", "id" },
                                            { "crsCode", "Course", "crsCode" }});
        Table Transcript = new Table ("Transcript", "studId crsCode semester grade", "Integer String String String", "studId crsCode semester");
        test.addRelSchema ("Transcript",
                           "studId crsCode semester grade",
                           "Integer String String String",
                           "studId crsCode semester",
                           new String [][] {{ "studId", "Student", "id"},
                                            { "crsCode", "Course", "crsCode" },
                                            { "crsCode semester", "Teaching", "crsCode semester" }});

        String [] tables = { "Student", "Professor", "Course", "Teaching", "Transcript" };
        
        int tups [] = new int [] { 10000, 1000, 2000, 50000, 5000 };
    
        System.out.println("Generating Tuples in resultTest...");
        Comparable [][][] resultTest = test.generate (tups);
        
      //Insert tuples from Student schema random generation ( 10000 )
        for (int j = 0; j < resultTest [0].length; j++) {
            
 

            Student.insert(resultTest[0][j]);
        } 
      //Insert tuples from Professor schema random generation ( 1000 )
        for (int j = 0; j < resultTest [1].length; j++) {
            
 

            Professor.insert(resultTest[1][j]);
        } 
        
      //Insert tuples from Course schema random generation ( 2000 )
        for (int j = 0; j < resultTest [2].length; j++) {
            
 

            Course.insert(resultTest[2][j]);
        } 
      //Insert tuples from Teaching schema random generation ( 50000 )
        for (int j = 0; j < resultTest [3].length; j++) {
            
 

            Teaching.insert(resultTest[3][j]);
        } 
        
      //Insert tuples from Transcript schema random generation ( 5000 )
        for (int j = 0; j < resultTest [4].length; j++) {
            

            Transcript.insert(resultTest[4][j]);
        } 
        
        for (int i = 0; i < resultTest.length; i++) {
            out.println (tables [i]);
            for (int j = 0; j < resultTest [i].length; j++) {
                for (int k = 0; k < resultTest [i][j].length; k++) {
                    out.print (resultTest [i][j][k] + ",");
                } // for
                out.println ();
            } // for
            out.println ();
        } // for
       
      //Excel File attempt
        try {
            File exlFile = new File("p3data.xls");
            WritableWorkbook writableWorkbook = Workbook
                    .createWorkbook(exlFile);
 
            WritableSheet worksheet = writableWorkbook.createSheet("Sheet1", 0);
      //Variable decs for time recording
        long startTime;
        long endTime;
        double duration;
        
        //Variable decs for random tuple selection
        int rowCounter=0;
        Random ran = new Random();

        System.out.println("\tStudent Select 10000 ");
        rowCounter++;
        Label select1 = new Label (rowCounter,0, "Student Select 10000");
        worksheet.addCell(select1);
       
        for( int i = 0; i < 50 ; i++){
            startTime = System.nanoTime();
            Student.select(t -> t[Student.col("id")].equals( resultTest[0][ran.nextInt(10000)][0].toString()));
            endTime = System.nanoTime();
            duration = (double)(endTime - startTime)/1000000000.0;
            System.out.println("\t\t" + duration + " Secs");
            Number num = new Number(rowCounter, 1+i, duration);
            worksheet.addCell(num);
        }//for
        
       
        System.out.println("\tProfessor Select 1000 ");
        rowCounter++;
        Label select2 = new Label (rowCounter,0, "Professor Select 1000 ");
        worksheet.addCell(select2);
        for( int i = 0; i < 50 ; i++){
            startTime = System.nanoTime();
            Professor.select(t -> t[Professor.col("id")].equals( resultTest[1][ran.nextInt(1000)][0].toString()));
            endTime = System.nanoTime();
            duration = (double)(endTime - startTime)/1000000000.0;
            System.out.println("\t\t" + duration + " Secs");
            Number num = new Number(rowCounter, 1+i, duration);
            worksheet.addCell(num);
        }//for
        
        System.out.println("\tCourse Select 2000 ");
        rowCounter++;
        Label select3 = new Label (rowCounter,0, "Course Select 2000 ");
        worksheet.addCell(select3);
        for( int i = 0; i < 50 ; i++){
            startTime = System.nanoTime();
            Course.select(t -> t[Course.col("crsCode")].equals( resultTest[2][ran.nextInt(1000)][0].toString()));
            endTime = System.nanoTime();
            duration = (double)(endTime - startTime)/1000000000.0;
            System.out.println("\t\t" + duration + " Secs");
            Number num = new Number(rowCounter, 1+i, duration);
            worksheet.addCell(num);
        }//for
        System.out.println("\tStudent join with Transcript - 10000 tuples + 50000 tuples");
        rowCounter++;
        Label join1 = new Label (rowCounter,0, "Student Join Transcript");
        worksheet.addCell(join1);
        for( int i = 0; i < 50 ; i++){
            startTime = System.nanoTime();
            Table tempTable = Student.join ("id",  "studId", Transcript);
            endTime = System.nanoTime();
            duration = (double)(endTime - startTime)/1000000000.0;
            System.out.println("\t\t" + (duration) + " Secs");
            Number num = new Number(rowCounter, 1+i, duration);
            worksheet.addCell(num);
        }//for
        System.out.println("\tStudent ijoin with Transcript - 10000 tuples + 50000 tuples");
        rowCounter++;
        Label join2 = new Label (rowCounter,0, "Student iJoin Transcript");
        worksheet.addCell(join2);
        for( int i = 0; i < 50 ; i++){
            startTime = System.nanoTime();
            Table tempTable = Student.i_join ("id",  "studId", Transcript);
            endTime = System.nanoTime();
            duration = (double)(endTime - startTime)/1000000000.0;
            System.out.println("\t\t" + (duration) + " Secs");
            Number num = new Number(rowCounter, 1+i, duration);
            worksheet.addCell(num);
        }//for
        
        writableWorkbook.write();
        writableWorkbook.close();
       }//try excel
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (RowsExceededException e) {
            e.printStackTrace();
        } catch (WriteException e) {
            e.printStackTrace();
        }
    } // main

} // TestTupleGenerator
