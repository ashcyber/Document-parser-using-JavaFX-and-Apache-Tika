/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datauploader;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

// data extraction imports 
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.parser.microsoft.ooxml.OOXMLParser;
import org.apache.tika.parser.microsoft.OfficeParser;
import org.apache.tika.parser.rtf.RTFParser;
import org.xml.sax.SAXException;

// MySQL Database 
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.Statement;
import java.io.File;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.stage.FileChooser;
import org.apache.commons.lang.StringEscapeUtils;

public class FXMLDocumentController implements Initializable {

    private Service<Void> backgroundThread;

    @FXML
    private Button multi_file;
    
    @FXML 
    private Button clear_history;
    
    @FXML
    private TextField field;

    @FXML
    private ListView listView;

    @FXML
    private ProgressIndicator pi;
    
    @FXML
    private void clearHistoryAction(ActionEvent event){
        listView.getItems().clear();
    }
    
    @FXML
    private void multipleFileAction(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File("C:\\Users\\agsxpro\\Desktop"));
        List<File> selectedFiles = fc.showOpenMultipleDialog(null);        
 
        backgroundThread = new Service<Void>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        for (int i = 0; i < selectedFiles.size(); i++) {
                            dataExtraction fileData = new dataExtraction();
                            DBConnect fileCon = new DBConnect();
                            String content = fileData.getDataMulti(selectedFiles.get(i));
                            updateProgress(i + 1, selectedFiles.size());
                            String job_category = field.getText();
                            final String thisFileName = selectedFiles.get(i).getName();

                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    listView.getItems().add(thisFileName);
                                }
                            });
                            String fileName = selectedFiles.get(i).getName(); 
                            job_category = StringEscapeUtils.escapeSql(job_category); 
                            content = StringEscapeUtils.escapeSql(content);
                            fileName = StringEscapeUtils.escapeSql(fileName); 
                            fileCon.insertData(job_category, fileName, content);
                        }

                        return null;
                    }
                ;
            };
        }

    };
        backgroundThread.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                System.out.println("Done!");
            }
        });
        pi.progressProperty().bind(backgroundThread.progressProperty());
        backgroundThread.restart();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }

}

class dataExtraction {

    public String doc_to_text(String path) throws FileNotFoundException, IOException, SAXException, TikaException {

        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        FileInputStream inputstream = new FileInputStream(new File(path));
        ParseContext pcontext = new ParseContext();

        //parsing the document using OfficeParser
        OfficeParser msword = new OfficeParser();
        msword.parse(inputstream, handler, metadata, pcontext);
        String content = handler.toString();
        return content;
    }

    public String docx_to_text(String path) throws FileNotFoundException, IOException, SAXException, TikaException {
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        FileInputStream inputstream = new FileInputStream(new File(path));
        ParseContext pcontext = new ParseContext();

        //parsing the document using OOXMLParser
        OOXMLParser msofficeparser = new OOXMLParser();
        msofficeparser.parse(inputstream, handler, metadata, pcontext);

        String content = handler.toString();
        return content;
    }

    public String pdf_to_text(String path) throws FileNotFoundException, IOException, SAXException, TikaException {
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        FileInputStream inputstream = new FileInputStream(new File(path));
        ParseContext pcontext = new ParseContext();

        //parsing the document using PDF parser
        PDFParser pdfparser = new PDFParser();
        pdfparser.parse(inputstream, handler, metadata, pcontext);

        String content = handler.toString();
        return content;
    }

    public String rtf_to_text(String path) throws FileNotFoundException, IOException, SAXException, TikaException {
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        FileInputStream inputstream = new FileInputStream(new File(path));
        ParseContext pcontext = new ParseContext();

        //parsing the document using RTF parser
        RTFParser rtfparser = new RTFParser();
        rtfparser.parse(inputstream, handler, metadata, pcontext);

        String content = handler.toString();
        return content;
    }

    public String getExtension(String file) {
        //returns four types of extension of the file namely pdf,doc,docx and rtf
        String ext = "";
        if (file.endsWith("docx")) {
            ext = "docx";
        } else if (file.endsWith("doc")) {
            ext = "doc";
        } else if (file.endsWith("pdf")) {
            ext = "pdf";
        } else if (file.endsWith("rtf")) {
            ext = "rtf";
        } else {
            ext = "";
        }

        return ext;
    }

    public String getDataMulti(File file) {
        String text_resume = "";
        try {
            String fileName = file.getName();
            switch (getExtension(fileName)) {
                case "doc":
                    text_resume = doc_to_text(file.getPath());
                    break;
                case "docx":
                    text_resume = docx_to_text(file.getPath());
                    break;
                case "pdf":
                    text_resume = pdf_to_text(file.getPath());
                    break;
                case "rtf":
                    text_resume = rtf_to_text(file.getPath());
                    break;
                default:
                    break;
            }
        } catch (IOException ex) {
            Logger.getLogger(dataExtraction.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(dataExtraction.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TikaException ex) {
            Logger.getLogger(dataExtraction.class.getName()).log(Level.SEVERE, null, ex);
        }
        return text_resume;
    }
}

// class to handle database operation (MySQL with WAMPSERVER connection)
class DBConnect {

    private Connection con;
    private Statement st;
    private ResultSet rs;

    public DBConnect() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            con = (Connection) DriverManager.getConnection("jdbc:mysql://localhost:3306/scrape_data", "root", "");
            st = (Statement) con.createStatement();
        } catch (Exception ex) {
            System.out.println("Error: " + ex);
        }
    }

    public void getData() {
        try {
            String query = "SELECT * FROM gcs2";
            rs = st.executeQuery(query);
            System.out.println("Records from Database");
            while (rs.next()) {
                String data = rs.getString("file_resume");
                System.out.println("file_resume " + data);
            }

        } catch (Exception ex) {
            System.out.println("Error: " + ex);
        }
    }

    public void insertData(String job_category, String file_resume, String text_resume) {
        try {
            String query = String.format("INSERT INTO gcs2(job_category, file_resume, text_resume, date_modified) VALUES('%s','%s','%s',CURDATE())", job_category, file_resume, text_resume);
            st.executeUpdate(query);
            System.out.println(file_resume + " uploaded to the database");
        } catch (Exception ex) {
            System.out.println("Error: " + ex);
        }
        
        try { 
            con.close();
        } catch (SQLException ex) {
            Logger.getLogger(DBConnect.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
