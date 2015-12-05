package stdweb.Desktop;/**
 * Created by bitledger on 15.11.15.
 */

import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainPanel //extends Application
{

    public static void main1(String[] args) {
        //launch(args);
    }
//
    public void createDesktop() throws IOException {
        JFXPanel panel = new JFXPanel();
        Parent root = (Parent) FXMLLoader.load(this.getClass().getClassLoader().getResource("mainPanel.fxml"));
        Stage primaryStage =new Stage();



        primaryStage.setTitle("Hello World");
        primaryStage.setScene(new Scene(root, 300.0D, 275.0D));
        primaryStage.show();
    }
    //@Override
    public void start1(Stage primaryStage) throws IOException {
        Parent root = (Parent) FXMLLoader.load(this.getClass().getResource("mainPanel.fxml"));
        primaryStage.setTitle("Hello World");
        primaryStage.setScene(new Scene(root, 300.0D, 275.0D));
        primaryStage.show();
    }
}
