
SET DEFINE OFF
CREATE TABLE country (
    country_id          NUMBER(10) NOT NULL,
    country_name        VARCHAR2(100) NOT NULL,
    destination_code    VARCHAR2(10) NOT NULL,
    is_active           CHAR(1) DEFAULT 'Y' NOT NULL,
    created_at          TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_country PRIMARY KEY (country_id),
    CONSTRAINT uq_country_name UNIQUE (country_name),
    CONSTRAINT uq_country_code UNIQUE (destination_code),
    CONSTRAINT ck_country_active CHECK (is_active IN ('Y', 'N'))
);

CREATE TABLE visa_type (
    visa_type_id        NUMBER(10) NOT NULL,
    type_name           VARCHAR2(100) NOT NULL,
    description         VARCHAR2(1000),
    is_active           CHAR(1) DEFAULT 'Y' NOT NULL,
    created_at          TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_visa_type PRIMARY KEY (visa_type_id),
    CONSTRAINT uq_visa_type_name UNIQUE (type_name),
    CONSTRAINT ck_vtype_active CHECK (is_active IN ('Y', 'N'))
);

CREATE TABLE checklist_item (
    checklist_item_id   NUMBER(10) NOT NULL,
    item_name           VARCHAR2(200) NOT NULL,
    item_description    VARCHAR2(1000),
    is_active           CHAR(1) DEFAULT 'Y' NOT NULL,
    CONSTRAINT pk_checklist_item PRIMARY KEY (checklist_item_id),
    CONSTRAINT uq_checklist_item UNIQUE (item_name),
    CONSTRAINT ck_checkitem_active CHECK (is_active IN ('Y', 'N'))
);

CREATE TABLE user_account (
    user_id             NUMBER(10) NOT NULL,
    full_name           VARCHAR2(150) NOT NULL,
    username            VARCHAR2(80) NOT NULL,
    password_hash       VARCHAR2(500) NOT NULL,
    email               VARCHAR2(254) NOT NULL,
    account_status      VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    created_at          TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at          TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_user_account PRIMARY KEY (user_id),
    CONSTRAINT uq_user_username UNIQUE (username),
    CONSTRAINT uq_user_email UNIQUE (email),
    CONSTRAINT ck_user_status CHECK
        (account_status IN ('PENDING', 'ACTIVE', 'INACTIVE', 'LOCKED'))
);

CREATE TABLE app_role (
    role_id             NUMBER(10) NOT NULL,
    role_name           VARCHAR2(40) NOT NULL,
    description         VARCHAR2(500),
    CONSTRAINT pk_app_role PRIMARY KEY (role_id),
    CONSTRAINT uq_app_role_name UNIQUE (role_name)
);

CREATE TABLE user_role (
    user_id             NUMBER(10) NOT NULL,
    role_id             NUMBER(10) NOT NULL,
    assigned_at         TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    assigned_by_user_id NUMBER(10),
    CONSTRAINT pk_user_role PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_urole_user FOREIGN KEY (user_id)
        REFERENCES user_account (user_id),
    CONSTRAINT fk_urole_role FOREIGN KEY (role_id)
        REFERENCES app_role (role_id),
    CONSTRAINT fk_urole_assigner FOREIGN KEY (assigned_by_user_id)
        REFERENCES user_account (user_id)
);

CREATE TABLE applicant (
    applicant_id            NUMBER(10) NOT NULL,
    user_id                 NUMBER(10) NOT NULL,
    phone                   VARCHAR2(30),
    nationality_country_id  NUMBER(10) NOT NULL,
    created_at              TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_applicant PRIMARY KEY (applicant_id),
    CONSTRAINT uq_applicant_user UNIQUE (user_id),
    CONSTRAINT fk_app_user FOREIGN KEY (user_id)
        REFERENCES user_account (user_id),
    CONSTRAINT fk_app_nation FOREIGN KEY (nationality_country_id)
        REFERENCES country (country_id)
);

CREATE TABLE staff_profile (
    staff_id            NUMBER(10) NOT NULL,
    user_id             NUMBER(10) NOT NULL,
    staff_title         VARCHAR2(100),
    is_active           CHAR(1) DEFAULT 'Y' NOT NULL,
    created_at          TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_staff_profile PRIMARY KEY (staff_id),
    CONSTRAINT uq_staff_user UNIQUE (user_id),
    CONSTRAINT fk_staff_user FOREIGN KEY (user_id)
        REFERENCES user_account (user_id),
    CONSTRAINT ck_staff_active CHECK (is_active IN ('Y', 'N'))
);

