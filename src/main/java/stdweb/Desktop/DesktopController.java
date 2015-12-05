package stdweb.Desktop;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.beans.factory.annotation.Autowired;
import stdweb.Core.HashDecodeException;
import stdweb.Core.Sha3Hash;
import stdweb.Core.Utils;
import stdweb.Entity.LedgerBlock;
import stdweb.Repository.LedgerBlockRepository;

import java.net.URL;
import java.util.ResourceBundle;


public class DesktopController implements Initializable {

    @Autowired
    LedgerBlockRepository blockRepository;

    public void populateData() throws HashDecodeException {
        ObservableList<DataClass> data= FXCollections.observableArrayList();
        DataClass dataClass = new DataClass();
        dataClass.setId("id1");
        dataClass.setDescr("descr1");
        dataClass.setHash(new Sha3Hash(Utils.hash_decode("0xd9e7e9e2338348d85ad0c61b936620700897a60df0ad06e76c66366c19708682")));
        data.add(dataClass);

        dataClass = new DataClass();
        dataClass.setId("id2");
        dataClass.setDescr("ddd2");
        data.add(dataClass);

        dataClass = new DataClass();
        dataClass.setId("id3");
        dataClass.setDescr("ddddd3");
        data.add(dataClass);

        //tabv.setItems(data);
    }

    @FXML private TableView<LedgerBlock> tabv;
    @FXML private TableColumn<LedgerBlock,String> colId;
    @FXML private TableColumn<LedgerBlock,String> colHash;
    @FXML private TableColumn<LedgerBlock,String> colTxCount;
    @FXML private TableColumn<LedgerBlock,String> colBlockSize;

    public DesktopController() throws HashDecodeException {
        //this.tabv=getTableView();
    }
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("Desktop controller");

            colId.setCellValueFactory(new PropertyValueFactory<LedgerBlock,String>("id"));
            colHash.setCellValueFactory(new PropertyValueFactory<LedgerBlock,String>("hash"));
            colTxCount.setCellValueFactory(new PropertyValueFactory<LedgerBlock,String>("txCount"));
            colBlockSize.setCellValueFactory(new PropertyValueFactory<LedgerBlock,String>("blockSize"));
//            //this.tabv=getTableView();
//
//
            //Page<LedgerBlock> all = blockRepository.findAll(new PageRequest(1, 100));
//
//            ObservableList<LedgerBlock> list = FXCollections.observableArrayList(all.getContent());
//            this.tabv.setItems(list);


    }
}
