package com.seboch.jdbc.proxy;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;

import com.seboch.jdbc.proxy.JdbcProxyFactory;
import com.seboch.jdbc.proxy.lifecycle.ResourceLifecycleListener;

public class JdbcProxyFactoryTest {

	private DataSource mockDatasource;
	private Connection mockConnection;
	private PreparedStatement mockPreparedStatement;
	private Statement mockStatement;
	private CallableStatement mockCallableStatement;
	private ResultSet mockResultSet;
	private ResourceLifecycleListener mockListener;
	private JdbcProxyFactory proxyFactory;

	@Before
	public void setup() throws SQLException {
		mockDatasource = mock(DataSource.class);
		mockConnection = mock(Connection.class);
		when(mockDatasource.getConnection()).thenReturn(mockConnection);
		when(mockDatasource.getConnection(anyString(), anyString()))
				.thenReturn(mockConnection);

		mockStatement = mock(Statement.class);
		when(mockConnection.createStatement()).thenReturn(mockStatement);
		when(mockConnection.createStatement(anyInt(), anyInt())).thenReturn(
				mockStatement);
		when(mockConnection.createStatement(anyInt(), anyInt(), anyInt()))
				.thenReturn(mockStatement);

		mockCallableStatement = mock(CallableStatement.class);
		when(mockConnection.prepareCall(anyString())).thenReturn(
				mockCallableStatement);
		when(mockConnection.prepareCall(anyString(), anyInt(), anyInt()))
				.thenReturn(mockCallableStatement);
		when(
				mockConnection.prepareCall(anyString(), anyInt(), anyInt(),
						anyInt())).thenReturn(mockCallableStatement);

		mockPreparedStatement = mock(PreparedStatement.class);
		when(mockConnection.prepareStatement(anyString())).thenReturn(
				mockPreparedStatement);
		when(mockConnection.prepareStatement(anyString(), anyInt()))
				.thenReturn(mockPreparedStatement);
		when(mockConnection.prepareStatement(anyString(), any(int[].class)))
				.thenReturn(mockPreparedStatement);
		when(mockConnection.prepareStatement(anyString(), any(String[].class)))
				.thenReturn(mockPreparedStatement);
		when(mockConnection.prepareStatement(anyString(), anyInt(), anyInt()))
				.thenReturn(mockPreparedStatement);
		when(
				mockConnection.prepareStatement(anyString(), anyInt(),
						anyInt(), anyInt())).thenReturn(mockPreparedStatement);

		mockResultSet = mock(ResultSet.class);
		when(mockStatement.executeQuery("Truc")).thenReturn(mockResultSet);
		when(mockCallableStatement.executeQuery("Truc")).thenReturn(
				mockResultSet);
		when(mockPreparedStatement.executeQuery("Truc")).thenReturn(
				mockResultSet);
		when(mockCallableStatement.executeQuery()).thenReturn(mockResultSet);
		when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

		mockListener = mock(ResourceLifecycleListener.class);
		proxyFactory = new JdbcProxyFactory();
	}

	@Test
	public void testDataSourceProxy() throws SQLException {
		DataSource proxiedDs = proxyFactory.proxyDatasource(mockDatasource);
		when(mockDatasource.getLoginTimeout()).thenReturn(55);

		assertNotSame(proxiedDs, mockDatasource);
		assertSame(mockDatasource.getLoginTimeout(),
				proxiedDs.getLoginTimeout());
		assertNotSame(mockDatasource.getConnection(), proxiedDs.getConnection());

	}

	@Test
	public void testConnectionProxy() throws SQLException {
		proxyFactory.addResourceLifecycleListener(mockListener);
		DataSource proxiedDs = proxyFactory.proxyDatasource(mockDatasource);

		verifyZeroInteractions(mockListener);

		Connection proxiedConnection = proxiedDs.getConnection();
		verify(mockListener).connectionCreated(mockConnection);
		verify(mockListener, never()).connectionClosed(any(Connection.class));

		proxiedConnection.close();
		verify(mockListener).connectionClosed(mockConnection);

		when(proxiedConnection.getCatalog()).thenReturn("XXX");

		assertNotSame(mockConnection, proxiedConnection);
		assertSame(mockConnection.getCatalog(), proxiedConnection.getCatalog());

		proxyFactory.removeResourceLifecycleListener(mockListener);

		proxiedDs.getConnection();
		verifyNoMoreInteractions(mockListener);

	}

	@Test
	public void testCallableStatementProxy() throws SQLException {
		proxyFactory.addResourceLifecycleListener(mockListener);
		DataSource proxiedDs = proxyFactory.proxyDatasource(mockDatasource);
		Connection connection = proxiedDs.getConnection();

		verify(mockListener, never()).statementCreated(any(Statement.class));
		verify(mockListener, never()).statementClosed(any(Statement.class));

		CallableStatement callableStatement = connection.prepareCall("Truc");
		verify(mockListener).statementCreated(mockCallableStatement);

		callableStatement.close();
		verify(mockListener).statementClosed(mockCallableStatement);

	}

	@Test
	public void testStatementLifecycle() throws SQLException {
		proxyFactory.addResourceLifecycleListener(mockListener);
		DataSource proxiedDs = proxyFactory.proxyDatasource(mockDatasource);
		Connection connection = proxiedDs.getConnection();

		verify(mockListener, never()).statementCreated(any(Statement.class));
		verify(mockListener, never()).statementClosed(any(Statement.class));

		Statement statement = connection.createStatement();
		verify(mockListener).statementCreated(mockStatement);

		statement.close();
		verify(mockListener).statementClosed(mockStatement);
	}

	@Test
	public void testPreparedStatementLifecycle() throws SQLException {
		proxyFactory.addResourceLifecycleListener(mockListener);
		DataSource proxiedDs = proxyFactory.proxyDatasource(mockDatasource);
		Connection connection = proxiedDs.getConnection();

		verify(mockListener, never()).statementCreated(any(Statement.class));
		verify(mockListener, never()).statementClosed(any(Statement.class));

		PreparedStatement preparedStatement = connection
				.prepareStatement("Truc");
		verify(mockListener).statementCreated(mockPreparedStatement);

		preparedStatement.close();
		verify(mockListener).statementClosed(mockPreparedStatement);
	}

	@Test
	public void testResultSetLifecycle() throws SQLException {
		proxyFactory.addResourceLifecycleListener(mockListener);
		DataSource proxiedDs = proxyFactory.proxyDatasource(mockDatasource);
		Connection connection = proxiedDs.getConnection();
		PreparedStatement statement = connection.prepareStatement("Truc");

		verify(mockListener, never()).resultSetCreated(any(ResultSet.class));
		verify(mockListener, never()).resultSetClosed(any(ResultSet.class));

		ResultSet resultSet = statement.executeQuery();
		verify(mockListener).resultSetCreated(mockResultSet);

		resultSet.close();
		verify(mockListener).resultSetClosed(mockResultSet);
	}

}
