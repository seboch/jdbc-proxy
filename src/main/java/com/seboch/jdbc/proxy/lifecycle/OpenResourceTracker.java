package com.seboch.jdbc.proxy.lifecycle;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.seboch.jdbc.proxy.JdbcProxyFactory;

public class OpenResourceTracker implements ResourceLifecycleListener {

	public static final class CreationInfo<T> {

		private final T createdObject;
		private final long creationTime;
		private final List<StackTraceElement> creationStackTrace;

		public CreationInfo(T createdObject,
				List<StackTraceElement> creationStackTrace, long creationTime) {
			this.createdObject = createdObject;
			this.creationTime = creationTime;
			this.creationStackTrace = creationStackTrace;
		}

		public T getCreatedObject() {
			return createdObject;
		}

		public List<StackTraceElement> getCreationStackTrace() {
			return Collections.unmodifiableList(creationStackTrace);
		}

		public long getCreationTime() {
			return creationTime;
		}

		@Override
		public String toString() {
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(createdObject.getClass().getSimpleName());
			stringBuilder.append("@");
			stringBuilder.append(System.identityHashCode(createdObject));
			stringBuilder.append(", created ");
			stringBuilder
					.append((System.currentTimeMillis() - getCreationTime()));
			stringBuilder.append("ms ago");

			if (!getCreationStackTrace().isEmpty()) {
				stringBuilder.append("\n");
				for (StackTraceElement element : getCreationStackTrace()) {
					stringBuilder.append("\tat ").append(element.toString())
							.append("\n");
				}
			}

			return stringBuilder.toString();
		}
	}

	// FIXME we should probably use Identify hasmap here
	private final Map<Connection, CreationInfo<Connection>> openConnections = new ConcurrentHashMap<Connection, CreationInfo<Connection>>();
	private final Map<Statement, CreationInfo<Statement>> openStatements = new ConcurrentHashMap<Statement, CreationInfo<Statement>>();
	private final Map<ResultSet, CreationInfo<ResultSet>> openResultSets = new ConcurrentHashMap<ResultSet, CreationInfo<ResultSet>>();

	private int creationStackMaxLength = 20;

	private <T> CreationInfo<T> newCreationInfo(T object) {
		return new CreationInfo<T>(object, stackTrace(creationStackMaxLength),
				System.currentTimeMillis());
	}

	private List<StackTraceElement> stackTrace(int creationStackMaxLength) {
		if (creationStackMaxLength == 0) {
			return Collections.emptyList();
		} else {
			StackTraceElement[] stackTrace = Thread.currentThread()
					.getStackTrace();

			int skippedFirstLines = 1;
			while (skipStackElement(stackTrace[skippedFirstLines])
					&& skippedFirstLines < stackTrace.length) {
				skippedFirstLines++;
			}
			int upperBound = Math.min(stackTrace.length, creationStackMaxLength
					+ skippedFirstLines);
			List<StackTraceElement> stack = new ArrayList<StackTraceElement>(
					upperBound - skippedFirstLines);
			for (int i = skippedFirstLines; i < upperBound; i++) {
				stack.add(stackTrace[i]);
			}
			return stack;
		}
	}

	private boolean skipStackElement(StackTraceElement stackTraceElement) {
		String className = stackTraceElement.getClassName();
		if (className.equals(OpenResourceTracker.class.getName())) {
			return true;
		} else if (className.startsWith(JdbcProxyFactory.class.getName())) {
			return true;
		} else if (stackTraceElement.getMethodName().startsWith("invoke")
				&& stackTraceElement.getClassName().contains("reflect")) {
			return true;
		} else if (className.contains("EventListenerSupport")) {
			return true;
		} else if (className.startsWith("com.sun.proxy.$Proxy")) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void connectionCreated(Connection connection) {
		openConnections.put(connection, newCreationInfo(connection));
	}

	@Override
	public void connectionClosed(Connection connection) {
		openConnections.remove(connection);
	}

	@Override
	public void statementCreated(Statement statement) {
		openStatements.put(statement, newCreationInfo(statement));
	}

	@Override
	public void statementClosed(Statement statement) {
		openStatements.remove(statement);
	}

	@Override
	public void resultSetCreated(ResultSet resultSet) {
		openResultSets.put(resultSet, newCreationInfo(resultSet));
	}

	@Override
	public void resultSetClosed(ResultSet resultSet) {
		openResultSets.remove(resultSet);
	}

	public Map<Connection, CreationInfo<Connection>> getOpenConnections() {
		return new HashMap<Connection, OpenResourceTracker.CreationInfo<Connection>>(
				openConnections);
	}

	public Map<ResultSet, CreationInfo<ResultSet>> getOpenResultSets() {
		return new HashMap<ResultSet, OpenResourceTracker.CreationInfo<ResultSet>>(
				openResultSets);
	}

	public Map<Statement, CreationInfo<Statement>> getOpenStatements() {
		return new HashMap<Statement, OpenResourceTracker.CreationInfo<Statement>>(
				openStatements);
	}

}
