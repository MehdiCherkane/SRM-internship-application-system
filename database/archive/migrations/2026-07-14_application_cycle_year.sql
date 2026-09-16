-- Adds a cycle_year column to support DB-level uniqueness for "one active application per applicant per cycle".
-- Nullable: drafts have cycle_year = NULL and do NOT participate in the unique index.
-- The unique index itself is added as a separate step AFTER verifying no duplicates exist.

ALTER TABLE applications
    ADD COLUMN cycle_year INT NULL;

-- Backfill cycle_year for already-submitted rows (anything with a non-null submitted_date).
UPDATE applications
SET cycle_year = YEAR(submitted_date)
WHERE submitted_date IS NOT NULL
  AND cycle_year IS NULL;

-- Find applicants with more than one submitted application in the same cycle_year.
-- If this query returns rows, you MUST resolve duplicates BEFORE adding the unique index.
-- Otherwise the index creation fails.
SELECT applicant_id, cycle_year, COUNT(*) AS dupes
FROM applications
WHERE cycle_year IS NOT NULL AND cycle_year <> 0
GROUP BY applicant_id, cycle_year
HAVING COUNT(*) > 1;
