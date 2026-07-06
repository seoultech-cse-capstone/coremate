SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS subject (
    subject_id BIGINT NOT NULL AUTO_INCREMENT,
    subject_code VARCHAR(50) NOT NULL,
    subject_name VARCHAR(100) NOT NULL,
    subject_name_en VARCHAR(100) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (subject_id),
    CONSTRAINT uq_subject_subject_code UNIQUE (subject_code),
    CONSTRAINT uq_subject_subject_name UNIQUE (subject_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS category_lv1 (
    category_lv1_id BIGINT NOT NULL AUTO_INCREMENT,
    subject_id BIGINT NOT NULL,
    category_lv1_code VARCHAR(50) NOT NULL,
    category_lv1_name VARCHAR(100) NOT NULL,
    category_lv1_name_en VARCHAR(100) NULL,
    description TEXT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (category_lv1_id),
    CONSTRAINT uq_category_lv1_code UNIQUE (subject_id, category_lv1_code),
    CONSTRAINT uq_category_lv1_name UNIQUE (subject_id, category_lv1_name),
    CONSTRAINT fk_category_lv1_subject_id
        FOREIGN KEY (subject_id) REFERENCES subject(subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS category_lv2 (
    category_lv2_id BIGINT NOT NULL AUTO_INCREMENT,
    category_lv1_id BIGINT NOT NULL,
    category_lv2_code VARCHAR(50) NOT NULL,
    category_lv2_name VARCHAR(100) NOT NULL,
    category_lv2_name_en VARCHAR(100) NULL,
    description TEXT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (category_lv2_id),
    CONSTRAINT uq_category_lv2_code UNIQUE (category_lv1_id, category_lv2_code),
    CONSTRAINT uq_category_lv2_name UNIQUE (category_lv1_id, category_lv2_name),
    CONSTRAINT fk_category_lv2_category_lv1_id
        FOREIGN KEY (category_lv1_id) REFERENCES category_lv1(category_lv1_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS concept (
    concept_id BIGINT NOT NULL AUTO_INCREMENT,
    concept_code VARCHAR(100) NOT NULL,
    category_lv2_id BIGINT NOT NULL,
    concept_name VARCHAR(150) NOT NULL,
    concept_name_en VARCHAR(150) NULL,
    aliases_json JSON NULL,
    one_line_definition TEXT NOT NULL,
    core_summary TEXT NOT NULL,
    core_keywords_json JSON NOT NULL,
    key_properties_json JSON NOT NULL,
    source_refs JSON NULL,
    sort_order INT NOT NULL DEFAULT 0,
    review_status VARCHAR(20) NOT NULL DEFAULT 'draft',
    review_note TEXT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version_no INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    approved_at TIMESTAMP NULL,
    PRIMARY KEY (concept_id),
    CONSTRAINT uq_concept_concept_code UNIQUE (concept_code),
    CONSTRAINT uq_concept_name_in_lv2 UNIQUE (category_lv2_id, concept_name),
    CONSTRAINT fk_concept_category_lv2_id
        FOREIGN KEY (category_lv2_id) REFERENCES category_lv2(category_lv2_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS concept_relation (
    relation_id BIGINT NOT NULL AUTO_INCREMENT,
    source_concept_id BIGINT NOT NULL,
    target_concept_id BIGINT NOT NULL,
    relation_type VARCHAR(30) NOT NULL,
    relation_note TEXT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (relation_id),
    CONSTRAINT uq_concept_relation UNIQUE (
        source_concept_id,
        target_concept_id,
        relation_type
    ),
    CONSTRAINT ck_concept_relation_not_self CHECK (source_concept_id <> target_concept_id),
    CONSTRAINT fk_concept_relation_source_id
        FOREIGN KEY (source_concept_id) REFERENCES concept(concept_id),
    CONSTRAINT fk_concept_relation_target_id
        FOREIGN KEY (target_concept_id) REFERENCES concept(concept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS common_mistake (
    common_mistake_id BIGINT NOT NULL AUTO_INCREMENT,
    concept_id BIGINT NOT NULL,
    mistake_title VARCHAR(200) NOT NULL,
    mistake_text TEXT NOT NULL,
    correction_text TEXT NOT NULL,
    review_status VARCHAR(20) NOT NULL DEFAULT 'draft',
    review_note TEXT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (common_mistake_id),
    CONSTRAINT fk_common_mistake_concept_id
        FOREIGN KEY (concept_id) REFERENCES concept(concept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS question (
    question_id BIGINT NOT NULL AUTO_INCREMENT,
    concept_id BIGINT NOT NULL,
    hint_common_mistake_id BIGINT NULL,
    question_format VARCHAR(30) NOT NULL DEFAULT 'MCQ_4',
    question_type VARCHAR(30) NOT NULL,
    question_text TEXT NOT NULL,
    choices_json JSON NULL,
    correct_answer VARCHAR(255) NOT NULL,
    explanation_text TEXT NOT NULL,
    difficulty_level SMALLINT NOT NULL DEFAULT 3,
    difficulty_source VARCHAR(20) NOT NULL DEFAULT 'ai_initial',
    expected_solve_sec INT NULL,
    content_hash CHAR(64) NULL,
    review_status VARCHAR(20) NOT NULL DEFAULT 'draft',
    review_note TEXT NULL,
    source_type VARCHAR(20) NOT NULL DEFAULT 'ai_generated',
    source_refs JSON NULL,
    is_daily_quiz_eligible BOOLEAN NOT NULL DEFAULT TRUE,
    is_mock_eligible BOOLEAN NOT NULL DEFAULT TRUE,
    is_concept_practice_eligible BOOLEAN NOT NULL DEFAULT TRUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version_no INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    approved_at TIMESTAMP NULL,
    PRIMARY KEY (question_id),
    CONSTRAINT uq_question_content_hash UNIQUE (content_hash),
    CONSTRAINT ck_question_difficulty CHECK (difficulty_level BETWEEN 1 AND 5),
    CONSTRAINT fk_question_concept_id
        FOREIGN KEY (concept_id) REFERENCES concept(concept_id),
    CONSTRAINT fk_question_hint_mistake_id
        FOREIGN KEY (hint_common_mistake_id) REFERENCES common_mistake(common_mistake_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_category_lv1_subject_id ON category_lv1 (subject_id);
CREATE INDEX idx_category_lv2_category_lv1_id ON category_lv2 (category_lv1_id);
CREATE INDEX idx_concept_category_lv2_id ON concept (category_lv2_id);
CREATE INDEX idx_concept_relation_source_id ON concept_relation (source_concept_id);
CREATE INDEX idx_concept_relation_target_id ON concept_relation (target_concept_id);
CREATE INDEX idx_common_mistake_concept_id ON common_mistake (concept_id);
CREATE INDEX idx_common_mistake_concept_title ON common_mistake (concept_id, mistake_title);
CREATE INDEX idx_question_concept_id ON question (concept_id);
CREATE INDEX idx_question_hint_mistake_id ON question (hint_common_mistake_id);
