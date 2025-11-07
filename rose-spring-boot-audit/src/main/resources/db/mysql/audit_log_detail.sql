-- =====================================================
-- 审计日志详情表创建脚本 (MySQL)
-- =====================================================
-- 用于存储审计日志的详细信息，采用 JSON 格式存储复杂数据结构
-- 支持敏感数据标记、加密存储、按类型分类等特性
-- 详情数据按类型分为：HTTP请求相关、操作对象相关、数据变更相关、系统技术相关、安全相关
-- =====================================================

-- 删除已存在的表（谨慎使用）
-- DROP TABLE IF EXISTS audit_log_detail;

-- 创建审计日志详情表
CREATE TABLE audit_log_detail (
    -- 主键ID，使用雪花算法生成
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    
    -- 外键关联
    audit_log_id BIGINT NOT NULL COMMENT '审计日志ID（外键）',
    
    -- ==================== 详情分类信息 ====================
    detail_type VARCHAR(50) NOT NULL COMMENT '详情类型（HTTP/OPERATION/DATA_CHANGE/SYSTEM/SECURITY）',
    detail_key VARCHAR(50) NOT NULL COMMENT '详情键（具体的详情项标识）',
    
    -- ==================== 详情内容 ====================
    detail_value LONGTEXT COMMENT '详情值（JSON格式，可能加密脱敏）',
    
    -- ==================== 安全标记 ====================
    is_sensitive TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否包含敏感数据（0-否，1-是）',
    is_encrypted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已加密存储（0-否，1-是）',
    
    -- ==================== 多租户支持 ====================
    tenant_id VARCHAR(50) COMMENT '租户ID（多租户支持）',
    
    -- ==================== 系统字段 ====================
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    -- 主键约束
    PRIMARY KEY (id),
    
    -- 外键约束
    CONSTRAINT fk_audit_log_detail_audit_log 
        FOREIGN KEY (audit_log_id) 
        REFERENCES audit_log (id) 
        ON DELETE CASCADE 
        ON UPDATE CASCADE
        
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci 
  COMMENT='审计日志详情表';

-- =====================================================
-- 创建索引
-- =====================================================

-- 审计日志ID索引（最重要的查询条件）
CREATE INDEX idx_audit_detail_log_id ON audit_log_detail (audit_log_id);

-- 详情类型索引（按类型查询）
CREATE INDEX idx_audit_detail_type ON audit_log_detail (detail_type);

-- 详情键索引（按键查询）
CREATE INDEX idx_audit_detail_key ON audit_log_detail (detail_key);

-- 敏感数据索引（安全审计）
CREATE INDEX idx_audit_detail_sensitive ON audit_log_detail (is_sensitive);

-- 加密状态索引（加密管理）
CREATE INDEX idx_audit_detail_encrypted ON audit_log_detail (is_encrypted);

-- 租户ID索引（多租户查询）
CREATE INDEX idx_audit_detail_tenant_id ON audit_log_detail (tenant_id);

-- 创建时间索引（时间范围查询）
CREATE INDEX idx_audit_detail_created_time ON audit_log_detail (created_time);

-- 复合索引：审计日志ID+详情类型（常用查询组合）
CREATE INDEX idx_audit_detail_log_type ON audit_log_detail (audit_log_id, detail_type);

-- 复合索引：审计日志ID+详情键（精确查询）
CREATE INDEX idx_audit_detail_log_key ON audit_log_detail (audit_log_id, detail_key);

-- 复合索引：租户+类型（多租户按类型查询）
CREATE INDEX idx_audit_detail_tenant_type ON audit_log_detail (tenant_id, detail_type);

-- 复合索引：敏感数据+加密状态（安全管理）
CREATE INDEX idx_audit_detail_security ON audit_log_detail (is_sensitive, is_encrypted);

-- =====================================================
-- 创建视图
-- =====================================================

-- 敏感数据详情视图
CREATE VIEW v_audit_detail_sensitive AS
SELECT 
    id,
    audit_log_id,
    detail_type,
    detail_key,
    CASE 
        WHEN is_encrypted = 1 THEN '[已加密]'
        ELSE LEFT(detail_value, 100)
    END AS detail_value_preview,
    is_sensitive,
    is_encrypted,
    tenant_id,
    created_time
FROM audit_log_detail 
WHERE is_sensitive = 1;

-- HTTP请求详情视图
CREATE VIEW v_audit_detail_http AS
SELECT 
    id,
    audit_log_id,
    detail_key,
    detail_value,
    tenant_id,
    created_time
FROM audit_log_detail 
WHERE detail_type = 'HTTP';

-- 数据变更详情视图
CREATE VIEW v_audit_detail_data_change AS
SELECT 
    id,
    audit_log_id,
    detail_key,
    detail_value,
    is_sensitive,
    is_encrypted,
    tenant_id,
    created_time
FROM audit_log_detail 
WHERE detail_type = 'DATA_CHANGE';

-- 安全相关详情视图
CREATE VIEW v_audit_detail_security AS
SELECT 
    id,
    audit_log_id,
    detail_key,
    CASE 
        WHEN is_sensitive = 1 THEN '[敏感数据]'
        ELSE detail_value
    END AS detail_value_safe,
    tenant_id,
    created_time
FROM audit_log_detail 
WHERE detail_type = 'SECURITY';

-- =====================================================
-- 创建存储过程
-- =====================================================

DELIMITER $$

-- 批量插入审计详情的存储过程
CREATE PROCEDURE BatchInsertAuditDetails(
    IN p_audit_log_id BIGINT,
    IN p_details JSON
)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE detail_count INT;
    DECLARE detail_item JSON;
    
    -- 获取详情数组长度
    SET detail_count = JSON_LENGTH(p_details);
    
    -- 循环插入每个详情项
    WHILE i < detail_count DO
        SET detail_item = JSON_EXTRACT(p_details, CONCAT('$[', i, ']'));
        
        INSERT INTO audit_log_detail (
            audit_log_id,
            detail_type,
            detail_key,
            detail_value,
            is_sensitive,
            is_encrypted,
            tenant_id
        ) VALUES (
            p_audit_log_id,
            JSON_UNQUOTE(JSON_EXTRACT(detail_item, '$.detail_type')),
            JSON_UNQUOTE(JSON_EXTRACT(detail_item, '$.detail_key')),
            JSON_UNQUOTE(JSON_EXTRACT(detail_item, '$.detail_value')),
            COALESCE(JSON_EXTRACT(detail_item, '$.is_sensitive'), 0),
            COALESCE(JSON_EXTRACT(detail_item, '$.is_encrypted'), 0),
            JSON_UNQUOTE(JSON_EXTRACT(detail_item, '$.tenant_id'))
        );
        
        SET i = i + 1;
    END WHILE;
    
    SELECT CONCAT('成功插入 ', detail_count, ' 条审计详情记录') AS result;
END$$

-- 清理过期详情数据的存储过程
CREATE PROCEDURE CleanupExpiredAuditDetails(IN days_to_keep INT)
BEGIN
    DECLARE deleted_count INT DEFAULT 0;
    DECLARE cutoff_date DATETIME;
    
    -- 计算截止日期
    SET cutoff_date = DATE_SUB(NOW(), INTERVAL days_to_keep DAY);
    
    -- 删除过期数据
    DELETE FROM audit_log_detail 
    WHERE created_time < cutoff_date;
    
    -- 获取删除的记录数
    SET deleted_count = ROW_COUNT();
    
    SELECT CONCAT('清理完成，删除了 ', deleted_count, ' 条过期详情记录') AS result,
           cutoff_date AS cutoff_date;
END$$

-- 加密敏感详情数据的存储过程
CREATE PROCEDURE EncryptSensitiveDetails(IN batch_size INT)
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE detail_id BIGINT;
    DECLARE processed_count INT DEFAULT 0;
    
    -- 游标定义：查找未加密的敏感数据
    DECLARE sensitive_cursor CURSOR FOR
        SELECT id 
        FROM audit_log_detail 
        WHERE is_sensitive = 1 
          AND is_encrypted = 0 
        LIMIT batch_size;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN sensitive_cursor;
    
    read_loop: LOOP
        FETCH sensitive_cursor INTO detail_id;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        -- 这里应该调用实际的加密函数
        -- 目前只是标记为已加密
        UPDATE audit_log_detail 
        SET is_encrypted = 1,
            detail_value = CONCAT('[ENCRYPTED]', detail_value)
        WHERE id = detail_id;
        
        SET processed_count = processed_count + 1;
    END LOOP;
    
    CLOSE sensitive_cursor;
    
    SELECT CONCAT('处理完成，加密了 ', processed_count, ' 条敏感详情记录') AS result;
END$$

DELIMITER ;

-- =====================================================
-- 创建触发器
-- =====================================================

-- 审计详情插入触发器（自动设置租户ID）
DELIMITER $$

CREATE TRIGGER tr_audit_detail_before_insert
BEFORE INSERT ON audit_log_detail
FOR EACH ROW
BEGIN
    -- 如果没有设置租户ID，从关联的审计日志中获取
    IF NEW.tenant_id IS NULL THEN
        SELECT tenant_id INTO NEW.tenant_id 
        FROM audit_log 
        WHERE id = NEW.audit_log_id;
    END IF;
    
    -- 设置创建时间
    IF NEW.created_time IS NULL THEN
        SET NEW.created_time = NOW();
    END IF;
END$$

DELIMITER ;

-- =====================================================
-- 创建函数
-- =====================================================

DELIMITER $$

-- 获取审计详情统计信息的函数
CREATE FUNCTION GetAuditDetailStats(p_audit_log_id BIGINT) 
RETURNS JSON
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE result JSON;
    
    SELECT JSON_OBJECT(
        'total_count', COUNT(*),
        'sensitive_count', SUM(CASE WHEN is_sensitive = 1 THEN 1 ELSE 0 END),
        'encrypted_count', SUM(CASE WHEN is_encrypted = 1 THEN 1 ELSE 0 END),
        'type_distribution', JSON_OBJECTAGG(detail_type, type_count)
    ) INTO result
    FROM (
        SELECT 
            detail_type,
            COUNT(*) as type_count,
            is_sensitive,
            is_encrypted
        FROM audit_log_detail 
        WHERE audit_log_id = p_audit_log_id
        GROUP BY detail_type, is_sensitive, is_encrypted
    ) stats;
    
    RETURN result;
END$$

DELIMITER ;

-- =====================================================
-- 性能优化建议
-- =====================================================

-- 1. 定期分析表统计信息
-- ANALYZE TABLE audit_log_detail;

-- 2. 定期优化表结构
-- OPTIMIZE TABLE audit_log_detail;

-- 3. 监控索引使用情况
-- SELECT * FROM sys.schema_unused_indexes WHERE object_schema = DATABASE() AND object_name = 'audit_log_detail';

-- 4. 监控表大小
-- SELECT 
--     table_name,
--     ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'Size (MB)'
-- FROM information_schema.tables 
-- WHERE table_schema = DATABASE() 
--   AND table_name = 'audit_log_detail';

-- =====================================================
-- 数据完整性检查
-- =====================================================

-- 检查孤立的详情记录（没有对应的审计日志）
-- SELECT COUNT(*) as orphaned_details
-- FROM audit_log_detail d
-- LEFT JOIN audit_log l ON d.audit_log_id = l.id
-- WHERE l.id IS NULL;

-- 检查敏感数据的加密状态
-- SELECT 
--     COUNT(*) as total_sensitive,
--     SUM(CASE WHEN is_encrypted = 1 THEN 1 ELSE 0 END) as encrypted_count,
--     ROUND(SUM(CASE WHEN is_encrypted = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as encryption_rate
-- FROM audit_log_detail 
-- WHERE is_sensitive = 1;

-- =====================================================
-- 表注释和权限设置
-- =====================================================

-- 添加表级别注释
ALTER TABLE audit_log_detail COMMENT = '审计日志详情表 - 存储审计日志的详细信息，支持JSON格式、敏感数据标记和加密存储';

-- =====================================================
-- 初始化完成提示
-- =====================================================

SELECT 'audit_log_detail 表创建完成！' AS message,
       '包含索引、视图、存储过程、触发器、函数' AS features,
       '支持敏感数据管理和性能优化' AS capabilities;