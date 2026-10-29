package com.markethub.dao;

import com.markethub.exception.MarketHubException;
import com.markethub.model.Role;
import com.markethub.model.User;

import java.util.List;
import java.util.Optional;

/** Database operations for accounts. */
public interface UserDao extends GenericDao<User, Long> {

    /** E-mail lookup (case-insensitive; addresses are stored lower-case). */
    Optional<User> findByEmail(String email) throws MarketHubException;

    List<User> findByRole(Role role) throws MarketHubException;
}
