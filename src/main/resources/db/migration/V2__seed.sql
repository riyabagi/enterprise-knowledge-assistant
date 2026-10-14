-- ============================================================
-- V2__seed.sql  Seed data: users, policies, sample documents
-- ============================================================

-- ─── Users (bcrypt, cost 10) ─────────────────────────────────
-- Passwords: admin123, alice123, hrm123
INSERT INTO app_user (username, password_hash, full_name, email, role) VALUES
('admin',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lihO', 'System Administrator', 'admin@enterprise.local',     'ADMIN'),
('alice',     '$2a$10$TLbNhBeMayBiMDXPdnBfT.kA4tgzSBJXKYa0ohtGFq4sEXDEJFNZK', 'Alice Johnson',        'alice@enterprise.local',     'EMPLOYEE'),
('hrmanager', '$2a$10$DO0Kz9UKvj5qDHKsJoQRde7qSg5yX8YoHh6IfZ30cJB4xzIGELj2q', 'HR Manager',           'hrmanager@enterprise.local', 'HR');

-- ─── Access Policies ─────────────────────────────────────────
INSERT INTO access_policy (role, access_level) VALUES
('ADMIN',    'PUBLIC'),
('ADMIN',    'INTERNAL'),
('ADMIN',    'HR_ONLY'),
('ADMIN',    'RESTRICTED'),
('EMPLOYEE', 'PUBLIC'),
('EMPLOYEE', 'INTERNAL'),
('HR',       'PUBLIC'),
('HR',       'INTERNAL'),
('HR',       'HR_ONLY');

-- ─── Sample documents ────────────────────────────────────────
-- We insert the document first without current_version_id, then add the version and update

INSERT INTO knowledge_document (id, title, description, category, access_level, owner_id, status)
VALUES
(1, 'Company Remote Work Policy',
 'Guidelines for remote work arrangements including eligibility, equipment, and security requirements.',
 'HR', 'INTERNAL', 1, 'INDEXED'),
(2, 'IT Security Guidelines',
 'Mandatory security practices: password policy, VPN usage, device management, and incident reporting.',
 'IT', 'INTERNAL', 1, 'INDEXED'),
(3, 'Employee Onboarding Handbook',
 'Step-by-step onboarding guide covering first-day checklist, system access, and HR policies.',
 'GENERAL', 'PUBLIC', 1, 'INDEXED'),
(4, 'Salary Bands and Compensation Review Process',
 'Confidential HR document detailing salary bands by level and the annual review process.',
 'HR', 'HR_ONLY', 1, 'INDEXED'),
(5, 'Q3 2024 IT Infrastructure Report',
 'Internal report on server uptime, incident count, and planned upgrades for Q4.',
 'IT', 'INTERNAL', 1, 'INDEXED');

INSERT INTO document_version
    (id, document_id, version_number, original_filename, content_type, file_size_bytes, checksum, status, created_by_id)
VALUES
(1, 1, 1, 'remote-work-policy.txt',        'text/plain', 1800, 'abc001', 'PUBLISHED', 1),
(2, 2, 1, 'it-security-guidelines.txt',    'text/plain', 2200, 'abc002', 'PUBLISHED', 1),
(3, 3, 1, 'onboarding-handbook.txt',       'text/plain', 3500, 'abc003', 'PUBLISHED', 1),
(4, 4, 1, 'salary-bands-confidential.txt', 'text/plain', 1200, 'abc004', 'PUBLISHED', 1),
(5, 5, 1, 'q3-it-infra-report.txt',        'text/plain', 2800, 'abc005', 'PUBLISHED', 1);

UPDATE knowledge_document SET current_version_id = 1 WHERE id = 1;
UPDATE knowledge_document SET current_version_id = 2 WHERE id = 2;
UPDATE knowledge_document SET current_version_id = 3 WHERE id = 3;
UPDATE knowledge_document SET current_version_id = 4 WHERE id = 4;
UPDATE knowledge_document SET current_version_id = 5 WHERE id = 5;

-- ─── Sample chunks (pre-extracted for immediate search testing) ───
INSERT INTO knowledge_chunk (document_version_id, chunk_index, section_reference, content, searchable_text) VALUES
(1, 0, 'Section 1 – Eligibility', 'All full-time employees who have completed their 90-day probationary period are eligible to apply for remote work arrangements. Part-time and contract staff are not eligible. Remote work requires manager approval and HR sign-off.',
 'remote work eligibility probationary period manager approval'),
(1, 1, 'Section 2 – Equipment', 'The company provides a laptop and VPN access to approved remote workers. Employees must use company-issued equipment for all work activities. Personal computers must not be used to access internal systems.',
 'remote work equipment laptop VPN company-issued personal computer'),
(2, 0, 'Section 1 – Password Policy', 'Passwords must be at least 12 characters, include uppercase, lowercase, a digit, and a special character. Passwords must be changed every 90 days. Do not reuse the last 10 passwords. Use the company-approved password manager.',
 'password policy security 12 characters expiry password manager'),
(2, 1, 'Section 2 – VPN Usage', 'All remote access to internal systems must use the company VPN. VPN credentials must not be shared. Disconnect from VPN when not actively working. Report any VPN issues to the IT helpdesk immediately.',
 'VPN remote access internal systems security helpdesk'),
(3, 0, 'Chapter 1 – Your First Day', 'On your first day, report to reception to collect your badge. You will meet your buddy mentor and attend a two-hour Welcome Orientation. IT will configure your laptop and provide credentials for email, Slack, and internal systems.',
 'onboarding first day badge mentor orientation IT laptop credentials'),
(3, 1, 'Chapter 2 – Benefits Enrollment', 'New employees have 30 days to enroll in health, dental, and vision benefits. Log in to the HR portal at hr.enterprise.local to complete enrollment. Contact hrmanager@enterprise.local with questions.',
 'onboarding benefits enrollment health dental vision HR portal'),
(4, 0, 'Salary Bands – Level 1 to 3', 'Level 1 (Junior): $55,000–$75,000. Level 2 (Mid): $75,000–$100,000. Level 3 (Senior): $100,000–$140,000. Bands are reviewed annually in Q4. This document is confidential and restricted to HR personnel.',
 'salary band compensation level junior mid senior confidential HR'),
(5, 0, 'Q3 2024 Highlights', 'Server uptime for Q3 2024 averaged 99.7%. There were 3 P1 incidents: a network outage on July 12 (resolved in 4h), a storage failure on August 6 (resolved in 2h), and a security patch window on September 20 (planned downtime).',
 'Q3 2024 uptime incidents network outage storage failure security patch');