CREATE TABLE visa_rule (
    visa_rule_id            NUMBER(10) NOT NULL,
    visa_type_id            NUMBER(10) NOT NULL,
    destination_country_id  NUMBER(10) NOT NULL,
    nationality_country_id  NUMBER(10) NOT NULL,
    service_fee             NUMBER(12,2) DEFAULT 0 NOT NULL,
    currency_code           CHAR(3) DEFAULT 'BDT' NOT NULL,
    processing_time         VARCHAR2(100),
    is_active               CHAR(1) DEFAULT 'Y' NOT NULL,
    last_updated            TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_by_user_id      NUMBER(10),
    update_note             VARCHAR2(1000),
    CONSTRAINT pk_visa_rule PRIMARY KEY (visa_rule_id),
    CONSTRAINT uq_visa_rule UNIQUE
        (visa_type_id, destination_country_id, nationality_country_id),
    CONSTRAINT fk_vrule_type FOREIGN KEY (visa_type_id)
        REFERENCES visa_type (visa_type_id),
    CONSTRAINT fk_vrule_dest FOREIGN KEY (destination_country_id)
        REFERENCES country (country_id),
    CONSTRAINT fk_vrule_nat FOREIGN KEY (nationality_country_id)
        REFERENCES country (country_id),
    CONSTRAINT fk_vrule_user FOREIGN KEY (updated_by_user_id)
        REFERENCES user_account (user_id),
    CONSTRAINT ck_vrule_fee CHECK (service_fee >= 0),
    CONSTRAINT ck_vrule_active CHECK (is_active IN ('Y', 'N'))
);

CREATE TABLE visa_rule_checklist (
    visa_rule_id        NUMBER(10) NOT NULL,
    checklist_item_id   NUMBER(10) NOT NULL,
    is_required         CHAR(1) DEFAULT 'Y' NOT NULL,
    display_order       NUMBER(4) DEFAULT 1 NOT NULL,
    instructions        VARCHAR2(1000),
    CONSTRAINT pk_vrule_check PRIMARY KEY (visa_rule_id, checklist_item_id),
    CONSTRAINT fk_vrc_rule FOREIGN KEY (visa_rule_id)
        REFERENCES visa_rule (visa_rule_id),
    CONSTRAINT fk_vrc_item FOREIGN KEY (checklist_item_id)
        REFERENCES checklist_item (checklist_item_id),
    CONSTRAINT ck_vrc_required CHECK (is_required IN ('Y', 'N')),
    CONSTRAINT ck_vrc_order CHECK (display_order >= 1)
);

CREATE TABLE visa_rule_fee_history (
    fee_history_id      NUMBER(10) NOT NULL,
    visa_rule_id        NUMBER(10) NOT NULL,
    old_service_fee     NUMBER(12,2) NOT NULL,
    new_service_fee     NUMBER(12,2) NOT NULL,
    old_currency_code   CHAR(3) NOT NULL,
    new_currency_code   CHAR(3) NOT NULL,
    changed_at          TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    changed_by_user_id  NUMBER(10),
    change_note         VARCHAR2(1000),
    CONSTRAINT pk_vrule_fee_hist PRIMARY KEY (fee_history_id),
    CONSTRAINT fk_vrfh_rule FOREIGN KEY (visa_rule_id)
        REFERENCES visa_rule (visa_rule_id),
    CONSTRAINT fk_vrfh_user FOREIGN KEY (changed_by_user_id)
        REFERENCES user_account (user_id),
    CONSTRAINT ck_vrfh_old_fee CHECK (old_service_fee >= 0),
    CONSTRAINT ck_vrfh_new_fee CHECK (new_service_fee >= 0)
);

