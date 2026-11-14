package org.example.repository;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Types;
import java.util.List;
import java.util.Map;

@Repository
public class AdminRepository {
    private final JdbcTemplate jdbc;

    public AdminRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
    public long createCountry(String name, String code, String active) {
        jdbc.update("INSERT INTO country (country_name, destination_code, is_active) VALUES (?, ?, ?)", name, code, active);
        return ;
    }
    public void updateCountry(long id, String name, String code, String active) {

          }

}
