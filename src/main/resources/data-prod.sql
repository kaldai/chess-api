-- Тестовые пользователи
INSERT INTO players (username, email, password, classical_rating, rapid_rating, blitz_rating)
VALUES ('alice', 'alice@example.com', 'password', 1500, 1450, 1400),
       ('bob', 'bob@example.com', 'password', 1400, 1350, 1300),
       ('charlie', 'charlie@example.com', 'password', 1300, 1250, 1200)
ON CONFLICT (username) DO NOTHING;