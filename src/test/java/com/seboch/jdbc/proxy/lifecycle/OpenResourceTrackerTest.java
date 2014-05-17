package com.seboch.jdbc.proxy.lifecycle;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;

import com.seboch.jdbc.proxy.JdbcProxyFactory;
import com.seboch.jdbc.proxy.lifecycle.OpenResourceTracker;
import com.seboch.jdbc.proxy.lifecycle.OpenResourceTracker.CreationInfo;

public class OpenResourceTrackerTest {

	private OpenResourceTracker tracker;
	private long startTime;

	@Before
	public void setup() {
		tracker = new OpenResourceTracker();
		startTime = System.currentTimeMillis();
	}

	@Test
	public void testConnections() {
		Connection mockConnection = mock(Connection.class);
		Connection mockConnection2 = mock(Connection.class);

		tracker.connectionCreated(mockConnection);
		tracker.connectionCreated(mockConnection2);
		tracker.connectionClosed(mockConnection2);

		assertEquals(1, tracker.getOpenConnections().size());
		assertTrue(tracker.getOpenConnections().containsKey(mockConnection));
		assertFalse(tracker.getOpenConnections().containsKey(mockConnection2));
		CreationInfo<Connection> creationInfo = tracker.getOpenConnections()
				.get(mockConnection);
		assertEquals(mockConnection, creationInfo.getCreatedObject());
		assertTrue(creationInfo.getCreationTime() >= startTime);
		checkStack(creationInfo.getCreationStackTrace());
	}

	@Test
	public void testStatements() {
		Statement mockStatement = mock(Statement.class);
		Statement mockStatement2 = mock(Statement.class);

		tracker.statementCreated(mockStatement);
		tracker.statementCreated(mockStatement2);
		tracker.statementClosed(mockStatement2);

		assertEquals(1, tracker.getOpenStatements().size());
		assertTrue(tracker.getOpenStatements().containsKey(mockStatement));
		assertFalse(tracker.getOpenStatements().containsKey(mockStatement2));

		CreationInfo<Statement> creationInfo = tracker.getOpenStatements().get(
				mockStatement);
		assertEquals(mockStatement, creationInfo.getCreatedObject());
		assertTrue(creationInfo.getCreationTime() >= startTime);
		checkStack(creationInfo.getCreationStackTrace());
	}

	@Test
	public void testResultSets() {
		ResultSet mockResultSet = mock(ResultSet.class);
		ResultSet mockResultSet2 = mock(ResultSet.class);

		tracker.resultSetCreated(mockResultSet);
		tracker.resultSetCreated(mockResultSet2);
		tracker.resultSetClosed(mockResultSet2);

		assertEquals(1, tracker.getOpenResultSets().size());
		assertTrue(tracker.getOpenResultSets().containsKey(mockResultSet));
		assertFalse(tracker.getOpenResultSets().containsKey(mockResultSet2));

		CreationInfo<ResultSet> creationInfo = tracker.getOpenResultSets().get(
				mockResultSet);
		assertEquals(mockResultSet, creationInfo.getCreatedObject());
		assertTrue(creationInfo.getCreationTime() >= startTime);

		checkStack(creationInfo.getCreationStackTrace());
	}

	@Test
	public void testConnectionStackFromJdbcProxyFactory() throws SQLException {
		JdbcProxyFactory factory = new JdbcProxyFactory();

		DataSource mockDatasource = mock(DataSource.class);
		Connection mockConnection = mock(Connection.class);

		when(mockDatasource.getConnection()).thenReturn(mockConnection);

		factory.addResourceLifecycleListener(tracker);

		factory.proxyDatasource(mockDatasource).getConnection();

		CreationInfo<Connection> creationInfo = tracker.getOpenConnections()
				.get(mockConnection);
		System.out.println(creationInfo.toString());
		checkStack(creationInfo.getCreationStackTrace());
	}

	private void checkStack(List<StackTraceElement> creationStackTrace) {
		// Assert stack trace start in current class that created elements
		assertEquals(getClass().getName(), creationStackTrace.get(0)
				.getClassName());
	}
}
