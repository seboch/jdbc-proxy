package com.seboch.jdbc.proxy.lifecycle;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public interface ResourceLifecycleListener {

	public void connectionCreated(Connection connection);

	public void connectionClosed(Connection connection);
	
	public void statementCreated(Statement statement);

	public void statementClosed(Statement statement);
	
	public void resultSetCreated(ResultSet resultSet);

	public void resultSetClosed(ResultSet resultSet);
}

