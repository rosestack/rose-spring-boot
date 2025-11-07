-- =====================================================
-- 审计日志主表创建脚本 (PostgreSQL)
-- =====================================================
-- 用于存储审计日志的核心信息，包括事件基本信息、用户信息、HTTP信息、执行结果等
-- 支持多租户、加密存储、完整性保护等特性
-- 采用按月分区策略，提高查询性能和数据管理效率
-- =====================================================

-- 删除已存在的表（谨慎使用）
-- DROP TABLE IF EXISTS audit_log CASCADE;

-- 创建审计日志主表
CREATE TABLE audit_log (
    -- 主键ID，使用雪花算法生成
    id BIGINT NOT NULL,
    
    -- ==================== 事件基本信息 ====================
    event_time TIMESTAMP NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_subtype VARCHAR(50),
    operation_name VARCHAR(200),
    status VARCHAR(20) NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    
    -- ==================== 用户信息 ====================
    user_id VARCHAR(64),
    user_name VARCHAR(100),
    
    -- ==================== HTTP请求信息 ====================
    request_uri VARCHAR(500),
    http_method VARCHAR(10),
    http_status INTEGER,
    session_id VARCHAR(128),
    
    -- ==================== 网络信息 ====================
    client_ip INET,
    server_ip INET,
    geo_location VARCHAR(200),
    user_agent VARCHAR(100),
    
    -- ==================== 系统信息 ====================
    app_name VARCHAR(100),
    tenant_id VARCHAR(50),
    trace_id VARCHAR(100),
    execution_time BIGINT,

    -- ==================== 安全信息 ====================
    digital_signature VARCHAR(512),
    hash_value VARCHAR(128),
    prev_hash VARCHAR(128),
    
    -- ==================== 系统字段 ====================
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- 主键约束
    PRIMARY KEY (id, event_time)
) PARTITION BY RANGE (event_time);

-- 添加表注释
COMMENT ON TABLE audit_log IS '审计日志主表 - 存储系统审计日志的核心信息，支持分区、索引优化和完整性保护';

-- 添加列注释
COMMENT ON COLUMN audit_log.id IS '主键ID';
COMMENT ON COLUMN audit_log.event_time IS '事件时间';
COMMENT ON COLUMN audit_log.event_type IS '事件类型（技术分类）';
COMMENT ON COLUMN audit_log.event_subtype IS '事件子类型（技术子分类）';
COMMENT ON COLUMN audit_log.operation_name IS '具体业务操作名称';
COMMENT ON COLUMN audit_log.status IS '操作状态（SUCCESS/FAILURE/PENDING）';
COMMENT ON COLUMN audit_log.risk_level IS '风险等级（LOW/MEDIUM/HIGH/CRITICAL）';
COMMENT ON COLUMN audit_log.user_id IS '用户ID';
COMMENT ON COLUMN audit_log.user_name IS '用户名';
COMMENT ON COLUMN audit_log.request_uri IS '请求URI';
COMMENT ON COLUMN audit_log.http_method IS 'HTTP方法（GET/POST/PUT/DELETE等）';
COMMENT ON COLUMN audit_log.http_status IS 'HTTP状态码';
COMMENT ON COLUMN audit_log.session_id IS '会话ID';
COMMENT ON COLUMN audit_log.client_ip IS '客户端IP地址';
COMMENT ON COLUMN audit_log.server_ip IS '服务器IP地址';
COMMENT ON COLUMN audit_log.geo_location IS '地理位置信息';
COMMENT ON COLUMN audit_log.user_agent IS '用户代理简要信息';
COMMENT ON COLUMN audit_log.app_name IS '应用名称';
COMMENT ON COLUMN audit_log.tenant_id IS '租户ID（多租户支持）';
COMMENT ON COLUMN audit_log.trace_id IS '追踪ID（链路追踪）';
COMMENT ON COLUMN audit_log.execution_time IS '执行耗时（毫秒）';
COMMENT ON COLUMN audit_log.digital_signature IS '数字签名（完整性保护）';
COMMENT ON COLUMN audit_log.hash_value IS '哈希值（完整性保护）';
COMMENT ON COLUMN audit_log.prev_hash IS '前一条记录哈希值（链式完整性保护）';
COMMENT ON COLUMN audit_log.created_time IS '创建时间';
COMMENT ON COLUMN audit_log.deleted IS '逻辑删除标识';

-- =====================================================
-- 创建分区表
-- =====================================================

