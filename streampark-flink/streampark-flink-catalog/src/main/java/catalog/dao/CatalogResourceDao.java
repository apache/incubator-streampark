package catalog.dao;

import java.util.List;
import java.util.Optional;

public interface CatalogResourceDao {
    List<Database> listDatabases(String catalogName);

}
