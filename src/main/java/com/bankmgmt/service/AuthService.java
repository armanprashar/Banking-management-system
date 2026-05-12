package com.bankmgmt.service;

import com.bankmgmt.dao.IActivityLogDAO;
import com.bankmgmt.dao.IAdminDAO;
import com.bankmgmt.dao.IUserDAO;
import com.bankmgmt.exceptions.AuthenticationException;
import com.bankmgmt.exceptions.BankException;
import com.bankmgmt.model.Role;
import com.bankmgmt.model.User;
import com.bankmgmt.utils.FormValidator;
import com.bankmgmt.utils.PasswordHasher;

/**
 * Authentication / registration / password recovery orchestration (service layer).
 */
public class AuthService {

    private final IUserDAO userDAO;
    private final IAdminDAO adminDAO;
    private final IActivityLogDAO activityLogDAO;

    public AuthService(IUserDAO userDAO, IAdminDAO adminDAO, IActivityLogDAO activityLogDAO) {
        this.userDAO = userDAO;
        this.adminDAO = adminDAO;
        this.activityLogDAO = activityLogDAO;
    }

    public User login(String username, String password) throws AuthenticationException {
        User u = userDAO.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("Invalid credentials."));
        if (!u.isActive()) {
            throw new AuthenticationException("Account deactivated.");
        }
        if (!PasswordHasher.verify(password, u.getSalt(), u.getPasswordHash())) {
            activityLogDAO.insert(u.getId(), "LOGIN_FAIL", "username=" + username);
            throw new AuthenticationException("Invalid credentials.");
        }
        activityLogDAO.insert(u.getId(), "LOGIN_SUCCESS", null);
        return u;
    }

    public boolean isPlatformAdmin(User user) {
        return user.getRole() == Role.ADMIN && adminDAO.isAdmin(user.getId());
    }

    public User registerCustomer(String username, String email, String plainPassword,
                                 String fullName, String phone,
                                 String securityQuestion, String securityAnswerPlain)
            throws BankException {
        if (!FormValidator.isUsername(username)) {
            throw new BankException("Username must be 4–32 chars: letters, digits, underscore.");
        }
        if (!FormValidator.isEmail(email)) {
            throw new BankException("Invalid email.");
        }
        if (!FormValidator.isStrongEnoughPassword(plainPassword)) {
            throw new BankException("Password needs ≥8 chars with upper, lower, digit, and symbol.");
        }
        if (!FormValidator.isNonBlank(fullName)) {
            throw new BankException("Full name required.");
        }
        if (!FormValidator.isNonBlank(securityQuestion) || !FormValidator.isNonBlank(securityAnswerPlain)) {
            throw new BankException("Security question and answer required.");
        }
        if (userDAO.existsByUsername(username)) {
            throw new BankException("Username already registered.");
        }
        if (userDAO.existsByEmail(email)) {
            throw new BankException("Email already registered.");
        }

        String salt = PasswordHasher.generateSaltHex(16);
        String pwdHash = PasswordHasher.hash(plainPassword, salt);
        String ansSalt = PasswordHasher.generateSaltHex(16);
        String ansHash = PasswordHasher.hash(securityAnswerPlain.trim(), ansSalt);

        User u = new User();
        u.setUsername(username.trim());
        u.setEmail(email.trim());
        u.setPasswordHash(pwdHash);
        u.setSalt(salt);
        u.setRole(Role.USER);
        u.setFullName(fullName.trim());
        u.setPhone(FormValidator.isNonBlank(phone) ? phone.trim() : null);
        u.setSecurityQuestion(securityQuestion.trim());
        u.setSecurityAnswerHash(ansHash);
        u.setSaltAnswer(ansSalt);
        u.setActive(true);

        long id = userDAO.insert(u);
        u.setId(id);
        activityLogDAO.insert(id, "USER_REGISTERED", username);
        return u;
    }

    /** Validates recovery answer then rotates password with freshly generated salts. */
    public void resetPasswordAfterVerification(String username,
                                               String securityAnswerPlain,
                                               String newPlainPassword)
            throws AuthenticationException, BankException {
        User u = userDAO.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("User not found."));
        if (!PasswordHasher.verify(securityAnswerPlain.trim(), u.getSaltAnswer(), u.getSecurityAnswerHash())) {
            activityLogDAO.insert(u.getId(), "PASSWORD_RESET_FAIL", null);
            throw new AuthenticationException("Security answer does not match.");
        }
        if (!FormValidator.isStrongEnoughPassword(newPlainPassword)) {
            throw new BankException("New password needs ≥8 chars with upper, lower, digit, and symbol.");
        }
        String salt = PasswordHasher.generateSaltHex(16);
        String hash = PasswordHasher.hash(newPlainPassword, salt);
        userDAO.updatePassword(u.getId(), hash, salt);
        activityLogDAO.insert(u.getId(), "PASSWORD_RESET_OK", null);
    }

    public String recoveryQuestionFor(String username) throws AuthenticationException {
        User u = userDAO.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("User not found."));
        return u.getSecurityQuestion();
    }
}
