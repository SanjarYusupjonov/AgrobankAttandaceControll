-- =====================================================================
-- 1. DEPARTMENT
-- =====================================================================
CREATE TABLE IF NOT EXISTS department
(
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL CONSTRAINT uk_department_name UNIQUE
);

-- =====================================================================
-- 2. ATTENDANCE_INFO
-- =====================================================================
CREATE TABLE IF NOT EXISTS attendance_info
(
    id            BIGSERIAL,
    name          VARCHAR(255),
    time          TIMESTAMP,
    card_reader   VARCHAR(255),
    department_id BIGINT REFERENCES department (id),
    CONSTRAINT uq_name_time UNIQUE (name, time)
);

-- =====================================================================
-- 3. ATTENDANCE_INFO_CLEAN
-- =====================================================================
CREATE TABLE IF NOT EXISTS attendance_info_clean
(
    id            BIGINT,
    name          VARCHAR(255),
    time          TIMESTAMP,
    card_reader   VARCHAR(255),
    department_id BIGINT REFERENCES department (id),
    CONSTRAINT uq_clean_name_time UNIQUE (name, time)
);

-- =====================================================================
-- 4. FUNCTION: berilgan (name, date) uchun clean ni qayta hisoblash
-- =====================================================================
CREATE OR REPLACE FUNCTION refresh_clean_for_name_date(p_name VARCHAR, p_date DATE)
    RETURNS VOID AS
$$
BEGIN
    DELETE
    FROM attendance_info_clean
    WHERE name = p_name
      AND DATE(time) = p_date;

    INSERT INTO attendance_info_clean (id, name, time, card_reader, department_id)
    WITH ordered AS (SELECT *,
                            LAG(card_reader) OVER (
                                PARTITION BY name, DATE(time)
                                ORDER BY time
                                ) AS prev_status
                     FROM attendance_info
                     WHERE name = p_name
                       AND DATE(time) = p_date),
         grouped AS (SELECT *,
                            SUM(
                                    CASE
                                        WHEN prev_status IS NULL OR prev_status <> card_reader
                                            THEN 1
                                        ELSE 0
                                        END
                            ) OVER (
                                PARTITION BY name, DATE(time)
                                ORDER BY time
                                ) AS grp
                     FROM ordered),
         ranked AS (SELECT *,
                           ROW_NUMBER() OVER (
                               PARTITION BY name, DATE(time), grp
                               ORDER BY time DESC
                               ) AS rn
                    FROM grouped)
    SELECT id, name, time, card_reader, department_id
    FROM ranked
    WHERE rn = 1
    ORDER BY name, time
    ON CONFLICT (name, time) DO UPDATE
        SET card_reader   = EXCLUDED.card_reader,
            department_id = EXCLUDED.department_id,
            id            = EXCLUDED.id;
END;
$$ LANGUAGE plpgsql;

-- =====================================================================
-- 5. TRIGGER FUNCTION
-- =====================================================================
CREATE OR REPLACE FUNCTION trg_sync_attendance_clean()
    RETURNS TRIGGER AS
$$
BEGIN
    IF TG_OP = 'DELETE' THEN
        PERFORM refresh_clean_for_name_date(OLD.name, DATE(OLD.time));
        RETURN OLD;
    ELSE
        PERFORM refresh_clean_for_name_date(NEW.name, DATE(NEW.time));
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- =====================================================================
-- 6. TRIGGER — attendance_info ga bog'lash
-- =====================================================================
DROP TRIGGER IF EXISTS trg_attendance_info_sync ON attendance_info;

CREATE TRIGGER trg_attendance_info_sync
    AFTER INSERT OR UPDATE OR DELETE
    ON attendance_info
    FOR EACH ROW
EXECUTE FUNCTION trg_sync_attendance_clean();
