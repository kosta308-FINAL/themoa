SET @themoa_schema_name := DATABASE();
SET @themoa_has_member := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = @themoa_schema_name
      AND TABLE_NAME = 'member'
);
SET @themoa_has_member_created_at := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @themoa_schema_name
      AND TABLE_NAME = 'member'
      AND COLUMN_NAME = 'created_at'
);

SET @themoa_add_member_created_at_sql := IF(
    @themoa_has_member = 1 AND @themoa_has_member_created_at = 0,
    'ALTER TABLE member ADD COLUMN created_at datetime(6) NULL',
    'SELECT 1'
);
PREPARE themoa_add_member_created_at_stmt FROM @themoa_add_member_created_at_sql;
EXECUTE themoa_add_member_created_at_stmt;
DEALLOCATE PREPARE themoa_add_member_created_at_stmt;

SET @themoa_old_sql_mode := @@SESSION.sql_mode;
SET SESSION sql_mode = REPLACE(REPLACE(@@SESSION.sql_mode, 'NO_ZERO_DATE', ''), 'NO_ZERO_IN_DATE', '');

SET @themoa_backfill_member_created_at_sql := IF(
    @themoa_has_member = 1,
    'UPDATE member SET created_at = CURRENT_TIMESTAMP(6) WHERE created_at IS NULL OR created_at = ''0000-00-00 00:00:00''',
    'SELECT 1'
);
PREPARE themoa_backfill_member_created_at_stmt FROM @themoa_backfill_member_created_at_sql;
EXECUTE themoa_backfill_member_created_at_stmt;
DEALLOCATE PREPARE themoa_backfill_member_created_at_stmt;

SET SESSION sql_mode = @themoa_old_sql_mode;

SET @themoa_require_member_created_at_sql := IF(
    @themoa_has_member = 1,
    'ALTER TABLE member MODIFY COLUMN created_at datetime(6) NOT NULL',
    'SELECT 1'
);
PREPARE themoa_require_member_created_at_stmt FROM @themoa_require_member_created_at_sql;
EXECUTE themoa_require_member_created_at_stmt;
DEALLOCATE PREPARE themoa_require_member_created_at_stmt;
