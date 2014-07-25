/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package app;

import com.itextpdf.text.Anchor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfAction;
import com.itextpdf.text.pdf.PdfAnnotation;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author trietnm
 */
public class App {
    public String INPUT;
    public String OUTPUT;
    public String DATABASE;
    
    /**
     * @param args the command line arguments
     * @throws java.lang.ClassNotFoundException
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     * @throws com.itextpdf.text.DocumentException
     */
    public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException, DocumentException {
        if(args != null && args.length < 3){
            System.out.println("Miss Input Parameters");
            return;
        }
            
        String strInput  = args[0];
        String strDB     = args[1];
        String strOutput = args[2];
        
        App app = new App(strInput, strOutput, strDB);
        app.clonePDF();
    }

    private App(String strInput, String strOutput, String strDB) {
        this.INPUT    = strInput;
        this.OUTPUT   = strOutput;
        this.DATABASE = strDB;
    }

    private void clonePDF() throws DocumentException, FileNotFoundException, IOException, ClassNotFoundException {
        PdfReader reader = new PdfReader(INPUT);
        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(OUTPUT));
        stamper.setRotateContents(false);
        int n = reader.getNumberOfPages();

        for (int i = 0; i < n; i++) {
            setLink(i, stamper);
        }
        stamper.close();
        reader.close();
    }

    private void setLink(int pageId, PdfStamper stamper) throws ClassNotFoundException, IOException {
        // load the sqlite-JDBC driver using the current class loader
        Class.forName("org.sqlite.JDBC");

        Connection connection = null;
        try
        {
            // create a database connection
            connection = DriverManager.getConnection("jdbc:sqlite:"+ DATABASE);
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);  // set timeout to 30 sec.

            ResultSet rs = statement.executeQuery(
                "select * from page_annotations "
                + "where annotation_type = 'goto' and page_id = " + pageId);
            float spendX = 0;
            float spendY = 0;
            float coordX = 0;
            float coordY = 0;
            int pageDest = 0;
            Rectangle rect;
            PdfAnnotation annotation;
            while(rs.next())
            {
                spendX   = rs.getFloat("pdf_width");
                spendY   = rs.getFloat("pdf_height");
                coordX   = rs.getFloat("pdf_x");// + rs.getFloat("pdf_width");
                coordY   = rs.getFloat("pdf_y");
                pageDest = rs.getInt("annotation_target");

                Font font = FontFactory.getFont(FontFactory.COURIER, 10, Font.UNDERLINE);
                // Blue
                font.setColor(0, 0, 255);
                Chunk chunk = new Chunk("GOTO", font);
                Anchor anchor = new Anchor(chunk);
                ColumnText.showTextAligned(stamper.getUnderContent(pageId), Element.ALIGN_LEFT, anchor, coordX+spendX, coordY, 0);
                
                rect = new Rectangle(coordX, coordY, spendX, spendY);
                annotation = PdfAnnotation.createLink(
                    stamper.getWriter(), rect, PdfAnnotation.HIGHLIGHT_INVERT,
                    new PdfAction("#" + pageDest));
                stamper.addAnnotation(annotation, pageId);
            }
        }
        catch(SQLException e)
        {
            // if the error message is "out of memory", 
            // it probably means no database file is found
            System.err.println(e.getMessage());
        }
        finally
        {
            try
            {
                if(connection != null)
                    connection.close();
            }
            catch(SQLException e)
            {
                // connection close failed.
                System.err.println(e);
            }
        }
    }
    
}
