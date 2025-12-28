CREATE TABLE IF NOT EXISTS players
(
    id               BIGSERIAL PRIMARY KEY,
    username         VARCHAR(50) UNIQUE  NOT NULL,
    email            VARCHAR(255) UNIQUE NOT NULL,
    password         VARCHAR(255)        NOT NULL,
    classical_rating INTEGER   DEFAULT 1200,
    rapid_rating     INTEGER   DEFAULT 1200,
    blitz_rating     INTEGER   DEFAULT 1200,
    games_played     INTEGER   DEFAULT 0,
    games_won        INTEGER   DEFAULT 0,
    games_drawn      INTEGER   DEFAULT 0,
    games_lost       INTEGER   DEFAULT 0,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at    TIMESTAMP
);

CREATE TABLE IF NOT EXISTS games
(
    id              BIGSERIAL PRIMARY KEY,
    white_player_id BIGINT      NOT NULL REFERENCES players (id),
    black_player_id BIGINT REFERENCES players (id),
    game_type       VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    result          VARCHAR(20),
    initial_fen     TEXT                 DEFAULT 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
    current_fen     TEXT                 DEFAULT 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
    pgn             TEXT,
    time_control    INTEGER              DEFAULT 600,
    time_increment  INTEGER              DEFAULT 0,
    white_time_left INTEGER,
    black_time_left INTEGER,
    created_at      TIMESTAMP            DEFAULT CURRENT_TIMESTAMP,
    started_at      TIMESTAMP,
    finished_at     TIMESTAMP
);

CREATE TABLE IF NOT EXISTS moves
(
    id              BIGSERIAL PRIMARY KEY,
    game_id         BIGINT  NOT NULL REFERENCES games (id),
    move_number     INTEGER NOT NULL,
    from_square     VARCHAR(10),
    to_square       VARCHAR(10),
    promotion       VARCHAR(1),
    san             VARCHAR(10),
    white_time_left INTEGER,
    black_time_left INTEGER,
    timestamp       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);