DELETE FROM url_check;
DELETE FROM url;
ALTER TABLE url_check ALTER COLUMN id RESTART WITH 2;
ALTER TABLE url ALTER COLUMN id RESTART WITH 2;
