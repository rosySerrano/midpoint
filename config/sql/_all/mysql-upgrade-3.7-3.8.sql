SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE m_audit_event
  MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE m_audit_prop_value
  MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE m_audit_ref_value
  MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;

SET FOREIGN_KEY_CHECKS = 1;

DROP TABLE m_exclusion;
