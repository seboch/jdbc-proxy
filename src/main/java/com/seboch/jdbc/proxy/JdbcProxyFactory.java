package com.seboch.jdbc.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.lang3.event.EventListenerSupport;

import com.seboch.jdbc.proxy.lifecycle.ResourceLifecycleListener;

/**
 * @author sarod
 *
 */
public final class JdbcProxyFactory {

	private static final String CLOSE_METHOD = "close";

	private final EventListenerSupport<ResourceLifecycleListener> lifecycleListenerSupport;

	class DataSourceInvocationHandler implements InvocationHandler {

		private final DataSource wrappedDataSource;

		public DataSourceInvocationHandler(DataSource wrappedDataSource) {
			this.wrappedDataSource = wrappedDataSource;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			if (Connection.class.isAssignableFrom(method.getReturnType())) {
				Connection connection = (Connection) method.invoke(
						wrappedDataSource, args);
				return proxyConnection(connection);
			} else {
				return method.invoke(wrappedDataSource, args);
			}
		}
	}

	class ConnectionInvocationHandler implements InvocationHandler {

		private final Connection connection;

		public ConnectionInvocationHandler(Connection connection) {
			this.connection = connection;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			if (method.getName().equals(CLOSE_METHOD)) {
				method.invoke(connection, args);
				lifecycleListenerSupport.fire().connectionClosed(connection);
				return null;
			} else if (Statement.class.isAssignableFrom(method.getReturnType())) {
				Statement statement = (Statement) method.invoke(connection,
						args);
				lifecycleListenerSupport.fire().statementCreated(statement);
				Class<?>[] interfaces = null;
				if (statement instanceof CallableStatement) {
					interfaces = new Class<?>[] { CallableStatement.class };
				} else if (statement instanceof PreparedStatement) {
					interfaces = new Class<?>[] { PreparedStatement.class };
				} else {
					interfaces = new Class<?>[] { Statement.class };
				}
				return (Statement) Proxy.newProxyInstance(getClassLoader(),
						interfaces, new StatementInvocationHandler(statement));
			} else {
				return method.invoke(connection, args);
			}
		}
	}

	class ResultSetInvocationHandler implements InvocationHandler {
		private final ResultSet resultSet;

		public ResultSetInvocationHandler(ResultSet resultSet) {
			this.resultSet = resultSet;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			if (method.getName().equals(CLOSE_METHOD)) {
				method.invoke(resultSet, args);
				lifecycleListenerSupport.fire().resultSetClosed(resultSet);
				return null;
			} else {
				return method.invoke(resultSet, args);
			}
		}
	}

	class StatementInvocationHandler implements InvocationHandler {
		private final Statement statement;

		public StatementInvocationHandler(Statement statement) {
			this.statement = statement;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			if (method.getName().equals(CLOSE_METHOD)) {
				method.invoke(statement, args);
				lifecycleListenerSupport.fire().statementClosed(statement);
				return null;
			} else if (method.getName().equals("executeQuery")
					&& ResultSet.class.isAssignableFrom(method.getReturnType())) {
				ResultSet resultSet = (ResultSet) method
						.invoke(statement, args);
				lifecycleListenerSupport.fire().resultSetCreated(resultSet);
				return (ResultSet) Proxy.newProxyInstance(getClassLoader(),
						new Class<?>[] { ResultSet.class },
						new ResultSetInvocationHandler(resultSet));
			} else {
				return method.invoke(statement, args);
			}
		}
	}

	public JdbcProxyFactory() {
		lifecycleListenerSupport = new EventListenerSupport<ResourceLifecycleListener>(
				ResourceLifecycleListener.class);
	}

	private ClassLoader getClassLoader() {
		return getClass().getClassLoader();
	}

	public DataSource proxyDatasource(DataSource datasource) {
		return (DataSource) Proxy.newProxyInstance(getClassLoader(),
				new Class<?>[] { DataSource.class },
				new DataSourceInvocationHandler(datasource));
	}

	public Connection proxyConnection(Connection connection) {
		lifecycleListenerSupport.fire().connectionCreated(connection);
		return (Connection) Proxy.newProxyInstance(getClassLoader(),
				new Class<?>[] { Connection.class },
				new ConnectionInvocationHandler(connection));
	}

	public void addResourceLifecycleListener(ResourceLifecycleListener listener) {
		lifecycleListenerSupport.addListener(listener);
	}

	public void removeResourceLifecycleListener(
			ResourceLifecycleListener listener) {
		lifecycleListenerSupport.removeListener(listener);
	}

}
