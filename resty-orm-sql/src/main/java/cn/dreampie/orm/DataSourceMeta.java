package cn.dreampie.orm;

import cn.dreampie.log.Logger;
import cn.dreampie.orm.dialect.Dialect;
import cn.dreampie.orm.provider.DataSourceProvider;
import com.alibaba.druid.pool.DruidDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static cn.dreampie.common.util.Checker.checkNotNull;

/**
 * ConnectionAccess
 */
public class DataSourceMeta {

  private static final Logger logger = Logger.getLogger(DataSourceMeta.class);
  //不能使用static 让每个数据源都有一个connectionTL
  private final ThreadLocal<Connection> connectionTL = new ThreadLocal<Connection>();
  private boolean showSql;
  private String dsName;
  private DataSource dataSource;
  private Dialect dialect;

  public DataSourceMeta(DataSourceProvider dataSourceProvider) {
    this(dataSourceProvider.getDsName(), dataSourceProvider.getDataSource(), dataSourceProvider.getDialect(), false);
  }

  public DataSourceMeta(DataSourceProvider dataSourceProvider, boolean showSql) {
    this(dataSourceProvider.getDsName(), dataSourceProvider.getDataSource(), dataSourceProvider.getDialect(), showSql);
  }

  public DataSourceMeta(String dsName, DataSource dataSource, Dialect dialect) {
    this(dsName, dataSource, dialect, false);
  }

  public DataSourceMeta(String dsName, DataSource dataSource, Dialect dialect, boolean showSql) {
    this.dsName = checkNotNull(dsName);
    this.dataSource = checkNotNull(dataSource);
    this.dialect = checkNotNull(dialect);
    this.showSql = showSql;
  }

  public String getDsName() {
    return dsName;
  }

  public DataSource getDataSource() {
    return dataSource;
  }

  public Dialect getDialect() {
    return dialect;
  }

  public Connection getConnection() throws SQLException {
    Connection conn = connectionTL.get();
    if (conn != null)
      return conn;
    return dataSource.getConnection();
  }

  public boolean isShowSql() {
    return showSql;
  }

  public Connection getCurrentConnection() {
    return connectionTL.get();
  }

  public void setCurrentConnection(Connection connection) {
    connectionTL.set(connection);
  }

  public void rmCurrentConnection() {
    connectionTL.remove();
  }

  public final void close() {
    if (dataSource != null && dataSource instanceof DruidDataSource)
      ((DruidDataSource) dataSource).close();
  }

  /**
   * Close ResultSet、Statement、Connection
   * ThreadLocal support declare transaction.
   */
  public final void close(ResultSet rs, Statement st, Connection conn) {
    if (rs != null) {
      try {
        rs.close();
      } catch (SQLException e) {
        logger.warn("Could not close resultSet!", e);
      }
    }
    //关闭连接
    close(st, conn);
  }

  public final void close(Statement st, Connection conn) {
    if (st != null) {
      try {
        st.close();
      } catch (SQLException e) {
        logger.warn("Could not close statement!", e);
      }
    }
    //关闭连接
    close(conn);
  }

  public final void close(Connection conn) {
    if (connectionTL.get() == null) {   // in transaction if conn in threadlocal
      if (conn != null)
        try {
          conn.close();
        } catch (SQLException e) {
          logger.warn("Could not close connection!", e);
        }
    }
  }
}
