-- =============================================================================
-- schema_fingerprint.sql
--
-- 두 DB 의 스키마가 같은지 대조한다. 테이블마다 한 줄(테이블명, MD5)을 낸다.
-- 컬럼(순서·타입·NULL·기본값·문자셋), 인덱스, FK, CHECK, 엔진·콜레이션을 information_schema 에서
-- 모아 해시하므로, SHOW CREATE TABLE 의 표기 차이(COLLATE 만 찍히느냐, CHARACTER SET 까지
-- 찍히느냐)에 흔들리지 않는다. flyway_schema_history 는 뺀다.
--
-- 쓰는 곳: 운영 DB 가 V1__baseline 과 같은지(Flyway 전환 전), 이후 스키마가 어긋났는지.
-- 기준값은 빈 MySQL 8.4 에 백엔드를 띄워(Flyway 가 V1 부터 적용) 같은 파일을 돌려 얻는다.
--   mysql -N coremate < scripts/db/schema_fingerprint.sql | md5
-- =============================================================================
SET SESSION group_concat_max_len = 1000000;
SELECT t.table_name,
       MD5(CONCAT_WS('#',
         t.engine, t.table_collation,
         (SELECT GROUP_CONCAT(CONCAT_WS('|', c.ordinal_position, c.column_name, c.column_type, c.is_nullable,
                                          IFNULL(c.column_default,'<null>'), c.extra,
                                          IFNULL(c.character_set_name,'-'), IFNULL(c.collation_name,'-'))
                              ORDER BY c.ordinal_position SEPARATOR ';')
            FROM information_schema.columns c
           WHERE c.table_schema = t.table_schema AND c.table_name = t.table_name),
         (SELECT IFNULL(GROUP_CONCAT(CONCAT_WS('|', s.index_name, s.non_unique, s.seq_in_index, s.column_name,
                                                 IFNULL(s.sub_part,'-'), s.index_type)
                                     ORDER BY s.index_name, s.seq_in_index SEPARATOR ';'), '-')
            FROM information_schema.statistics s
           WHERE s.table_schema = t.table_schema AND s.table_name = t.table_name),
         (SELECT IFNULL(GROUP_CONCAT(CONCAT_WS('|', r.constraint_name, r.referenced_table_name, r.update_rule, r.delete_rule,
                                                 (SELECT GROUP_CONCAT(CONCAT(k.column_name,'->',k.referenced_column_name)
                                                                      ORDER BY k.ordinal_position)
                                                    FROM information_schema.key_column_usage k
                                                   WHERE k.constraint_schema = r.constraint_schema
                                                     AND k.constraint_name = r.constraint_name
                                                     AND k.table_name = r.table_name))
                                     ORDER BY r.constraint_name SEPARATOR ';'), '-')
            FROM information_schema.referential_constraints r
           WHERE r.constraint_schema = t.table_schema AND r.table_name = t.table_name),
         (SELECT IFNULL(GROUP_CONCAT(CONCAT_WS('|', tc.constraint_name, cc.check_clause)
                                     ORDER BY tc.constraint_name SEPARATOR ';'), '-')
            FROM information_schema.table_constraints tc
            JOIN information_schema.check_constraints cc
              ON cc.constraint_schema = tc.constraint_schema AND cc.constraint_name = tc.constraint_name
           WHERE tc.table_schema = t.table_schema AND tc.table_name = t.table_name AND tc.constraint_type = 'CHECK')
       )) AS fp
  FROM information_schema.tables t
 WHERE t.table_schema = DATABASE() AND t.table_type = 'BASE TABLE' AND t.table_name <> 'flyway_schema_history'
 ORDER BY t.table_name;