-- 2025年分区
CREATE TABLE audit_log_2025_01 PARTITION OF audit_log
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE audit_log_2025_02 PARTITION OF audit_log
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');

CREATE TABLE audit_log_2025_03 PARTITION OF audit_log
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');

CREATE TABLE audit_log_2025_04 PARTITION OF audit_log
    FOR VALUES FROM ('2025-04-01') TO ('2025-05-01');

CREATE TABLE audit_log_2025_05 PARTITION OF audit_log
    FOR VALUES FROM ('2025-05-01') TO ('2025-06-01');

CREATE TABLE audit_log_2025_06 PARTITION OF audit_log
    FOR VALUES FROM ('2025-06-01') TO ('2025-07-01');

CREATE TABLE audit_log_2025_07 PARTITION OF audit_log
    FOR VALUES FROM ('2025-07-01') TO ('2025-08-01');

CREATE TABLE audit_log_2025_08 PARTITION OF audit_log
    FOR VALUES FROM ('2025-08-01') TO ('2025-09-01');

CREATE TABLE audit_log_2025_09 PARTITION OF audit_log
    FOR VALUES FROM ('2025-09-01') TO ('2025-10-01');

CREATE TABLE audit_log_2025_10 PARTITION OF audit_log
    FOR VALUES FROM ('2025-10-01') TO ('2025-11-01');

CREATE TABLE audit_log_2025_11 PARTITION OF audit_log
    FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');

CREATE TABLE audit_log_2025_12 PARTITION OF audit_log
    FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');

-- 默认分区（用于未来数据）
CREATE TABLE audit_log_default PARTITION OF audit_log DEFAULT;

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
-- 创建函数和存储过程
-- =====================================================

-- 创建新分区的函数
CREATE OR REPLACE FUNCTION create_audit_log_partition(partition_year INTEGER, partition_month INTEGER)
RETURNS TEXT AS $$
DECLARE
    partition_name TEXT;
    start_date DATE;
    end_date DATE;
    sql_stmt TEXT;
