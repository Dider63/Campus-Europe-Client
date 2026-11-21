package org.example.repository;

import org.example.exception.ResourceNotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class CatalogRepository {
    private final JdbcTemplate jdbc;
    public CatalogRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Map<String, Object>> countries(boolean activeOnly) {
        return jdbc.queryForList("SELECT country_id AS \"countryId\", country_name AS \"countryName\", " +
                "destination_code AS \"destinationCode\", is_active AS \"isActive\" FROM country " +
                (activeOnly ? "WHERE is_active = 'Y' " : "") + "ORDER BY country_name");
    }

    public List<Map<String, Object>> visaTypes(boolean activeOnly) {
        return jdbc.queryForList("SELECT visa_type_id AS \"visaTypeId\", type_name AS \"typeName\", " +
                "description AS \"description\", is_active AS \"isActive\" FROM visa_type " +
                (activeOnly ? "WHERE is_active = 'Y' " : "") + "ORDER BY type_name");
    }

    public List<Map<String, Object>> catalog(Long destinationId, Long nationalityId, Long visaTypeId) {
        StringBuilder sql = new StringBuilder("SELECT visa_rule_id AS \"visaRuleId\", visa_type_id AS \"visaTypeId\", " +
                "visa_type_name AS \"visaTypeName\", destination_country_id AS \"destinationCountryId\", " +
                "destination_country AS \"destinationCountry\", nationality_country_id AS \"nationalityCountryId\", " +
                "applicant_nationality AS \"applicantNationality\", service_fee AS \"serviceFee\", " +
                "currency_code AS \"currencyCode\", processing_time AS \"processingTime\", last_updated AS \"lastUpdated\" " +
                "FROM vw_visa_catalog WHERE 1 = 1 " +
                "AND EXISTS (SELECT 1 FROM country d WHERE d.country_id = destination_country_id AND d.is_active = 'Y') " +
                "AND EXISTS (SELECT 1 FROM country n WHERE n.country_id = nationality_country_id AND n.is_active = 'Y')");
        java.util.ArrayList<Object> args = new java.util.ArrayList<>();
        if (destinationId != null) { sql.append(" AND destination_country_id = ?"); args.add(destinationId); }
        if (nationalityId != null) { sql.append(" AND nationality_country_id = ?"); args.add(nationalityId); }
        if (visaTypeId != null) { sql.append(" AND visa_type_id = ?"); args.add(visaTypeId); }
        sql.append(" ORDER BY destination_country, visa_type_name");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    public Map<String, Object> catalogItem(long visaRuleId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT vr.visa_rule_id AS \"visaRuleId\", " +
                "vr.visa_type_id AS \"visaTypeId\", vt.type_name AS \"visaTypeName\", vt.description AS \"visaDescription\", " +
                "vr.destination_country_id AS \"destinationCountryId\", d.country_name AS \"destinationCountry\", " +
                "vr.nationality_country_id AS \"nationalityCountryId\", n.country_name AS \"applicantNationality\", " +
                "vr.service_fee AS \"serviceFee\", vr.currency_code AS \"currencyCode\", vr.processing_time AS \"processingTime\", " +
                "vr.is_active AS \"isActive\", vr.last_updated AS \"lastUpdated\" " +
                "FROM visa_rule vr JOIN visa_type vt ON vt.visa_type_id = vr.visa_type_id " +
                "JOIN country d ON d.country_id = vr.destination_country_id " +
                "JOIN country n ON n.country_id = vr.nationality_country_id WHERE vr.visa_rule_id = ? " +
                "AND vr.is_active = 'Y' AND vt.is_active = 'Y' AND d.is_active = 'Y' AND n.is_active = 'Y'", visaRuleId);
        if (rows.isEmpty()) throw new ResourceNotFoundException("Visa rule not found.");
        return rows.getFirst();
    }

    public List<Map<String, Object>> checklist(long visaRuleId) {
        return jdbc.queryForList("SELECT vrc.checklist_item_id AS \"checklistItemId\", " +
                "ci.item_name AS \"itemName\", ci.item_description AS \"itemDescription\", " +
                "vrc.is_required AS \"isRequired\", vrc.display_order AS \"displayOrder\", " +
                "vrc.instructions AS \"instructions\" FROM visa_rule_checklist vrc JOIN checklist_item ci " +
                "ON ci.checklist_item_id = vrc.checklist_item_id WHERE vrc.visa_rule_id = ? " +
                "ORDER BY vrc.display_order, ci.item_name", visaRuleId);
    }

    public void assertActiveVisaType(long visaTypeId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM visa_type WHERE visa_type_id = ? AND is_active = 'Y'", Integer.class, visaTypeId);
        if (count == null || count == 0) throw new IllegalArgumentException("The selected visa type is not active or does not exist.");
    }

    public void assertActiveCountry(long countryId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM country WHERE country_id = ? AND is_active = 'Y'", Integer.class, countryId);
        if (count == null || count == 0) throw new IllegalArgumentException("The selected country is not active or does not exist.");
    }
}
