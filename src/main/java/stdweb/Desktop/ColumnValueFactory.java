package stdweb.Desktop;

import com.sun.javafx.property.PropertyReference;
import javafx.beans.NamedArg;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

/**
 * Created by bitledger on 17.11.15.
 */
public class ColumnValueFactory<S,T> implements Callback<TableColumn.CellDataFeatures<S,T>, ObservableValue<T>> {
    private final String property;

    private Class<?> columnClass;
    private String previousProperty;
    private PropertyReference<T> propertyRef;

    /**
     * Creates a default PropertyValueFactory to extract the value from a given
     * TableView row item reflectively, using the given property name.
     *
     * @param property The name of the property with which to attempt to
     *      reflectively extract a corresponding value for in a given object.
     */
    public ColumnValueFactory(@NamedArg("property") String property) {
        this.property = property;
    }

    /** {@inheritDoc} */
    @Override public ObservableValue<T> call(TableColumn.CellDataFeatures<S,T> param) {
        return getCellDataReflectively(param.getValue());
    }

    /**
     * Returns the property name provided in the constructor.
     */
    public final String getProperty() { return property; }

    private ObservableValue<T> getCellDataReflectively(S rowData) {
        if (getProperty() == null || getProperty().isEmpty() || rowData == null) return null;

        try {
            // we attempt to cache the property reference here, as otherwise
            // performance suffers when working in large data models. For
            // a bit of reference, refer to RT-13937.
            if (columnClass == null || previousProperty == null ||
                    ! columnClass.equals(rowData.getClass()) ||
                    ! previousProperty.equals(getProperty())) {

                // create a new PropertyReference
                this.columnClass = rowData.getClass();
                this.previousProperty = getProperty();
                this.propertyRef = new PropertyReference<T>(rowData.getClass(), getProperty());
            }

            if (propertyRef.hasProperty()) {
                return propertyRef.getProperty(rowData);
            } else {
                T value = propertyRef.get(rowData);
                return new ReadOnlyObjectWrapper<T>(value);
            }
        } catch (IllegalStateException e) {
            // log the warning and move on
//            final PlatformLogger logger = Logging.getControlsLogger();
//            if (logger.isLoggable(PlatformLogger.Level.WARNING)) {
//                logger.finest("Can not retrieve property '" + getProperty() +
//                        "' in PropertyValueFactory: " + this +
//                        " with provided class type: " + rowData.getClass(), e);
//            }
        }

        return null;
    }
}
