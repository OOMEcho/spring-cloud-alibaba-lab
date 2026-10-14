CREATE TABLE lab_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    nickname VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL
);
INSERT INTO lab_user (id, username, nickname, enabled) VALUES
    (1, 'demo', '演示用户', TRUE), (2, 'disabled', '禁用用户', FALSE);
