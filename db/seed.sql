-- Aegis Claims Platform — seed data.
-- Provides multiple members with claims/invoices so the claim-detail IDOR
-- (CWE-639) is demonstrable: e.g. member "bhopkins" (id 4471) can open claim
-- 90233 which belongs to "amorgan" (id 5583). See demo/trigger-artifact.md.
--
-- Passwords are salted BCrypt hashes (auth.PasswordHasher). Cleartext for the demo:
--   admin / admin123        (ADMIN)
--   jadjuster / letmein     (ADJUSTER)
--   amorgan / password      (MEMBER, id 5583)
--   bhopkins / password     (MEMBER, id 4471)
--   cwright / password      (MEMBER, id 6001)
--   dpatel / claims2015     (MEMBER, id 6002)
-- (BCrypt is salted, so each row's hash differs even where the cleartext matches.)

INSERT INTO users (id, username, password_hash, full_name, email, role) VALUES
    (1,    'admin',     '$2a$10$8Dvs/2yXHUvSBTgBWZGnP.htPBi//LSgtKuWFy9aN1gQ6czmy1oaq', 'System Administrator', 'admin@aegis.example',    'ADMIN'),
    (2,    'jadjuster', '$2a$10$qzJypEIVvqJiLhspiEm7nuIjcpGYwweDRdnTZIO11palHhtqsrg52', 'Jordan Adjuster',      'jordan@aegis.example',   'ADJUSTER'),
    (5583, 'amorgan',   '$2a$10$4xHi1urQbsq5EqLlMXDAIuU.ZfiQvNM2k2m2s0ntjiHscghLamgt6', 'Alex Morgan',          'alex.morgan@example.com','MEMBER'),
    (4471, 'bhopkins',  '$2a$10$HJrI9x5JLp4Zg806Ry76eOfJwcBIY7LRPKxuGqzoP02QZ6UB5Jnmy', 'Bailey Hopkins',       'bailey.h@example.com',   'MEMBER'),
    (6001, 'cwright',   '$2a$10$M8kL38kdIcy7zmwiIOv/KeIy5DB/6JGWw2JFFENHXjDQf1M1KevRq', 'Casey Wright',         'casey.w@example.com',    'MEMBER'),
    (6002, 'dpatel',    '$2a$10$59KCjCzOztmbrGfVKrn/..2ZXHc2QA2wTJh9y0IqGtQnEa/k8Lgvy', 'Devan Patel',          'devan.p@example.com',    'MEMBER');

INSERT INTO policies (id, policy_number, holder_user_id, product, status, premium_cents, effective_date, end_date) VALUES
    (7001, 'POL-2019-5583', 5583, 'PPO Family Health',       'ACTIVE', 48200, DATE '2019-01-01', NULL),
    (7002, 'POL-2018-4471', 4471, 'HMO Individual Health',   'ACTIVE', 31500, DATE '2018-06-01', NULL),
    (7003, 'POL-2020-6001', 6001, 'Short-Term Disability',   'ACTIVE', 12900, DATE '2020-03-15', NULL),
    (7004, 'POL-2017-6002', 6002, 'PPO Family Health',       'LAPSED', 48200, DATE '2017-01-01', DATE '2023-01-01');

-- Claims. Claim 90233 belongs to amorgan (5583) — the flagship IDOR target.
INSERT INTO claims (id, claim_number, policy_id, member_user_id, claim_type, status, amount_cents, approved_cents, diagnosis_code, adjudicator_notes, submitted_at, adjudicated_at) VALUES
    (90233, 'CLM-90233', 7001, 5583, 'MEDICAL',    'ADJUDICATED', 184500, 152000, 'J20.9', 'Approved at contracted rate.',        TIMESTAMP '2024-02-11 09:14:00', TIMESTAMP '2024-02-13 16:02:00'),
    (90234, 'CLM-90234', 7001, 5583, 'PHARMACY',   'PAID',        4200,   4200,   'Z79.4', 'Formulary drug, full allowance.',    TIMESTAMP '2024-03-02 11:20:00', TIMESTAMP '2024-03-03 10:00:00'),
    (90235, 'CLM-90235', 7001, 5583, 'DENTAL',     'SUBMITTED',   32000,  NULL,   'K02.9', NULL,                                  TIMESTAMP '2024-05-19 08:05:00', NULL),
    (90311, 'CLM-90311', 7002, 4471, 'MEDICAL',    'ADJUDICATED', 96000,  71000,  'M54.5', 'Partial denial: PT sessions capped.', TIMESTAMP '2024-01-28 14:40:00', TIMESTAMP '2024-01-31 09:30:00'),
    (90312, 'CLM-90312', 7002, 4471, 'VISION',     'DENIED',      21000,  0,      'H52.4', 'Out-of-network provider.',           TIMESTAMP '2024-04-06 13:15:00', TIMESTAMP '2024-04-08 12:00:00'),
    (90420, 'CLM-90420', 7003, 6001, 'DISABILITY', 'VALIDATED',   540000, NULL,   'S82.2', NULL,                                  TIMESTAMP '2024-06-01 10:10:00', NULL),
    (90421, 'CLM-90421', 7003, 6001, 'MEDICAL',    'SUBMITTED',   7800,   NULL,   'R51',   NULL,                                  TIMESTAMP '2024-06-22 07:45:00', NULL),
    (90555, 'CLM-90555', 7004, 6002, 'MEDICAL',    'DENIED',      250000, 0,      'I10',   'Policy lapsed at date of service.',  TIMESTAMP '2024-02-02 09:00:00', TIMESTAMP '2024-02-05 15:20:00');

INSERT INTO claim_lines (id, claim_id, service_code, description, billed_cents, allowed_cents) VALUES
    (1, 90233, '99213', 'Office visit, established patient',     18000,  15200),
    (2, 90233, '71046', 'Chest X-ray, 2 views',                  90000,  74000),
    (3, 90233, '85025', 'Complete blood count',                  76500,  62800),
    (4, 90234, 'RX0042','Generic maintenance medication',        4200,   4200),
    (5, 90311, '97110', 'Therapeutic exercises, 4 units',        64000,  47000),
    (6, 90311, '99214', 'Office visit, moderate complexity',     32000,  24000),
    (7, 90420, 'DIS-01','Short-term disability, week 1-4',      540000,  NULL),
    (8, 90555, '93000', 'Electrocardiogram, complete',          250000,  0);

INSERT INTO invoices (id, invoice_number, policy_id, member_user_id, amount_cents, status, due_date, created_at) VALUES
    (8001, 'INV-8001', 7001, 5583, 48200, 'PAID',    DATE '2024-06-01', TIMESTAMP '2024-05-15 00:00:00'),
    (8002, 'INV-8002', 7001, 5583, 48200, 'OPEN',    DATE '2024-07-01', TIMESTAMP '2024-06-15 00:00:00'),
    (8003, 'INV-8003', 7002, 4471, 31500, 'OVERDUE', DATE '2024-05-01', TIMESTAMP '2024-04-15 00:00:00'),
    (8004, 'INV-8004', 7002, 4471, 31500, 'OPEN',    DATE '2024-07-01', TIMESTAMP '2024-06-15 00:00:00'),
    (8005, 'INV-8005', 7003, 6001, 12900, 'OPEN',    DATE '2024-07-01', TIMESTAMP '2024-06-15 00:00:00'),
    (8006, 'INV-8006', 7004, 6002, 48200, 'VOID',    DATE '2023-01-01', TIMESTAMP '2022-12-15 00:00:00');

INSERT INTO payments (id, invoice_id, amount_cents, method, status, external_ref, created_at) VALUES
    (9001, 8001, 48200, 'ACH',  'SETTLED', 'PMT-EXT-771201', TIMESTAMP '2024-05-28 00:00:00'),
    (9002, 8003, 15000, 'CARD', 'SETTLED', 'PMT-EXT-771455', TIMESTAMP '2024-05-10 00:00:00'),
    (9003, 8003, 16500, 'CARD', 'FAILED',  'PMT-EXT-771499', TIMESTAMP '2024-05-11 00:00:00');

INSERT INTO documents (id, claim_id, owner_user_id, filename, stored_path, content_type, uploaded_at) VALUES
    (5001, 90233, 5583, 'eob-90233.pdf',       'eob-90233.pdf',       'application/pdf', TIMESTAMP '2024-02-13 16:05:00'),
    (5002, 90311, 4471, 'referral-90311.pdf',  'referral-90311.pdf',  'application/pdf', TIMESTAMP '2024-01-28 14:41:00');
