package catalog;

import org.apache.flink.table.catalog.Catalog;
import org.apache.flink.table.catalog.CatalogBaseTable;
import org.apache.flink.table.catalog.CatalogDatabase;
import org.apache.flink.table.catalog.CatalogFunction;
import org.apache.flink.table.catalog.CatalogPartition;
import org.apache.flink.table.catalog.CatalogPartitionSpec;
import org.apache.flink.table.catalog.ObjectPath;
import org.apache.flink.table.catalog.exceptions.CatalogException;
import org.apache.flink.table.catalog.exceptions.DatabaseAlreadyExistException;
import org.apache.flink.table.catalog.exceptions.DatabaseNotEmptyException;
import org.apache.flink.table.catalog.exceptions.DatabaseNotExistException;
import org.apache.flink.table.catalog.exceptions.FunctionAlreadyExistException;
import org.apache.flink.table.catalog.exceptions.FunctionNotExistException;
import org.apache.flink.table.catalog.exceptions.PartitionAlreadyExistsException;
import org.apache.flink.table.catalog.exceptions.PartitionNotExistException;
import org.apache.flink.table.catalog.exceptions.PartitionSpecInvalidException;
import org.apache.flink.table.catalog.exceptions.TableAlreadyExistException;
import org.apache.flink.table.catalog.exceptions.TableNotExistException;
import org.apache.flink.table.catalog.exceptions.TableNotPartitionedException;
import org.apache.flink.table.catalog.exceptions.TablePartitionedException;
import org.apache.flink.table.catalog.stats.CatalogColumnStatistics;
import org.apache.flink.table.catalog.stats.CatalogTableStatistics;
import org.apache.flink.table.expressions.Expression;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class StreamParkCatalog implements Catalog {
    private final String catalogName;
    private final Map<String, String> options;
    private final CatalogResourceDao dao;
    private final TableEnvClassLoader tEnvClassLoader;

    private final ConnectorClassLoaderProvider connectorClassLoaderProvider;

    private final UdfClassLoaderProvider udfClassLoaderProvider;

    public StreamParkCatalog(String catalogName, Map<String, String> options, CatalogResourceDao dao, TableEnvClassLoader tEnvClassLoader, @Nullable ConnectorClassLoaderProvider connectorClassLoaderProvider, @Nullable UdfClassLoaderProvider udfClassLoaderProvider) {
        Objects.requireNonNull(catalogName);
        Objects.requireNonNull(options);
        Objects.requireNonNull(dao);
        Objects.requireNonNull(tEnvClassLoader);
        if (!options.containsKey("defaultDatabase")) {
            throw new CatalogException(String.format("Catalog `%s` is missing required property '%s'.", catalogName.getResourceId(), "defaultDatabase"));
        } else if (((String)options.get("defaultDatabase")).isBlank()) {
            throw new CatalogException(String.format("'%s' property of catalog `%s` must not be empty or blank.", "defaultDatabase", catalogName.getResourceId()));
        } else {
            this.catalogName = catalogName;
            this.options = options;
            this.dao = dao;
            this.tEnvClassLoader = tEnvClassLoader;
            this.connectorClassLoaderProvider = connectorClassLoaderProvider;
            this.udfClassLoaderProvider = udfClassLoaderProvider;
            if (udfClassLoaderProvider != null) {
                this.tEnvClassLoader.addClassLoader(udfClassLoaderProvider.createUdfClassLoader(catalogName.getNamespace()));
            }

            this.createDefaultDatabase();
        }
    }


    @Override
    public void open() throws CatalogException {

    }

    @Override
    public void close() throws CatalogException {

    }

    @Override
    public String getDefaultDatabase() throws CatalogException {
        return this.options.get("defaultDatabase");

    }

    @Override
    public List<String> listDatabases() throws CatalogException {
        return Optional.of(new ClassloaderFunctionDefinitionFactory(this.tEnvClassLoader));
    }

    @Override
    public CatalogDatabase getDatabase(String s) throws DatabaseNotExistException, CatalogException {
        return null;
    }

    @Override
    public boolean databaseExists(String s) throws CatalogException {
        return false;
    }

    @Override
    public void createDatabase(String s, CatalogDatabase catalogDatabase, boolean b) throws DatabaseAlreadyExistException, CatalogException {

    }

    @Override
    public void dropDatabase(String s, boolean b, boolean b1) throws DatabaseNotExistException, DatabaseNotEmptyException, CatalogException {

    }

    @Override
    public void alterDatabase(String s, CatalogDatabase catalogDatabase, boolean b) throws DatabaseNotExistException, CatalogException {

    }

    @Override
    public List<String> listTables(String s) throws DatabaseNotExistException, CatalogException {
        return null;
    }

    @Override
    public List<String> listViews(String s) throws DatabaseNotExistException, CatalogException {
        return null;
    }

    @Override
    public CatalogBaseTable getTable(ObjectPath objectPath) throws TableNotExistException, CatalogException {
        return null;
    }

    @Override
    public boolean tableExists(ObjectPath objectPath) throws CatalogException {
        return false;
    }

    @Override
    public void dropTable(ObjectPath objectPath, boolean b) throws TableNotExistException, CatalogException {

    }

    @Override
    public void renameTable(ObjectPath objectPath, String s, boolean b) throws TableNotExistException, TableAlreadyExistException, CatalogException {

    }

    @Override
    public void createTable(ObjectPath objectPath, CatalogBaseTable catalogBaseTable, boolean b) throws TableAlreadyExistException, DatabaseNotExistException, CatalogException {

    }

    @Override
    public void alterTable(ObjectPath objectPath, CatalogBaseTable catalogBaseTable, boolean b) throws TableNotExistException, CatalogException {

    }

    @Override
    public List<CatalogPartitionSpec> listPartitions(ObjectPath objectPath) throws TableNotExistException, TableNotPartitionedException, CatalogException {
        return null;
    }

    @Override
    public List<CatalogPartitionSpec> listPartitions(ObjectPath objectPath, CatalogPartitionSpec catalogPartitionSpec) throws TableNotExistException, TableNotPartitionedException, PartitionSpecInvalidException, CatalogException {
        return null;
    }

    @Override
    public List<CatalogPartitionSpec> listPartitionsByFilter(ObjectPath objectPath, List<Expression> list) throws TableNotExistException, TableNotPartitionedException, CatalogException {
        return null;
    }

    @Override
    public CatalogPartition getPartition(ObjectPath objectPath, CatalogPartitionSpec catalogPartitionSpec) throws PartitionNotExistException, CatalogException {
        return null;
    }

    @Override
    public boolean partitionExists(ObjectPath objectPath, CatalogPartitionSpec catalogPartitionSpec) throws CatalogException {
        return false;
    }

    @Override
    public void createPartition(ObjectPath objectPath, CatalogPartitionSpec catalogPartitionSpec, CatalogPartition catalogPartition, boolean b) throws TableNotExistException, TableNotPartitionedException, PartitionSpecInvalidException, PartitionAlreadyExistsException, CatalogException {

    }

    @Override
    public void dropPartition(ObjectPath objectPath, CatalogPartitionSpec catalogPartitionSpec, boolean b) throws PartitionNotExistException, CatalogException {

    }

    @Override
    public void alterPartition(ObjectPath objectPath, CatalogPartitionSpec catalogPartitionSpec, CatalogPartition catalogPartition, boolean b) throws PartitionNotExistException, CatalogException {

    }

    @Override
    public List<String> listFunctions(String s) throws DatabaseNotExistException, CatalogException {
        return null;
    }

    @Override
    public CatalogFunction getFunction(ObjectPath objectPath) throws FunctionNotExistException, CatalogException {
        return null;
    }

    @Override
    public boolean functionExists(ObjectPath objectPath) throws CatalogException {
        return false;
    }

    @Override
    public void createFunction(ObjectPath objectPath, CatalogFunction catalogFunction, boolean b) throws FunctionAlreadyExistException, DatabaseNotExistException, CatalogException {

    }

    @Override
    public void alterFunction(ObjectPath objectPath, CatalogFunction catalogFunction, boolean b) throws FunctionNotExistException, CatalogException {

    }

    @Override
    public void dropFunction(ObjectPath objectPath, boolean b) throws FunctionNotExistException, CatalogException {

    }

    @Override
    public CatalogTableStatistics getTableStatistics(ObjectPath objectPath) throws TableNotExistException, CatalogException {
        return null;
    }

    @Override
    public CatalogColumnStatistics getTableColumnStatistics(ObjectPath objectPath) throws TableNotExistException, CatalogException {
        return null;
    }

    @Override
    public CatalogTableStatistics getPartitionStatistics(ObjectPath objectPath, CatalogPartitionSpec catalogPartitionSpec) throws PartitionNotExistException, CatalogException {
        return null;
    }

    @Override
    public CatalogColumnStatistics getPartitionColumnStatistics(ObjectPath objectPath, CatalogPartitionSpec catalogPartitionSpec) throws PartitionNotExistException, CatalogException {
        return null;
    }

    @Override
    public void alterTableStatistics(ObjectPath objectPath, CatalogTableStatistics catalogTableStatistics, boolean b) throws TableNotExistException, CatalogException {

    }

    @Override
    public void alterTableColumnStatistics(ObjectPath objectPath, CatalogColumnStatistics catalogColumnStatistics, boolean b) throws TableNotExistException, CatalogException, TablePartitionedException {

    }

    @Override
    public void alterPartitionStatistics(ObjectPath objectPath, CatalogPartitionSpec catalogPartitionSpec, CatalogTableStatistics catalogTableStatistics, boolean b) throws PartitionNotExistException, CatalogException {

    }

    @Override
    public void alterPartitionColumnStatistics(ObjectPath objectPath, CatalogPartitionSpec catalogPartitionSpec, CatalogColumnStatistics catalogColumnStatistics, boolean b) throws PartitionNotExistException, CatalogException {

    }
}
