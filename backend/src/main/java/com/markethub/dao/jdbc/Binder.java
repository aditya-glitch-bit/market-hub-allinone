package com.markethub.dao.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Fills the {@code ?} placeholders of a prepared statement. */
@FunctionalInterface
interface Binder {

    /** Binder for statements without parameters. */
    Binder NONE = statement -> { };

    void bind(PreparedStatement statement) throws SQLException;
}
