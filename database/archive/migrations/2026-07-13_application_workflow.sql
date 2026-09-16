-- Run once against the MariaDB schema shown in the project discussion.
-- Your table already has `cni`, so this migration does not add it again.
-- Draft records are allowed to be incomplete; the application enforces CIN on submission.

ALTER TABLE applications
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    MODIFY COLUMN cni VARCHAR(30) NULL,
    MODIFY COLUMN first_name VARCHAR(255) NULL,
    MODIFY COLUMN last_name VARCHAR(255) NULL,
    MODIFY COLUMN email VARCHAR(255) NULL,
    MODIFY COLUMN phone VARCHAR(255) NULL,
    MODIFY COLUMN university VARCHAR(255) NULL,
    MODIFY COLUMN major VARCHAR(255) NULL,
    MODIFY COLUMN submitted_date DATETIME(6) NULL;

-- Keep PENDING temporarily so existing values remain valid during conversion.
ALTER TABLE applications
    MODIFY COLUMN status ENUM('DRAFT', 'PENDING', 'SUBMITTED', 'UNDER_REVIEW', 'ACCEPTED', 'REJECTED') NOT NULL;

UPDATE applications SET status = 'SUBMITTED' WHERE status = 'PENDING';

ALTER TABLE applications
    MODIFY COLUMN status ENUM('DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'ACCEPTED', 'REJECTED') NOT NULL;
