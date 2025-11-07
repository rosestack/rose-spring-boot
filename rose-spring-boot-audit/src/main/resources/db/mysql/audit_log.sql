-- =====================================================
-- 审计日志主表创建脚本 (MySQL)
-- =====================================================
-- 用于存储审计日志的核心信息，包括事件基本信息、用户信息、HTTP信息、执行结果等
-- 支持多租户、加密存储、完整性保护等特性
-- 采用按月分区策略，提高查询性能和数据管理效率
-- =====================================================

-- 删除已存在的表（谨慎使用）
-- DROP TABLE IF EXISTS audit_log;

-- 创建审计日志主表
CREATE TABLE audit_log (
    -- 主键ID，使用雪花算法生成
    id BIGINT NOT NULL COMMENT '主键ID',
    
    -- ==================== 事件基本信息 ====================
    event_time DATETIME NOT NULL COMMENT '事件时间',
    event_type VARCHAR(50) NOT NULL COMMENT '事件类型（技术分类）',
    event_subtype VARCHAR(50) COMMENT '事件子类型（技术子分类）',
    operation_name VARCHAR(200) COMMENT '具体业务操作名称',
    status VARCHAR(20) NOT NULL COMMENT '操作状态（SUCCESS/FAILURE/PENDING）',
    risk_level VARCHAR(20) NOT NULL COMMENT '风险等级（LOW/MEDIUM/HIGH/CRITICAL）',
    
    -- ==================== 用户信息 ====================
    user_id VARCHAR(64) COMMENT '用户ID',
    user_name VARCHAR(100) COMMENT '用户名',
    
    -- ==================== HTTP请求信息 ====================
    request_uri VARCHAR(500) COMMENT '请求URI',
    http_method VARCHAR(10) COMMENT 'HTTP方法（GET/POST/PUT/DELETE等）',
    http_status INT COMMENT 'HTTP状态码',
    session_id VARCHAR(128) COMMENT '会话ID',
    
    -- ==================== 网络信息 ====================
    client_ip VARCHAR(45) COMMENT '客户端IP地址（支持IPv6）',
    server_ip VARCHAR(45) COMMENT '服务器IP地址（支持IPv6）',
    geo_location VARCHAR(200) COMMENT '地理位置信息',
    user_agent VARCHAR(100) COMMENT '用户代理简要信息',
    
    -- ==================== 系统信息 ====================
    app_name VARCHAR(100) COMMENT '应用名称',
    tenant_id VARCHAR(50) COMMENT '租户ID（多租户支持）',
    trace_id VARCHAR(100) COMMENT '追踪ID（链路追踪）',
    execution_time BIGINT COMMENT '执行耗时（毫秒）',

    -- ==================== 安全信息 ====================
    digital_signature VARCHAR(512) COMMENT '数字签名（完整性保护）',
    hash_value VARCHAR(128) COMMENT '哈希值（完整性保护）',
    prev_hash VARCHAR(128) COMMENT '前一条记录哈希值（链式完整性保护）',
    
    -- ==================== 系统字段 ====================
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识（0-未删除，1-已删除）',
    
    -- 主键约束
    PRIMARY KEY (id, event_time)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci 
  COMMENT='审计日志主表'
  -- 按月分区，提高查询性能
  PARTITION BY RANGE (YEAR(event_time) * 100 + MONTH(event_time)) (
    PARTITION p202501 VALUES LESS THAN (202502) COMMENT '2025年1月',
    PARTITION p202502 VALUES LESS THAN (202503) COMMENT '2025年2月',
    PARTITION p202503 VALUES LESS THAN (202504) COMMENT '2025年3月',
    PARTITION p202504 VALUES LESS THAN (202505) COMMENT '2025年4月',
    PARTITION p202505 VALUES LESS THAN (202506) COMMENT '2025年5月',
    PARTITION p202506 VALUES LESS THAN (202507) COMMENT '2025年6月',
    PARTITION p202507 VALUES LESS THAN (202508) COMMENT '2025年7月',
    PARTITION p202508 VALUES LESS THAN (202509) COMMENT '2025年8月',
    PARTITION p202509 VALUES LESS THAN (202510) COMMENT '2025年9月',
    PARTITION p202510 VALUES LESS THAN (202511) COMMENT '2025年10月',
    PARTITION p202511 VALUES LESS THAN (202512) COMMENT '2025年11月',
    PARTITION p202512 VALUES LESS THAN (202513) COMMENT '2025年12月',
    PARTITION p_future VALUES LESS THAN MAXVALUE COMMENT '未来分区'
  );

-- =====================================================
-- 创建索引
-- =====================================================

-- 事件时间索引（最重要的查询条件）
CREATE INDEX idx_audit_log_event_time ON audit_log (event_time);

-- 用户ID索引（用户行为分析）
CREATE INDEX idx_audit_log_user_id ON audit_log (user_id);

-- 租户ID索引（多租户查询）
CREATE INDEX idx_audit_log_tenant_id ON audit_log (tenant_id);

-- 事件类型索引（按类型统计）
CREATE INDEX idx_audit_log_event_type ON audit_log (event_type);

-- 风险等级索引（安全分析）
CREATE INDEX idx_audit_log_risk_level ON audit_log (risk_level);

-- 操作状态索引（失败分析）
CREATE INDEX idx_audit_log_status ON audit_log (status);

-- 追踪ID索引（链路追踪）
CREATE INDEX idx_audit_log_trace_id ON audit_log (trace_id);

-- 客户端IP索引（安全分析）
CREATE INDEX idx_audit_log_client_ip ON audit_log (client_ip);

-- 复合索引：租户+时间（最常用的查询组合）
CREATE INDEX idx_audit_log_tenant_time ON audit_log (tenant_id, event_time);

-- 复合索引：用户+时间（用户行为分析）
CREATE INDEX idx_audit_log_user_time ON audit_log (user_id, event_time);

-- 复合索引：事件类型+时间（统计分析）
CREATE INDEX idx_audit_log_type_time ON audit_log (event_type, event_time);

-- 复合索引：风险等级+时间（安全监控）
CREATE INDEX idx_audit_log_risk_time ON audit_log (risk_level, event_time);

-- 逻辑删除索引（过滤已删除记录）
CREATE INDEX idx_audit_log_deleted ON audit_log (deleted);

-- =====================================================
-- 分区管理存储过程
-- =====================================================

DELIMITER $$

-- 创建新分区的存储过程
CREATE PROCEDURE CreateAuditLogPartition(IN partition_year INT, IN partition_month INT)
BEGIN
    DECLARE partition_name VARCHAR(20);
    DECLARE partition_value INT;
    DECLARE next_partition_value INT;
    DECLARE sql_stmt TEXT;
    
    -- 计算分区名称和值
    SET partition_name = CONCAT('p', partition_year, LPAD(partition_month, 2, '0'));
    SET partition_value = partition_year * 100 + partition_month;
    SET next_partition_value = IF(partition_month = 12, 
                                  (partition_year + 1) * 100 + 1, 
                                  partition_value + 1);
    
    -- 构建SQL语句
    SET sql_stmt = CONCAT(
        'ALTER TABLE audit_log ADD PARTITION (',
        'PARTITION ', partition_name, 
        ' VALUES LESS THAN (', next_partition_value, ')',
        ' COMMENT ''', partition_year, '年', partition_month, '月'')'
    );
    
    -- 执行SQL
    SET @sql = sql_stmt;
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    
    SELECT CONCAT('分区 ', partition_name, ' 创建成功') AS result;
END$$

-- 删除旧分区的存储过程（保留指定月数的数据）
CREATE PROCEDURE DropOldAuditLogPartitions(IN keep_months INT)
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE partition_name VARCHAR(64);
    DECLARE partition_description VARCHAR(1024);
    DECLARE cutoff_value INT;
    DECLARE sql_stmt TEXT;
    
    -- 计算保留数据的截止值
    SET cutoff_value = (YEAR(DATE_SUB(NOW(), INTERVAL keep_months MONTH)) * 100) + 
                       MONTH(DATE_SUB(NOW(), INTERVAL keep_months MONTH));
    
    -- 游标定义
    DECLARE partition_cursor CURSOR FOR
        SELECT PARTITION_NAME, PARTITION_DESCRIPTION
        FROM INFORMATION_SCHEMA.PARTITIONS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'audit_log'
          AND PARTITION_NAME IS NOT NULL
          AND PARTITION_NAME != 'p_future'
          AND CAST(SUBSTRING(PARTITION_NAME, 2) AS UNSIGNED) < cutoff_value;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN partition_cursor;
    
    read_loop: LOOP
        FETCH partition_cursor INTO partition_name, partition_description;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        -- 删除分区
        SET sql_stmt = CONCAT('ALTER TABLE audit_log DROP PARTITION ', partition_name);
        SET @sql = sql_stmt;
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        
        SELECT CONCAT('分区 ', partition_name, ' 已删除') AS result;
    END LOOP;
    
    CLOSE partition_cursor;
END$$

DELIMITER ;

-- =====================================================
-- 创建视图
-- =====================================================

-- 活跃审计日志视图（排除已删除的记录）
CREATE VIEW v_audit_log_active AS
SELECT 
    id,
    event_time,
    event_type,
    event_subtype,
    operation_name,
    status,
    risk_level,
    user_id,
    user_name,
    request_uri,
    http_method,
    http_status,
    session_id,
    client_ip,
    server_ip,
    geo_location,
    user_agent,
    app_name,
    tenant_id,
    trace_id,
    execution_time,
    digital_signature,
    hash_value,
    prev_hash,
    created_time
FROM audit_log 
WHERE deleted = 0;

-- 高风险审计日志视图
CREATE VIEW v_audit_log_high_risk AS
SELECT 
    id,
    event_time,
    event_type,
    event_subtype,
    operation_name,
    status,
    risk_level,
    user_id,
    user_name,
    request_uri,
    client_ip,
    tenant_id,
    trace_id,
    created_time
FROM audit_log 
WHERE deleted = 0 
  AND risk_level IN ('HIGH', 'CRITICAL');

-- 失败操作审计日志视图
CREATE VIEW v_audit_log_failures AS
SELECT 
    id,
    event_time,
    event_type,
    operation_name,
    status,
    user_id,
    user_name,
    request_uri,
    client_ip,
    tenant_id,
    created_time
FROM audit_log 
WHERE deleted = 0 
  AND status = 'FAILURE';

-- =====================================================
-- 表注释和权限设置
-- =====================================================

-- 添加表级别注释
ALTER TABLE audit_log COMMENT = '审计日志主表 - 存储系统审计日志的核心信息，支持分区、索引优化和完整性保护';

-- 创建审计日志专用用户（可选）
-- CREATE USER 'audit_user'@'%' IDENTIFIED BY 'strong_password_here';
-- GRANT SELECT, INSERT ON audit_log TO 'audit_user'@'%';
-- GRANT SELECT, INSERT ON audit_log_detail TO 'audit_user'@'%';

-- =====================================================
-- 初始化完成提示
-- =====================================================

SELECT 'audit_log 表创建完成！' AS message,
       '包含分区、索引、视图、存储过程' AS features,
       '请根据实际需求调整分区策略和索引' AS note;