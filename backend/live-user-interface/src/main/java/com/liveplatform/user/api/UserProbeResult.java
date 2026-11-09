package com.liveplatform.user.api;

import java.io.Serial;
import java.io.Serializable;

/** 可序列化的 RPC DTO；不暴露 JDBC 或 Spring 类型。 */
public class UserProbeResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String serviceName;
    private String database;
    private String databaseProduct;
    private String databaseVersion;
    private String probeValue;

    public UserProbeResult() { }

    public UserProbeResult(String serviceName, String database, String databaseProduct,
                           String databaseVersion, String probeValue) {
        this.serviceName = serviceName;
        this.database = database;
        this.databaseProduct = databaseProduct;
        this.databaseVersion = databaseVersion;
        this.probeValue = probeValue;
    }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }
    public String getDatabaseProduct() { return databaseProduct; }
    public void setDatabaseProduct(String databaseProduct) { this.databaseProduct = databaseProduct; }
    public String getDatabaseVersion() { return databaseVersion; }
    public void setDatabaseVersion(String databaseVersion) { this.databaseVersion = databaseVersion; }
    public String getProbeValue() { return probeValue; }
    public void setProbeValue(String probeValue) { this.probeValue = probeValue; }
}
