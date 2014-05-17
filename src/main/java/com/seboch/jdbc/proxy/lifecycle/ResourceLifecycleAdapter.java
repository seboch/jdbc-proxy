package com.seboch.jdbc.proxy.lifecycle;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class ResourceLifecycleAdapter implements ResourceLifecycleListener {

	@Override
	public void connectionCreated(Connection connection) {
	}

	@Override
	public void connectionClosed(Connection connection) {
	}

	@Override
	public void statementCreated(Statement statement) {
	}

	@Override
	public void statementClosed(Statement statement) {
	}

	@Override
	public void resultSetCreated(ResultSet resultSet) {
	}

	@Override
	public void resultSetClosed(ResultSet resultSet) {
	}

}
