package com.markethub.service;

import com.markethub.concurrent.ActivityLog;
import com.markethub.dao.UserDao;
import com.markethub.exception.AuthenticationException;
import com.markethub.exception.ConflictException;
import com.markethub.exception.ForbiddenException;
import com.markethub.exception.MarketHubException;
import com.markethub.exception.NotFoundException;
import com.markethub.model.Role;
import com.markethub.model.User;
import com.markethub.model.UserFactory;
import com.markethub.util.LruCache;
import com.markethub.util.PasswordHasher;
import com.markethub.util.Validators;

import java.util.List;
import java.util.Optional;

/** Registration, sign-in and account administration. */
public class AccountService {

    /** Hash checked when the e-mail is unknown, so a miss takes as long as a wrong password. */
    private static final String DUMMY_HASH = PasswordHasher.hash("not-a-real-password-1");

    private final UserDao users;
    private final ActivityLog activity;
    /**
     * Every request re-loads the signed-in user, so recent users are cached. The cache is
     * evicted whenever an account changes, so a suspension takes effect immediately.
     * (With several server instances a shared cache or short expiry would be needed.)
     */
    private final LruCache<Long, User> cache = new LruCache<>(256);

    public AccountService(UserDao users, ActivityLog activity) {
        this.users = users;
        this.activity = activity;
    }

    /** Creates a buyer or seller account. Admin accounts can never be self-registered. */
    public User register(String name, String email, String password, String roleText) throws MarketHubException {
        Role role = Role.parse(roleText);
        if (role == Role.ADMIN) {
            throw new ForbiddenException("Admin accounts cannot be created through registration");
        }
        String cleanName = Validators.requireText(role == Role.SELLER ? "Store name" : "Name", name, 2, 100);
        String cleanEmail = Validators.requireEmail(email);
        Validators.requirePassword(password);
        if (users.findByEmail(cleanEmail).isPresent()) {
            throw new ConflictException("An account with this e-mail already exists");
        }
        User saved = users.save(UserFactory.create(role, null, cleanName, cleanEmail,
                PasswordHasher.hash(password), true, null));
        activity.record(saved.getName(), "registered as " + role.name().toLowerCase());
        return saved;
    }

    /** Checks credentials. The error message never says whether the e-mail or the password was wrong. */
    public User login(String email, String password) throws MarketHubException {
        Optional<User> found;
        try {
            found = users.findByEmail(Validators.requireEmail(email));
        } catch (MarketHubException e) {
            if (e.getHttpStatus() == 400) {
                found = Optional.empty(); // malformed e-mail: treat like "not found"
            } else {
                throw e;
            }
        }
        if (!found.isPresent()) {
            PasswordHasher.verify(String.valueOf(password), DUMMY_HASH);
            throw new AuthenticationException("Invalid e-mail or password");
        }
        User user = found.get();
        if (!PasswordHasher.verify(password, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid e-mail or password");
        }
        if (!user.isActive()) {
            throw new ForbiddenException("This account has been suspended");
        }
        activity.record(user.getName(), "signed in");
        return user;
    }

    /** Loads an account (cached). */
    public User getById(long id) throws MarketHubException {
        Optional<User> cached = cache.find(id);
        if (cached.isPresent()) {
            return cached.get();
        }
        User user = users.findById(id).orElseThrow(() -> new NotFoundException("User", id));
        cache.put(id, user);
        return user;
    }

    /** Loads the account behind a session and rejects it if it has been suspended since sign-in. */
    public User requireActive(long id) throws MarketHubException {
        User user;
        try {
            user = getById(id);
        } catch (NotFoundException e) {
            throw new AuthenticationException("Please sign in again");
        }
        if (!user.isActive()) {
            throw new ForbiddenException("This account has been suspended");
        }
        return user;
    }

    /** Admin only: every account. */
    public List<User> listAll(User actor) throws MarketHubException {
        requireAdmin(actor);
        return users.findAll();
    }

    /** Admin only: suspend or re-enable an account. Admins cannot suspend themselves. */
    public User setActive(User actor, long targetId, boolean active) throws MarketHubException {
        requireAdmin(actor);
        if (actor.getId() == targetId && !active) {
            throw new ConflictException("You cannot suspend your own account");
        }
        User target = users.findById(targetId).orElseThrow(() -> new NotFoundException("User", targetId));
        target.setActive(active);
        users.save(target);
        cache.remove(targetId);
        activity.record(actor.getName(), (active ? "reactivated " : "suspended ") + target.getName());
        return target;
    }

    private static void requireAdmin(User actor) throws ForbiddenException {
        if (actor.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Administrator access required");
        }
    }
}
