-- Repair the sample-account hashes only when they still contain the original V2 seed values.
-- This leaves any password changed after V2 untouched.
UPDATE app_user
SET password_hash = '$2a$10$gaQRR.3Bbg.bs0REIyUzueDZkqZlfmUVYM0tZjnAw0modUgVO9kvq'
WHERE username = 'admin'
  AND password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lihO';

UPDATE app_user
SET password_hash = '$2a$10$cY5psd0Op5xB9Yk1z3xUaOmkHIRPEQTd1xTlyRT5C9oYGkoIsE3AK'
WHERE username = 'alice'
  AND password_hash = '$2a$10$TLbNhBeMayBiMDXPdnBfT.kA4tgzSBJXKYa0ohtGFq4sEXDEJFNZK';

UPDATE app_user
SET password_hash = '$2a$10$sBLKjIWUmuR44RicG9xvJu5beR0r2E9.8DByv.we7pplWyL9FYMvW'
WHERE username = 'hrmanager'
  AND password_hash = '$2a$10$DO0Kz9UKvj5qDHKsJoQRde7qSg5yX8YoHh6IfZ30cJB4xzIGELj2q';
