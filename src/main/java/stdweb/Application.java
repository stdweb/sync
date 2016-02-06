package stdweb;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import stdweb.Core.Amount;
import stdweb.Core.GlobalRegistry;
import stdweb.Core.HashDecodeException;
import stdweb.Desktop.DesktopController;
import stdweb.Entity.AmountEntity;
import stdweb.ethereum.DbBean;
import stdweb.ethereum.LedgerSyncService;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import static java.lang.Runtime.getRuntime;

@SpringBootApplication
@EnableTransactionManagement
@EnableScheduling
@ComponentScan("stdweb")
public class Application //extends javafx.application.Application
{

    public static void main(String[] args) throws IOException {
        ConfigurableApplicationContext ctx = SpringApplication.run(Application.class, args);
//        String[] beanNames = ctx.getBeanDefinitionNames();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        GlobalRegistry reg=ctx.getBean(GlobalRegistry.class);

//        System.out.println("ethpool name :"+reg.getName("0x4bb96091ee9d802ed039c4d1a5f6216f90f81b01"));
//        System.out.println("nanopool name :"+reg.getName("0x52bc44d5378309ee2abf1539bf71de1b7d7be3b5"));


        LedgerSyncService bean = ctx.getBean(LedgerSyncService.class);
        bean.start();



//        DbBean dbBean=ctx.getBean(DbBean.class);
//        dbBean.deleteTopBlocksData(749842);
//        System.out.println(String.format("stdweb:FreeMemory %s, TotalMemory %s, 0.3 of totmem: %s",
//                getRuntime().freeMemory()/1024,getRuntime().totalMemory()/1024,getRuntime().totalMemory()/1024*0.3
//        ));
//
//        System.out.println("spring main finish");
       // launch(args);
    }


    //@Override
    public void start(Stage primaryStage) throws IOException, HashDecodeException {

        DesktopController controller=new DesktopController();

        FXMLLoader fxmlLoader = new FXMLLoader(Charset.defaultCharset());
        fxmlLoader.setController(controller);
        Parent root = (Parent) fxmlLoader.load(this.getClass().getClassLoader().getResource("mainPanel.fxml"));
        //Parent root = (Parent) FXMLLoader.load(this.getClass().getClassLoader().getResource("mainPanel.fxml"));

        primaryStage.setTitle("Hello World");
        primaryStage.setScene(new Scene(root, 800.0D, 600.0D));

        primaryStage.show();
    }
}
