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

-- Таблица приглашений на игру
CREATE TABLE IF NOT EXISTS invites
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID                 DEFAULT gen_random_uuid(),
    sender_id         BIGINT      NOT NULL REFERENCES players (id) ON DELETE CASCADE,
    receiver_id       BIGINT      NOT NULL REFERENCES players (id) ON DELETE CASCADE,
    game_type         VARCHAR(20) NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    -- Параметры игры
    time_control      INTEGER,
    time_increment    INTEGER,
    is_rated          BOOLEAN              DEFAULT TRUE,

    -- Ссылка на созданную игру (если принято)
    game_id           BIGINT      REFERENCES games (id) ON DELETE SET NULL,

    -- Временные метки
    sent_at           TIMESTAMP            DEFAULT CURRENT_TIMESTAMP,
    expires_at        TIMESTAMP            DEFAULT (CURRENT_TIMESTAMP + INTERVAL '7 days'),
    responded_at      TIMESTAMP,

    -- Дополнительная информация
    message           TEXT,
    custom_parameters JSONB,

    -- Ограничения
    CONSTRAINT fk_sender FOREIGN KEY (sender_id) REFERENCES players (id),
    CONSTRAINT fk_receiver FOREIGN KEY (receiver_id) REFERENCES players (id),
    CONSTRAINT fk_game FOREIGN KEY (game_id) REFERENCES games (id),
    CONSTRAINT chk_status CHECK (status IN (
                                            'PENDING', 'ACCEPTED', 'REJECTED',
                                            'EXPIRED', 'CANCELLED', 'WITHDRAWN'
        )),
    CONSTRAINT chk_not_self_invite CHECK (sender_id != receiver_id),
    CONSTRAINT chk_time_control CHECK (
        time_control IS NULL OR time_control BETWEEN 60 AND 10800
        ),
    CONSTRAINT chk_time_increment CHECK (
        time_increment IS NULL OR time_increment BETWEEN 0 AND 60
        )
);

-- Индексы для производительности
CREATE INDEX IF NOT EXISTS idx_invites_receiver_status
    ON invites (receiver_id, status) WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_invites_sender_status
    ON invites (sender_id, status);

CREATE INDEX IF NOT EXISTS idx_invites_game_id
    ON invites (game_id) WHERE game_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_invites_expires
    ON invites (expires_at) WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_invites_uuid
    ON invites (uuid);

-- Комментарии к таблице и колонкам
COMMENT ON TABLE invites IS 'Таблица приглашений на шахматные игры между игроками';
COMMENT ON COLUMN invites.uuid IS 'Публичный UUID для ссылок на приглашение';
COMMENT ON COLUMN invites.sender_id IS 'ID отправителя приглашения';
COMMENT ON COLUMN invites.receiver_id IS 'ID получателя приглашения';
COMMENT ON COLUMN invites.game_type IS 'Тип игры: CLASSICAL, RAPID, BLITZ';
COMMENT ON COLUMN invites.status IS 'Статус приглашения: PENDING, ACCEPTED, REJECTED и т.д.';
COMMENT ON COLUMN invites.time_control IS 'Контроль времени в секундах';
COMMENT ON COLUMN invites.time_increment IS 'Добавление времени в секундах';
COMMENT ON COLUMN invites.is_rated IS 'Рейтинговая ли игра';
COMMENT ON COLUMN invites.game_id IS 'ID созданной игры (если приглашение принято)';
COMMENT ON COLUMN invites.expires_at IS 'Дата истечения приглашения';
COMMENT ON COLUMN invites.message IS 'Дополнительное сообщение от отправителя';