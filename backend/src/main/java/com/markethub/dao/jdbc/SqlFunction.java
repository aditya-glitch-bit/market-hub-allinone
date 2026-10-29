package com.markethub.dao.jdbc;

import java.sql.SQLException;

/**
 * Like {@link java.util.function.Function} but allowed to throw {@link SQLException}, so JDBC
 * code can be written as a lambda.
 *
 * @param <A> argument type
 * @param <R> result type
 */
@FunctionalInterface
interface SqlFunction<A, R> {

    R apply(A argument) throws SQLException;
}