BEGIN
    -- 计算分区名称和日期范围
    partition_name := 'audit_log_' || partition_year || '_' || LPAD(partition_month::TEXT, 2, '0');
    start_date := DATE(partition_year || '-' || partition_month || '-01');
    end_date := start_date + INTERVAL '1 month';
    
    -- 构建SQL语句
    sql_stmt := 'CREATE TABLE ' || partition_name || ' PARTITION OF audit_log ' ||
                'FOR VALUES FROM (''' || start_date || ''') TO (''' || end_date || ''')';
    
    -- 执行SQL
    EXECUTE sql_stmt;
    
    RETURN '分区 ' || partition_name || ' 创建成功';
END;
$$ LANGUAGE plpgsql;

-- 删除旧分区的函数
CREATE OR REPLACE FUNCTION drop_old_audit_log_partitions(keep_months INTEGER)
RETURNS TEXT AS $$
DECLARE
    partition_record RECORD;
    cutoff_date DATE;
    dropped_count INTEGER := 0;
BEGIN
    -- 计算保留数据的截止日期
    cutoff_date := DATE_TRUNC('month', CURRENT_DATE - INTERVAL '1 month' * keep_months);
    
    -- 查找并删除旧分区
    FOR partition_record IN
        SELECT schemaname, tablename
        FROM pg_tables
        WHERE schemaname = CURRENT_SCHEMA()
          AND tablename LIKE 'audit_log_20%'
          AND tablename != 'audit_log_default'
          AND TO_DATE(SUBSTRING(tablename FROM 11), 'YYYY_MM') < cutoff_date
    LOOP
        EXECUTE 'DROP TABLE ' || partition_record.tablename;
        dropped_count := dropped_count + 1;
    END LOOP;
    
    RETURN '删除了 ' || dropped_count || ' 个旧分区';
END;
$$ LANGUAGE plpgsql;

-- 获取审计统计信息的函数
CREATE OR REPLACE FUNCTION get_audit_log_stats(
    start_time TIMESTAMP DEFAULT NULL,
    end_time TIMESTAMP DEFAULT NULL
)
RETURNS JSON AS $$
DECLARE
    result JSON;
    where_clause TEXT := '';
BEGIN
    -- 构建WHERE子句
    IF start_time IS NOT NULL AND end_time IS NOT NULL THEN
        where_clause := 'WHERE event_time BETWEEN ''' || start_time || ''' AND ''' || end_time || ''' AND deleted = FALSE';
    ELSIF start_time IS NOT NULL THEN
        where_clause := 'WHERE event_time >= ''' || start_time || ''' AND deleted = FALSE';
    ELSIF end_time IS NOT NULL THEN
        where_clause := 'WHERE event_time <= ''' || end_time || ''' AND deleted = FALSE';
    ELSE
        where_clause := 'WHERE deleted = FALSE';
    END IF;
    
    -- 执行统计查询
    EXECUTE 'SELECT json_build_object(
        ''total_count'', COUNT(*),
        ''success_count'', SUM(CASE WHEN status = ''SUCCESS'' THEN 1 ELSE 0 END),
        ''failure_count'', SUM(CASE WHEN status = ''FAILURE'' THEN 1 ELSE 0 END),
        ''high_risk_count'', SUM(CASE WHEN risk_level IN (''HIGH'', ''CRITICAL'') THEN 1 ELSE 0 END),
        ''event_type_distribution'', json_object_agg(event_type, type_count)
    ) FROM (
        SELECT 
            event_type,
            COUNT(*) as type_count,
            status,
            risk_level
        FROM audit_log ' || where_clause || '
        GROUP BY event_type, status, risk_level
    ) stats'
    INTO result;
    
    RETURN result;
END;
$$ LANGUAGE plpgsql;

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
WHERE deleted = FALSE;

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
WHERE deleted = FALSE 
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
WHERE deleted = FALSE 
  AND status = 'FAILURE';

-- 今日审计日志视图
CREATE VIEW v_audit_log_today AS
SELECT 
    id,
    event_time,
    event_type,
    operation_name,
    status,
    risk_level,
    user_id,
    user_name,
    client_ip,
    tenant_id,
    created_time
FROM audit_log 
WHERE deleted = FALSE 
  AND DATE(event_time) = CURRENT_DATE;

-- =====================================================
-- 创建触发器
-- =====================================================

-- 自动分区创建触发器函数
CREATE OR REPLACE FUNCTION auto_create_partition_trigger()
RETURNS TRIGGER AS $$
DECLARE
    partition_year INTEGER;
    partition_month INTEGER;
    partition_exists BOOLEAN;
BEGIN
    -- 提取年月
    partition_year := EXTRACT(YEAR FROM NEW.event_time);
    partition_month := EXTRACT(MONTH FROM NEW.event_time);
    
    -- 检查分区是否存在
    SELECT EXISTS (
        SELECT 1 FROM pg_tables 
        WHERE schemaname = CURRENT_SCHEMA() 
          AND tablename = 'audit_log_' || partition_year || '_' || LPAD(partition_month::TEXT, 2, '0')
    ) INTO partition_exists;
    
    -- 如果分区不存在，创建它
    IF NOT partition_exists THEN
        PERFORM create_audit_log_partition(partition_year, partition_month);
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 创建触发器
CREATE TRIGGER tr_audit_log_auto_partition
    BEFORE INSERT ON audit_log
    FOR EACH ROW
    EXECUTE FUNCTION auto_create_partition_trigger();

-- =====================================================
-- 创建扩展和优化
-- =====================================================

-- 启用pg_stat_statements扩展（如果可用）
-- CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- 创建GIN索引用于全文搜索（如果需要）
-- CREATE INDEX idx_audit_log_fulltext ON audit_log USING gin(to_tsvector('english', operation_name));

-- =====================================================
-- 权限设置
-- =====================================================

-- 创建审计日志专用角色（可选）
-- CREATE ROLE audit_role;
-- GRANT SELECT, INSERT ON audit_log TO audit_role;
-- GRANT SELECT, INSERT ON audit_log_detail TO audit_role;
-- GRANT USAGE ON SCHEMA public TO audit_role;

-- =====================================================
-- 性能监控查询
-- =====================================================

-- 查看分区信息
-- SELECT 
--     schemaname,
--     tablename,
--     pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
-- FROM pg_tables 
-- WHERE tablename LIKE 'audit_log%'
-- ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- 查看索引使用情况
-- SELECT 
--     indexrelname,
--     idx_scan,
--     idx_tup_read,
--     idx_tup_fetch
-- FROM pg_stat_user_indexes 
-- WHERE relname = 'audit_log'
-- ORDER BY idx_scan DESC;

-- =====================================================
-- 初始化完成提示
-- =====================================================

SELECT 'audit_log 表创建完成！' AS message,
       '包含分区、索引、视图、函数、触发器' AS features,
       '支持PostgreSQL特性和性能优化' AS capabilities;