package org.example.repository;

import com.nahidglobal.exception.ResourceNotFoundException;
import com.nahidglobal.security.CustomUserDetails;
import com.nahidglobal.entity.UserAccount;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class AccountRepository {
    private final JdbcTemplate jdbc;
    public AccountRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public CustomUserDetails loadUser(String username) {
        List<UserAccount> users = jdbc.query(
                "SELECT user_id, username, password_hash, full_name, email, " +
                "CASE WHEN EXISTS (SELECT 1 FROM staff_profile sf WHERE sf.user_id = user_account.user_id AND sf.is_active = 'N') " +
                "THEN 'INACTIVE' ELSE account_status END AS account_status " +
                "FROM user_account WHERE username = ?",
                (rs, rowNum) -> new UserAccount(rs.getLong("user_id"), rs.getString("username"),
                        rs.getString("password_hash"), rs.getString("full_name"), rs.getString("email"),
                        rs.getString("account_status")), username);
        if (users.isEmpty()) throw new ResourceNotFoundException("User not found.");
        UserAccount user = users.getFirst();
        List<String> roles = jdbc.queryForList(
                "SELECT r.role_name FROM app_role r JOIN user_role ur ON ur.role_id = r.role_id WHERE ur.user_id = ? ORDER BY r.role_name",
                String.class, user.userId());
        if (roles.size() != 1 || !java.util.Set.of("USER", "CONSULTANT", "ADMIN").contains(roles.getFirst())) {
            // USER_ACCOUNT intentionally has no role column. A valid USER_ROLE mapping is required.
            throw new ResourceNotFoundException("Account must have exactly one valid role (USER, CONSULTANT, ADMIN). Run database/migrate_three_roles.sql.");
        }
        return new CustomUserDetails(user, roles);
    }

    public long createUser(String fullName, String username, String passwordHash, String email, String status) {
        jdbc.update("INSERT INTO user_account (full_name, username, password_hash, email, account_status) VALUES (?, ?, ?, ?, ?)",
                fullName, username, passwordHash, email, status);
        return currentValue("seq_user_account");
    }

    public long createApplicant(long userId, String phone, long nationalityId) {
        jdbc.update("INSERT INTO applicant (user_id, phone, nationality_country_id) VALUES (?, ?, ?)",
                userId, phone, nationalityId);
        return currentValue("seq_applicant");
    }

    public long createStaffProfile(long userId, String title, String active) {
        jdbc.update("INSERT INTO staff_profile (user_id, staff_title, is_active) VALUES (?, ?, ?)", userId, title, active);
        return currentValue("seq_staff_profile");
    }

    private long currentValue(String sequence) {
        // The insert trigger consumed NEXTVAL on this same pooled connection/transaction.
        return jdbc.queryForObject("SELECT " + sequence + ".CURRVAL FROM dual", Long.class);
    }

    public long roleId(String roleName) {
        List<Long> ids = jdbc.query("SELECT role_id FROM app_role WHERE role_name = ?",
                (rs, rowNum) -> rs.getLong(1), roleName);
        if (ids.isEmpty()) throw new com.nahidglobal.exception.BusinessException(
                "Database setup is incomplete: role " + roleName + " does not exist. " +
                "Run database/migrate_three_roles.sql in the Oracle schema used by this application, then retry.");
        return ids.getFirst();
    }

    public void assignRole(long userId, String roleName, Long assignedBy) {
        jdbc.update("INSERT INTO user_role (user_id, role_id, assigned_by_user_id) VALUES (?, ?, ?)",
                userId, roleId(roleName), assignedBy);
    }

    public void replaceRoles(long userId, List<String> roles, Long assignedBy) {
        jdbc.update("DELETE FROM user_role WHERE user_id = ?", userId);
        for (String role : roles) assignRole(userId, role, assignedBy);
    }

    public Long applicantId(long userId) {
        List<Long> ids = jdbc.query("SELECT applicant_id FROM applicant WHERE user_id = ?",
                (rs, rowNum) -> rs.getLong(1), userId);
        return ids.isEmpty() ? null : ids.getFirst();
    }

    public Long applicantNationality(long userId) {
        List<Long> ids = jdbc.query("SELECT nationality_country_id FROM applicant WHERE user_id = ?",
                (rs, rowNum) -> rs.getLong(1), userId);
        return ids.isEmpty() ? null : ids.getFirst();
    }

    public Long staffId(long userId) {
        List<Long> ids = jdbc.query("SELECT staff_id FROM staff_profile WHERE user_id = ? AND is_active = 'Y'",
                (rs, rowNum) -> rs.getLong(1), userId);
        return ids.isEmpty() ? null : ids.getFirst();
    }

    public Map<String, Object> profile(long userId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT u.user_id AS \"userId\", u.full_name AS \"fullName\", u.username AS \"username\", u.email AS \"email\", " +
                "u.account_status AS \"accountStatus\", a.applicant_id AS \"applicantId\", a.phone AS \"phone\", " +
                "a.nationality_country_id AS \"nationalityCountryId\", c.country_name AS \"nationality\", " +
                "s.staff_id AS \"staffId\", s.staff_title AS \"staffTitle\", s.is_active AS \"staffActive\" " +
                "FROM user_account u LEFT JOIN applicant a ON a.user_id = u.user_id " +
                "LEFT JOIN country c ON c.country_id = a.nationality_country_id " +
                "LEFT JOIN staff_profile s ON s.user_id = u.user_id WHERE u.user_id = ?", userId);
        if (rows.isEmpty()) throw new ResourceNotFoundException("Profile not found.");
        Map<String, Object> result = rows.getFirst();
        result.put("roles", jdbc.queryForList("SELECT r.role_name FROM app_role r JOIN user_role ur ON ur.role_id = r.role_id WHERE ur.user_id = ? ORDER BY r.role_name", String.class, userId));
        return result;
    }

    public void updateBasicProfile(long userId, String fullName, String email) {
        jdbc.update("UPDATE user_account SET full_name = ?, email = ? WHERE user_id = ?", fullName, email, userId);
    }

    public void updatePasswordHash(long userId, String passwordHash) {
        int rows = jdbc.update("UPDATE user_account SET password_hash = ? WHERE user_id = ?", passwordHash, userId);
        if (rows != 1) throw new ResourceNotFoundException("User account not found.");
    }

    public void updateApplicantProfile(long userId, String phone, Long nationalityId) {
        int rows = jdbc.update("UPDATE applicant SET phone = ?, nationality_country_id = ? WHERE user_id = ?", phone, nationalityId, userId);
        if (rows == 0) throw new ResourceNotFoundException("Applicant profile not found.");
    }

    public void updateStaffProfile(long userId, String fullName, String email, String accountStatus, String title, String active) {
        int userRows = jdbc.update("UPDATE user_account SET full_name = ?, email = ?, account_status = ? WHERE user_id = ?",
                fullName, email, accountStatus, userId);
        if (userRows == 0) throw new ResourceNotFoundException("User account not found.");
        int staffRows = jdbc.update("UPDATE staff_profile SET staff_title = ?, is_active = ? WHERE user_id = ?", title, active, userId);
        if (staffRows == 0) throw new ResourceNotFoundException("Staff profile not found.");
    }

    public boolean isStaffUser(long userId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM staff_profile WHERE user_id = ?", Integer.class, userId);
        return count != null && count > 0;
    }

    public List<Map<String, Object>> listStaff() {
        return jdbc.queryForList("SELECT u.user_id AS \"userId\", u.full_name AS \"fullName\", u.username AS \"username\", u.email AS \"email\", " +
                "u.account_status AS \"accountStatus\", s.staff_id AS \"staffId\", s.staff_title AS \"staffTitle\", " +
                "s.is_active AS \"isActive\", LISTAGG(r.role_name, ', ') WITHIN GROUP (ORDER BY r.role_name) AS \"roles\" " +
                "FROM staff_profile s JOIN user_account u ON u.user_id = s.user_id " +
                "LEFT JOIN user_role ur ON ur.user_id = u.user_id LEFT JOIN app_role r ON r.role_id = ur.role_id " +
                "GROUP BY u.user_id, u.full_name, u.username, u.email, u.account_status, s.staff_id, s.staff_title, s.is_active " +
                "ORDER BY u.full_name");
    }

    public void updateAccountStatus(long userId, String status) {
        jdbc.update("UPDATE user_account SET account_status = ? WHERE user_id = ?", status, userId);
    }
}