CREATE TABLE visa_application (
    application_id           NUMBER(10) NOT NULL,
    applicant_id             NUMBER(10) NOT NULL,
    visa_rule_id             NUMBER(10) NOT NULL,
    application_date         TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    submission_date          TIMESTAMP,
    current_status           VARCHAR2(30) DEFAULT 'DRAFT' NOT NULL,
    service_fee_snapshot     NUMBER(12,2) NOT NULL,
    currency_code_snapshot   CHAR(3) NOT NULL,
    processing_time_snapshot VARCHAR2(100),
    last_updated              TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    last_updated_by_staff_id NUMBER(10),
    last_status_note         VARCHAR2(1000),
    CONSTRAINT pk_visa_application PRIMARY KEY (application_id),
    CONSTRAINT fk_vapp_applicant FOREIGN KEY (applicant_id)
        REFERENCES applicant (applicant_id),
    CONSTRAINT fk_vapp_rule FOREIGN KEY (visa_rule_id)
        REFERENCES visa_rule (visa_rule_id),
    CONSTRAINT fk_vapp_staff FOREIGN KEY (last_updated_by_staff_id)
        REFERENCES staff_profile (staff_id),
    CONSTRAINT ck_vapp_status CHECK
        (current_status IN
         ('DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'PROCESSING',
          'EMBASSY', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_vapp_fee CHECK (service_fee_snapshot >= 0)
);

CREATE TABLE application_details (
    application_id       NUMBER(10) NOT NULL,
    passport_number      VARCHAR2(50),
    passport_issue_date  DATE,
    passport_expiry_date DATE,
    date_of_birth        DATE,
    gender               VARCHAR2(20),
    marital_status       VARCHAR2(30),
    occupation           VARCHAR2(100),
    residential_address  VARCHAR2(500),
    travel_purpose       VARCHAR2(250),
    expected_arrival_date DATE,
    additional_info      CLOB,
    created_at           TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at           TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_app_details PRIMARY KEY (application_id),
    CONSTRAINT fk_appdet_app FOREIGN KEY (application_id)
        REFERENCES visa_application (application_id),
    CONSTRAINT ck_passport_dates CHECK
        (passport_expiry_date IS NULL OR passport_issue_date IS NULL
         OR passport_expiry_date > passport_issue_date)
);

CREATE TABLE application_document (
    document_id          NUMBER(10) NOT NULL,
    application_id       NUMBER(10) NOT NULL,
    checklist_item_id    NUMBER(10),
    file_name            VARCHAR2(255) NOT NULL,
    file_type            VARCHAR2(100),
    file_path            VARCHAR2(1000) NOT NULL,
    upload_date          TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    uploaded_by_user_id  NUMBER(10),
    review_status        VARCHAR2(25) DEFAULT 'PENDING' NOT NULL,
    reviewed_by_staff_id NUMBER(10),
    reviewed_at          TIMESTAMP,
    review_note          VARCHAR2(1000),
    CONSTRAINT pk_application_doc PRIMARY KEY (document_id),
    CONSTRAINT fk_doc_application FOREIGN KEY (application_id)
        REFERENCES visa_application (application_id),
    CONSTRAINT fk_doc_check_item FOREIGN KEY (checklist_item_id)
        REFERENCES checklist_item (checklist_item_id),
    CONSTRAINT fk_doc_uploader FOREIGN KEY (uploaded_by_user_id)
        REFERENCES user_account (user_id),
    CONSTRAINT fk_doc_reviewer FOREIGN KEY (reviewed_by_staff_id)
        REFERENCES staff_profile (staff_id),
    CONSTRAINT ck_doc_review CHECK
        (review_status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'NEEDS_CORRECTION'))
);

CREATE TABLE application_status_history (
    status_id             NUMBER(10) NOT NULL,
    application_id        NUMBER(10) NOT NULL,
    status                VARCHAR2(30) NOT NULL,
    updated_at            TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_by_staff_id   NUMBER(10),
    status_note           VARCHAR2(1000),
    CONSTRAINT pk_app_status_hist PRIMARY KEY (status_id),
    CONSTRAINT fk_ash_application FOREIGN KEY (application_id)
        REFERENCES visa_application (application_id),
    CONSTRAINT fk_ash_staff FOREIGN KEY (updated_by_staff_id)
        REFERENCES staff_profile (staff_id),
    CONSTRAINT ck_ash_status CHECK
        (status IN
         ('DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'PROCESSING',
          'EMBASSY', 'APPROVED', 'REJECTED'))
);

CREATE TABLE payment_record (
    payment_id             NUMBER(10) NOT NULL,
    application_id         NUMBER(10) NOT NULL,
    service_fee            NUMBER(12,2) NOT NULL,
    currency_code          CHAR(3) NOT NULL,
    amount_paid            NUMBER(12,2) DEFAULT 0 NOT NULL,
    payment_date           TIMESTAMP,
    payment_status         VARCHAR2(20) DEFAULT 'PENDING' NOT NULL,
    invoice_number         VARCHAR2(60),
    payment_method         VARCHAR2(40),
    transaction_reference  VARCHAR2(120),
    recorded_by_staff_id   NUMBER(10),
    CONSTRAINT pk_payment_record PRIMARY KEY (payment_id),
    CONSTRAINT fk_payment_app FOREIGN KEY (application_id)
        REFERENCES visa_application (application_id),
    CONSTRAINT fk_payment_staff FOREIGN KEY (recorded_by_staff_id)
        REFERENCES staff_profile (staff_id),
    CONSTRAINT uq_payment_invoice UNIQUE (invoice_number),
    CONSTRAINT uq_payment_txn UNIQUE (transaction_reference),
    CONSTRAINT ck_payment_fee CHECK (service_fee >= 0),
    CONSTRAINT ck_payment_amount CHECK (amount_paid >= 0),
    CONSTRAINT ck_payment_status CHECK
        (payment_status IN ('PENDING', 'PARTIAL', 'PAID', 'FAILED', 'REFUNDED', 'CANCELLED'))
);

CREATE TABLE consultant_appointment (
    appointment_id        NUMBER(10) NOT NULL,
    applicant_id          NUMBER(10) NOT NULL,
    requested_datetime    TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    confirmed_datetime    TIMESTAMP,
    duration_minutes      NUMBER(4) DEFAULT 30 NOT NULL,
    meeting_link           VARCHAR2(1000),
    appointment_status     VARCHAR2(20) DEFAULT 'REQUESTED' NOT NULL,
    confirmed_by_staff_id  NUMBER(10),
    created_at             TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_consult_appt PRIMARY KEY (appointment_id),
    CONSTRAINT fk_appt_applicant FOREIGN KEY (applicant_id)
        REFERENCES applicant (applicant_id),
    CONSTRAINT fk_appt_staff FOREIGN KEY (confirmed_by_staff_id)
        REFERENCES staff_profile (staff_id),
    CONSTRAINT ck_appt_status CHECK
        (appointment_status IN ('REQUESTED', 'CONFIRMED', 'CANCELLED', 'COMPLETED')),
    CONSTRAINT ck_appt_duration CHECK (duration_minutes BETWEEN 1 AND 600)
);

CREATE TABLE academic_record (
    academic_record_id NUMBER(10) NOT NULL,
    applicant_id       NUMBER(10) NOT NULL,
    qualification      VARCHAR2(150) NOT NULL,
    result             VARCHAR2(100),
    graduation_year    NUMBER(4),
    created_at         TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_academic_record PRIMARY KEY (academic_record_id),
    CONSTRAINT fk_acad_applicant FOREIGN KEY (applicant_id)
        REFERENCES applicant (applicant_id),
    CONSTRAINT ck_acad_grad_year CHECK
        (graduation_year IS NULL OR graduation_year BETWEEN 1950 AND 2100),
    CONSTRAINT uq_acad_app_pair UNIQUE (academic_record_id, applicant_id)
);

CREATE TABLE university (
    university_id    NUMBER(10) NOT NULL,
    country_id       NUMBER(10) NOT NULL,
    university_name  VARCHAR2(200) NOT NULL,
    website          VARCHAR2(500),
    entry_criteria   CLOB,
    CONSTRAINT pk_university PRIMARY KEY (university_id),
    CONSTRAINT fk_univ_country FOREIGN KEY (country_id)
        REFERENCES country (country_id),
    CONSTRAINT uq_univ_country_name UNIQUE (country_id, university_name)
);

CREATE TABLE university_recommendation (
    recommendation_id  NUMBER(10) NOT NULL,
    applicant_id       NUMBER(10) NOT NULL,
    academic_record_id NUMBER(10) NOT NULL,
    suggested_at       TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    recommendation_note VARCHAR2(1000),
    CONSTRAINT pk_univ_recommend PRIMARY KEY (recommendation_id),
    CONSTRAINT fk_urec_applicant FOREIGN KEY (applicant_id)
        REFERENCES applicant (applicant_id),
    CONSTRAINT fk_urec_academic FOREIGN KEY (academic_record_id, applicant_id)
        REFERENCES academic_record (academic_record_id, applicant_id)
);

CREATE TABLE recommendation_university (
    recommendation_id NUMBER(10) NOT NULL,
    university_id     NUMBER(10) NOT NULL,
    rank_no           NUMBER(4),
    match_note        VARCHAR2(1000),
    CONSTRAINT pk_rec_university PRIMARY KEY (recommendation_id, university_id),
    CONSTRAINT fk_ru_recommend FOREIGN KEY (recommendation_id)
        REFERENCES university_recommendation (recommendation_id),
    CONSTRAINT fk_ru_university FOREIGN KEY (university_id)
        REFERENCES university (university_id),
    CONSTRAINT ck_ru_rank CHECK (rank_no IS NULL OR rank_no >= 1),
    CONSTRAINT uq_ru_rank UNIQUE (recommendation_id, rank_no)
);


CREATE SEQUENCE seq_country START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_visa_type START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_check_item START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_user_account START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_app_role START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_applicant START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_staff_profile START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_visa_rule START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_vapp START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_fee_hist START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_app_doc START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_status_hist START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_payment START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_consult_appt START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_academic START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_university START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_univ_recom START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE OR REPLACE TRIGGER trg_country_bi
BEFORE INSERT ON country FOR EACH ROW
BEGIN
    IF :NEW.country_id IS NULL THEN
        SELECT seq_country.NEXTVAL INTO :NEW.country_id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_vtype_bi
BEFORE INSERT ON visa_type FOR EACH ROW
BEGIN
    IF :NEW.visa_type_id IS NULL THEN
        SELECT seq_visa_type.NEXTVAL INTO :NEW.visa_type_id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_checkitem_bi
BEFORE INSERT ON checklist_item FOR EACH ROW
BEGIN
    IF :NEW.checklist_item_id IS NULL THEN
        SELECT seq_check_item.NEXTVAL INTO :NEW.checklist_item_id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_user_bi
BEFORE INSERT ON user_account FOR EACH ROW
BEGIN
    IF :NEW.user_id IS NULL THEN
        SELECT seq_user_account.NEXTVAL INTO :NEW.user_id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_role_bi
BEFORE INSERT ON app_role FOR EACH ROW
BEGIN
    IF :NEW.role_id IS NULL THEN
        SELECT seq_app_role.NEXTVAL INTO :NEW.role_id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_applicant_bi
BEFORE INSERT ON applicant FOR EACH ROW
BEGIN
    IF :NEW.applicant_id IS NULL THEN
        SELECT seq_applicant.NEXTVAL INTO :NEW.applicant_id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_staff_bi
BEFORE INSERT ON staff_profile FOR EACH ROW
BEGIN
    IF :NEW.staff_id IS NULL THEN
        SELECT seq_staff_profile.NEXTVAL INTO :NEW.staff_id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_vrule_bi
BEFORE INSERT ON visa_rule FOR EACH ROW
BEGIN
    IF :NEW.visa_rule_id IS NULL THEN
        SELECT seq_visa_rule.NEXTVAL INTO :NEW.visa_rule_id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_vapp_bi
BEFORE INSERT ON visa_application FOR EACH ROW
DECLARE
    v_app_nationality  NUMBER(10);
    v_rule_nationality NUMBER(10);
    v_fee              NUMBER(12,2);
    v_currency         CHAR(3);
    v_processing       VARCHAR2(100);
BEGIN
    IF :NEW.application_id IS NULL THEN
        SELECT seq_vapp.NEXTVAL INTO :NEW.application_id FROM dual;
    END IF;

    SELECT nationality_country_id
      INTO v_app_nationality
      FROM applicant
     WHERE applicant_id = :NEW.applicant_id;

    SELECT nationality_country_id, service_fee, currency_code, processing_time
      INTO v_rule_nationality, v_fee, v_currency, v_processing
      FROM visa_rule
     WHERE visa_rule_id = :NEW.visa_rule_id;

    IF v_app_nationality <> v_rule_nationality THEN
        RAISE_APPLICATION_ERROR(-20001,
          'Visa rule nationality does not match the applicant nationality.');
    END IF;

    IF :NEW.service_fee_snapshot IS NULL THEN
        :NEW.service_fee_snapshot := v_fee;
    END IF;
    IF :NEW.currency_code_snapshot IS NULL THEN
        :NEW.currency_code_snapshot := v_currency;
    END IF;
    IF :NEW.processing_time_snapshot IS NULL THEN
        :NEW.processing_time_snapshot := v_processing;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_feehist_bi
BEFORE INSERT ON visa_rule_fee_history FOR EACH ROW
BEGIN
    IF :NEW.fee_history_id IS NULL THEN
        SELECT seq_fee_hist.NEXTVAL INTO :NEW.fee_history_id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_doc_bi
BEFORE INSERT ON application_document FOR EACH ROW
BEGIN
    IF :NEW.document_id IS NULL THEN
        SELECT seq_app_doc.NEXTVAL INTO :NEW.document_id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_status_bi
BEFORE INSERT ON application_status_history FOR EACH ROW
BEGIN
    IF :NEW.status_id IS NULL THEN
        SELECT seq_status_hist.NEXTVAL INTO :NEW.status_id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_payment_bi
BEFORE INSERT ON payment_record FOR EACH ROW
DECLARE
    v_fee      NUMBER(12,2);
    v_currency CHAR(3);
BEGIN
    IF :NEW.payment_id IS NULL THEN
        SELECT seq_payment.NEXTVAL INTO :NEW.payment_id FROM dual;
    END IF;

    IF :NEW.service_fee IS NULL OR :NEW.currency_code IS NULL THEN
        SELECT service_fee_snapshot, currency_code_snapshot
          INTO v_fee, v_currency
          FROM visa_application
         WHERE application_id = :NEW.application_id;

        IF :NEW.service_fee IS NULL THEN
            :NEW.service_fee := v_fee;
        END IF;
        IF :NEW.currency_code IS NULL THEN
            :NEW.currency_code := v_currency;
        END IF;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_appt_bi
BEFORE INSERT ON consultant_appointment FOR EACH ROW
BEGIN
    IF :NEW.appointment_id IS NULL THEN
        SELECT seq_consult_appt.NEXTVAL INTO :NEW.appointment_id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_academic_bi
BEFORE INSERT ON academic_record FOR EACH ROW
BEGIN
    IF :NEW.academic_record_id IS NULL THEN
        SELECT seq_academic.NEXTVAL INTO :NEW.academic_record_id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_university_bi
BEFORE INSERT ON university FOR EACH ROW
BEGIN
    IF :NEW.university_id IS NULL THEN
        SELECT seq_university.NEXTVAL INTO :NEW.university_id FROM dual;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_urec_bi
BEFORE INSERT ON university_recommendation FOR EACH ROW
BEGIN
    IF :NEW.recommendation_id IS NULL THEN
        SELECT seq_univ_recom.NEXTVAL INTO :NEW.recommendation_id FROM dual;
    END IF;
END;
/



CREATE OR REPLACE TRIGGER trg_doc_check_match
BEFORE INSERT OR UPDATE OF application_id, checklist_item_id
ON application_document FOR EACH ROW
WHEN (NEW.checklist_item_id IS NOT NULL)
DECLARE
    v_match_count NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO v_match_count
      FROM visa_application a
      JOIN visa_rule_checklist vrc
        ON vrc.visa_rule_id = a.visa_rule_id
     WHERE a.application_id = :NEW.application_id
       AND vrc.checklist_item_id = :NEW.checklist_item_id;

    IF v_match_count = 0 THEN
        RAISE_APPLICATION_ERROR(-20002,
          'Checklist item is not part of this application visa rule.');
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_vrule_updated_bu
BEFORE UPDATE ON visa_rule FOR EACH ROW
BEGIN
    :NEW.last_updated := SYSTIMESTAMP;
END;
/

CREATE OR REPLACE TRIGGER trg_doc_review_bu
BEFORE UPDATE OF review_status ON application_document FOR EACH ROW
BEGIN
    IF :NEW.review_status <> :OLD.review_status
       AND :NEW.review_status <> 'PENDING' THEN
        :NEW.reviewed_at := SYSTIMESTAMP;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_user_updated_bu
BEFORE UPDATE ON user_account FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSTIMESTAMP;
END;
/

CREATE OR REPLACE TRIGGER trg_appdet_updated_bu
BEFORE UPDATE ON application_details FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSTIMESTAMP;
END;
/


CREATE OR REPLACE PROCEDURE create_visa_application (
    p_applicant_id   IN  NUMBER,
    p_visa_rule_id   IN  NUMBER,
    p_application_id OUT NUMBER
) AS
BEGIN
    INSERT INTO visa_application (applicant_id, visa_rule_id)
    VALUES (p_applicant_id, p_visa_rule_id)
    RETURNING application_id INTO p_application_id;

    INSERT INTO application_status_history
        (application_id, status, updated_at, status_note)
    VALUES
        (p_application_id, 'DRAFT', SYSTIMESTAMP, 'Initial application created');
END;
/

CREATE OR REPLACE PROCEDURE submit_visa_application (
    p_application_id IN NUMBER
) AS
    v_status       visa_application.current_status%TYPE;
    v_missing_cnt  NUMBER;
    v_rule_id      visa_application.visa_rule_id%TYPE;
BEGIN
    SELECT current_status, visa_rule_id
      INTO v_status, v_rule_id
      FROM visa_application
     WHERE application_id = p_application_id
     FOR UPDATE;

    IF v_status <> 'DRAFT' THEN
        RAISE_APPLICATION_ERROR(-20010,
          'Only a DRAFT application can be submitted.');
    END IF;

    SELECT COUNT(*)
      INTO v_missing_cnt
      FROM visa_rule_checklist vrc
     WHERE vrc.visa_rule_id = v_rule_id
       AND vrc.is_required = 'Y'
       AND NOT EXISTS (
           SELECT 1
             FROM application_document d
            WHERE d.application_id = p_application_id
              AND d.checklist_item_id = vrc.checklist_item_id
              AND d.document_id = (
                  SELECT MAX(d2.document_id)
                    FROM application_document d2
                   WHERE d2.application_id = p_application_id
                     AND d2.checklist_item_id = vrc.checklist_item_id
              )
              AND d.review_status NOT IN ('REJECTED', 'NEEDS_CORRECTION')
       );

    IF v_missing_cnt > 0 THEN
        RAISE_APPLICATION_ERROR(-20011,
          'Required documents are missing or need correction.');
    END IF;

    UPDATE visa_application
       SET current_status = 'SUBMITTED',
           submission_date = SYSTIMESTAMP,
           last_updated = SYSTIMESTAMP,
           last_updated_by_staff_id = NULL,
           last_status_note = 'Submitted by applicant'
     WHERE application_id = p_application_id;

    INSERT INTO application_status_history
        (application_id, status, updated_at, status_note)
    VALUES
        (p_application_id, 'SUBMITTED', SYSTIMESTAMP, 'Submitted by applicant');
END;
/

CREATE OR REPLACE PROCEDURE set_visa_app_status (
    p_application_id       IN NUMBER,
    p_new_status           IN VARCHAR2,
    p_updated_by_staff_id  IN NUMBER DEFAULT NULL,
    p_status_note          IN VARCHAR2 DEFAULT NULL
) AS
    v_old_status visa_application.current_status%TYPE;
    v_new_status VARCHAR2(30);
BEGIN
    v_new_status := UPPER(TRIM(p_new_status));

    IF v_new_status = 'SUBMITTED' THEN
        RAISE_APPLICATION_ERROR(-20013,
          'Use SUBMIT_VISA_APPLICATION to submit an application.');
    END IF;

    SELECT current_status
      INTO v_old_status
      FROM visa_application
     WHERE application_id = p_application_id
     FOR UPDATE;

    IF v_new_status <> v_old_status THEN
        UPDATE visa_application
           SET current_status = v_new_status,
               submission_date = CASE
                   WHEN v_new_status = 'SUBMITTED' AND submission_date IS NULL
                   THEN SYSTIMESTAMP ELSE submission_date END,
               last_updated = SYSTIMESTAMP,
               last_updated_by_staff_id = p_updated_by_staff_id,
               last_status_note = p_status_note
         WHERE application_id = p_application_id;

        INSERT INTO application_status_history
            (application_id, status, updated_at,
             updated_by_staff_id, status_note)
        VALUES
            (p_application_id, v_new_status, SYSTIMESTAMP,
             p_updated_by_staff_id, p_status_note);
    END IF;
END;
/

CREATE OR REPLACE PROCEDURE update_visa_rule_fee (
    p_visa_rule_id       IN NUMBER,
    p_new_service_fee    IN NUMBER,
    p_new_currency_code  IN CHAR,
    p_changed_by_user_id IN NUMBER DEFAULT NULL,
    p_change_note        IN VARCHAR2 DEFAULT NULL
) AS
    v_old_fee      visa_rule.service_fee%TYPE;
    v_old_currency visa_rule.currency_code%TYPE;
    v_new_currency CHAR(3);
BEGIN
    IF p_new_service_fee IS NULL OR p_new_service_fee < 0 THEN
        RAISE_APPLICATION_ERROR(-20020, 'Service fee must be zero or greater.');
    END IF;

    v_new_currency := UPPER(TRIM(p_new_currency_code));
    IF v_new_currency IS NULL OR LENGTH(v_new_currency) <> 3 THEN
        RAISE_APPLICATION_ERROR(-20021, 'Currency code must contain 3 characters.');
    END IF;

    SELECT service_fee, currency_code
      INTO v_old_fee, v_old_currency
      FROM visa_rule
     WHERE visa_rule_id = p_visa_rule_id
     FOR UPDATE;

    UPDATE visa_rule
       SET service_fee = p_new_service_fee,
           currency_code = v_new_currency,
           updated_by_user_id = p_changed_by_user_id,
           update_note = p_change_note
     WHERE visa_rule_id = p_visa_rule_id;

    IF v_old_fee <> p_new_service_fee OR v_old_currency <> v_new_currency THEN
        INSERT INTO visa_rule_fee_history
            (visa_rule_id, old_service_fee, new_service_fee,
             old_currency_code, new_currency_code, changed_at,
             changed_by_user_id, change_note)
        VALUES
            (p_visa_rule_id, v_old_fee, p_new_service_fee,
             v_old_currency, v_new_currency, SYSTIMESTAMP,
             p_changed_by_user_id, p_change_note);
    END IF;
END;
/


CREATE INDEX ix_urole_role ON user_role (role_id);
CREATE INDEX ix_urole_assigner ON user_role (assigned_by_user_id);
CREATE INDEX ix_app_nation ON applicant (nationality_country_id);
CREATE INDEX ix_vrule_search ON visa_rule
    (destination_country_id, nationality_country_id, is_active);
CREATE INDEX ix_vrule_dest ON visa_rule (destination_country_id);
CREATE INDEX ix_vrule_nat ON visa_rule (nationality_country_id);
CREATE INDEX ix_vrule_type ON visa_rule (visa_type_id);
CREATE INDEX ix_vrule_user ON visa_rule (updated_by_user_id);
CREATE INDEX ix_vrc_item ON visa_rule_checklist (checklist_item_id);
CREATE INDEX ix_vrfh_rule ON visa_rule_fee_history (visa_rule_id, changed_at);
CREATE INDEX ix_vrfh_user ON visa_rule_fee_history (changed_by_user_id);
CREATE INDEX ix_vapp_applicant ON visa_application (applicant_id, application_date);
CREATE INDEX ix_vapp_rule ON visa_application (visa_rule_id);
CREATE INDEX ix_vapp_staff ON visa_application (last_updated_by_staff_id);
CREATE INDEX ix_vapp_status ON visa_application (current_status, application_date);
CREATE INDEX ix_doc_app ON application_document (application_id);
CREATE INDEX ix_doc_item ON application_document (checklist_item_id);
CREATE INDEX ix_doc_uploader ON application_document (uploaded_by_user_id);
CREATE INDEX ix_doc_reviewer ON application_document (reviewed_by_staff_id);
CREATE INDEX ix_ash_app_date ON application_status_history (application_id, updated_at);
CREATE INDEX ix_ash_staff ON application_status_history (updated_by_staff_id);
CREATE INDEX ix_payment_app ON payment_record (application_id);
CREATE INDEX ix_payment_staff ON payment_record (recorded_by_staff_id);
CREATE INDEX ix_appt_app_date ON consultant_appointment
    (applicant_id, requested_datetime);
CREATE INDEX ix_appt_staff ON consultant_appointment (confirmed_by_staff_id);
CREATE INDEX ix_acad_applicant ON academic_record (applicant_id);
CREATE INDEX ix_urec_applicant ON university_recommendation
    (applicant_id, suggested_at);
CREATE INDEX ix_ru_univ ON recommendation_university (university_id);


CREATE OR REPLACE VIEW vw_visa_catalog AS
SELECT
    vr.visa_rule_id,
    vt.visa_type_id,
    vt.type_name AS visa_type_name,
    dest.country_id AS destination_country_id,
    dest.country_name AS destination_country,
    nat.country_id AS nationality_country_id,
    nat.country_name AS applicant_nationality,
    vr.service_fee,
    vr.currency_code,
    vr.processing_time,
    vr.last_updated
FROM visa_rule vr
JOIN visa_type vt
  ON vt.visa_type_id = vr.visa_type_id
JOIN country dest
  ON dest.country_id = vr.destination_country_id
JOIN country nat
  ON nat.country_id = vr.nationality_country_id
WHERE vr.is_active = 'Y'
  AND vt.is_active = 'Y';

CREATE OR REPLACE VIEW vw_application_tracking AS
SELECT
    a.application_id,
    a.application_date,
    a.submission_date,
    a.current_status,
    ap.applicant_id,
    ua.full_name AS applicant_name,
    ua.email AS applicant_email,
    vt.type_name AS visa_type_name,
    dest.country_name AS destination_country,
    nat.country_name AS applicant_nationality,
    a.service_fee_snapshot AS service_fee,
    a.currency_code_snapshot AS currency_code,
    a.processing_time_snapshot AS processing_time
FROM visa_application a
JOIN applicant ap
  ON ap.applicant_id = a.applicant_id
JOIN user_account ua
  ON ua.user_id = ap.user_id
JOIN visa_rule vr
  ON vr.visa_rule_id = a.visa_rule_id
JOIN visa_type vt
  ON vt.visa_type_id = vr.visa_type_id
JOIN country dest
  ON dest.country_id = vr.destination_country_id
JOIN country nat
  ON nat.country_id = vr.nationality_country_id;


INSERT INTO app_role (role_name, description)
VALUES ('APPLICANT', 'Customer who searches and applies for visas');

INSERT INTO app_role (role_name, description)
VALUES ('AGENCY_STAFF', 'Agency staff who reviews applications and documents');

INSERT INTO app_role (role_name, description)
VALUES ('CONSULTANT', 'Consultant who manages consultation and case follow-up');

INSERT INTO app_role (role_name, description)
VALUES ('ADMIN', 'Administrator who manages users, roles and master data');

COMMIT;


PROMPT ------------------------------------------------------------
PROMPT Tables created:
PROMPT ------------------------------------------------------------
SELECT table_name FROM user_tables ORDER BY table_name;

PROMPT ------------------------------------------------------------
PROMPT Views created:
PROMPT ------------------------------------------------------------
SELECT view_name FROM user_views ORDER BY view_name;

PROMPT ------------------------------------------------------------
PROMPT Invalid objects (the result should be empty):
PROMPT ------------------------------------------------------------
SELECT object_name, object_type, status
  FROM user_objects
 WHERE status <> 'VALID'
 ORDER BY object_type, object_name;

PROMPT ------------------------------------------------------------
PROMPT Starter roles:
PROMPT ------------------------------------------------------------
SELECT role_id, role_name FROM app_role ORDER BY role_id;

PROMPT ============================================================
PROMPT Installation script completed. Review Script Output/errors.
PROMPT ============================================================
