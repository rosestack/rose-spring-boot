-- =====================================================
-- 审计日志详情表创建脚本 (PostgreSQL)
-- =====================================================
-- 用于存储审计日志的详细信息，采用 JSON 格式存储复杂数据结构
-- 支持敏感数据标记、加密存储、按类型分类等特性
-- 详情数据按类型分为：HTTP请求相关、操作对象相关、数据变更相关、系统技术相关、安全相关
-- =====================================================

-- 删除已存在的表（谨慎使用）
-- DROP TABLE IF EXISTS audit_log_detail CASCADE;

-- 创建审计日志详情表
CREATE TABLE audit_log_detail (
    -- 主键ID，使用序列生成
    id BIGSERIAL PRIMARY KEY,
    
    -- 外键关联
    audit_log_id BIGINT NOT NULL,
    
    -- ==================== 详情分类信息 ====================
    detail_type VARCHAR(50) NOT NULL,
    detail_key VARCHAR(50) NOT NULL,
    
    -- ==================== 详情内容 ====================
    detail_value JSONB,
    
    -- ==================== 安全标记 ====================
    is_sensitive BOOLEAN NOT NULL DEFAULT FALSE,
    is_encrypted BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- ==================== 多租户支持 ====================
    tenant_id VARCHAR(50),
    
    -- ==================== 系统字段 ====================
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 外键约束
    CONSTRAINT fk_audit_log_detail_audit_log 
        FOREIGN KEY (audit_log_id) 
        REFERENCES audit_log (id) 
        ON DELETE CASCADE 
        ON UPDATE CASCADE
);

-- 添加表注释
COMMENT ON TABLE audit_log_detail IS '审计日志详情表 - 存储审计日志的详细信息，支持JSONB格式、敏感数据标记和加密存储';

-- 添加列注释
COMMENT ON COLUMN audit_log_detail.id IS '主键ID';
COMMENT ON COLUMN audit_log_detail.audit_log_id IS '审计日志ID（外键）';
COMMENT ON COLUMN audit_log_detail.detail_type IS '详情类型（HTTP/OPERATION/DATA_CHANGE/SYSTEM/SECURITY）';
COMMENT ON COLUMN audit_log_detail.detail_key IS '详情键（具体的详情项标识）';
COMMENT ON COLUMN audit_log_detail.detail_value IS '详情值（JSONB格式，可能加密脱敏）';
COMMENT ON COLUMN audit_log_detail.is_sensitive IS '是否包含敏感数据';
COMMENT ON COLUMN audit_log_detail.is_encrypted IS '是否已加密存储';
COMMENT ON COLUMN audit_log_detail.tenant_id IS '租户ID（多租户支持）';
COMMENT ON COLUMN audit_log_detail.created_time IS '创建时间';

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

-- JSONB GIN索引（用于JSON查询）
CREATE INDEX idx_audit_detail_value_gin ON audit_log_detail USING gin (detail_value);

-- JSONB路径索引（用于特定JSON路径查询）
CREATE INDEX idx_audit_detail_value_path ON audit_log_detail USING gin ((detail_value -> 'data'));

-- =====================================================
-- 创建函数
-- =====================================================

-- 批量插入审计详情的函数
CREATE OR REPLACE FUNCTION batch_insert_audit_details(
    p_audit_log_id BIGINT,
    p_details JSONB
)
RETURNS TEXT AS $$
DECLARE
    detail_item JSONB;
    inserted_count INTEGER := 0;
BEGIN
    -- 循环处理每个详情项
    FOR detail_item IN SELECT * FROM jsonb_array_elements(p_details)
    LOOP
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
            detail_item->>'detail_type',
            detail_item->>'detail_key',
            detail_item->'detail_value',
            COALESCE((detail_item->>'is_sensitive')::BOOLEAN, FALSE),
            COALESCE((detail_item->>'is_encrypted')::BOOLEAN, FALSE),
            detail_item->>'tenant_id'
        );
        
        inserted_count := inserted_count + 1;
    END LOOP;
    
    RETURN '成功插入 ' || inserted_count || ' 条审计详情记录';
END;
$$ LANGUAGE plpgsql;

-- 清理过期详情数据的函数
CREATE OR REPLACE FUNCTION cleanup_expired_audit_details(days_to_keep INTEGER)
RETURNS TEXT AS $$
DECLARE
    deleted_count INTEGER;
    cutoff_date TIMESTAMP;
BEGIN
    -- 计算截止日期
    cutoff_date := CURRENT_TIMESTAMP - INTERVAL '1 day' * days_to_keep;
    
    -- 删除过期数据
    DELETE FROM audit_log_detail 
    WHERE created_time < cutoff_date;
    
    -- 获取删除的记录数
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    RETURN '清理完成，删除了 ' || deleted_count || ' 条过期详情记录，截止日期: ' || cutoff_date;
END;
$$ LANGUAGE plpgsql;

-- 加密敏感详情数据的函数
CREATE OR REPLACE FUNCTION encrypt_sensitive_details(batch_size INTEGER DEFAULT 1000)
RETURNS TEXT AS $$
DECLARE
    detail_record RECORD;
    processed_count INTEGER := 0;
BEGIN
    -- 查找并处理未加密的敏感数据
    FOR detail_record IN
        SELECT id, detail_value
        FROM audit_log_detail 
        WHERE is_sensitive = TRUE 
          AND is_encrypted = FALSE 
        LIMIT batch_size
    LOOP
        -- 这里应该调用实际的加密函数
        -- 目前只是标记为已加密并添加前缀
        UPDATE audit_log_detail 
        SET is_encrypted = TRUE,
            detail_value = jsonb_build_object('encrypted', TRUE, 'data', '[ENCRYPTED]' || (detail_value::TEXT))
        WHERE id = detail_record.id;
        
        processed_count := processed_count + 1;
    END LOOP;
    
    RETURN '处理完成，加密了 ' || processed_count || ' 条敏感详情记录';
END;
$$ LANGUAGE plpgsql;

-- 获取审计详情统计信息的函数
CREATE OR REPLACE FUNCTION get_audit_detail_stats(p_audit_log_id BIGINT DEFAULT NULL)
RETURNS JSONB AS $$
DECLARE
    result JSONB;
    where_clause TEXT := '';
BEGIN
    -- 构建WHERE子句
    IF p_audit_log_id IS NOT NULL THEN
        where_clause := 'WHERE audit_log_id = ' || p_audit_log_id;
    END IF;
    
    -- 执行统计查询
    EXECUTE 'SELECT jsonb_build_object(
        ''total_count'', COUNT(*),
        ''sensitive_count'', SUM(CASE WHEN is_sensitive THEN 1 ELSE 0 END),
        ''encrypted_count'', SUM(CASE WHEN is_encrypted THEN 1 ELSE 0 END),
        ''type_distribution'', jsonb_object_agg(detail_type, type_count),
        ''avg_value_size'', AVG(LENGTH(detail_value::TEXT)),
        ''total_size_mb'', ROUND(SUM(LENGTH(detail_value::TEXT))::NUMERIC / 1024 / 1024, 2)
    ) FROM (
        SELECT 
            detail_type,
            COUNT(*) as type_count,
            is_sensitive,
            is_encrypted,
            detail_value
        FROM audit_log_detail ' || where_clause || '
        GROUP BY detail_type, is_sensitive, is_encrypted, detail_value
    ) stats'
    INTO result;
    
    RETURN result;
END;
$$ LANGUAGE plpgsql;

-- JSON路径查询函数
CREATE OR REPLACE FUNCTION search_audit_details_by_path(
    json_path TEXT,
    search_value TEXT,
    limit_count INTEGER DEFAULT 100
)
RETURNS TABLE (
    id BIGINT,
    audit_log_id BIGINT,
    detail_type VARCHAR(50),
    detail_key VARCHAR(50),
    matched_value JSONB,
    created_time TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    EXECUTE 'SELECT 
        d.id,
        d.audit_log_id,
        d.detail_type,
        d.detail_key,
        d.detail_value #> ''' || json_path || ''' as matched_value,
        d.created_time
    FROM audit_log_detail d
    WHERE d.detail_value #>> ''' || json_path || ''' ILIKE ''%' || search_value || '%''
    ORDER BY d.created_time DESC
    LIMIT ' || limit_count;
END;
$$ LANGUAGE plpgsql;

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
        WHEN is_encrypted THEN jsonb_build_object('status', 'encrypted')
        ELSE jsonb_build_object('preview', LEFT(detail_value::TEXT, 100))
    END AS detail_value_preview,
    is_sensitive,
    is_encrypted,
    tenant_id,
    created_time
FROM audit_log_detail 
WHERE is_sensitive = TRUE;

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
        WHEN is_sensitive THEN jsonb_build_object('status', 'sensitive_data_masked')
        ELSE detail_value
    END AS detail_value_safe,
    tenant_id,
    created_time
FROM audit_log_detail 
WHERE detail_type = 'SECURITY';

-- 详情统计视图
CREATE VIEW v_audit_detail_statistics AS
SELECT 
    detail_type,
    COUNT(*) as total_count,
    SUM(CASE WHEN is_sensitive THEN 1 ELSE 0 END) as sensitive_count,
    SUM(CASE WHEN is_encrypted THEN 1 ELSE 0 END) as encrypted_count,
    AVG(LENGTH(detail_value::TEXT)) as avg_size,
    MIN(created_time) as first_created,
    MAX(created_time) as last_created
FROM audit_log_detail 
GROUP BY detail_type;

-- =====================================================
-- 创建触发器
-- =====================================================

-- 审计详情插入触发器函数
CREATE OR REPLACE FUNCTION audit_detail_before_insert_trigger()
RETURNS TRIGGER AS $$
BEGIN
    -- 如果没有设置租户ID，从关联的审计日志中获取
    IF NEW.tenant_id IS NULL THEN
        SELECT tenant_id INTO NEW.tenant_id 
        FROM audit_log 
        WHERE id = NEW.audit_log_id;
    END IF;
    
    -- 设置创建时间
    IF NEW.created_time IS NULL THEN
        NEW.created_time := CURRENT_TIMESTAMP;
    END IF;
    
    -- 验证JSON格式
    IF NEW.detail_value IS NOT NULL THEN
        BEGIN
            -- 尝试访问JSON，如果格式错误会抛出异常
            PERFORM NEW.detail_value::JSONB;
        EXCEPTION WHEN OTHERS THEN
            RAISE EXCEPTION '详情值必须是有效的JSON格式';
        END;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 创建触发器
CREATE TRIGGER tr_audit_detail_before_insert
    BEFORE INSERT ON audit_log_detail
    FOR EACH ROW
    EXECUTE FUNCTION audit_detail_before_insert_trigger();

-- 审计详情更新触发器函数（记录敏感数据变更）
CREATE OR REPLACE FUNCTION audit_detail_after_update_trigger()
RETURNS TRIGGER AS $$
BEGIN
    -- 如果敏感数据状态发生变化，记录日志
    IF OLD.is_sensitive != NEW.is_sensitive OR OLD.is_encrypted != NEW.is_encrypted THEN
        INSERT INTO audit_log_detail (
            audit_log_id,
            detail_type,
            detail_key,
            detail_value,
            is_sensitive,
            is_encrypted,
            tenant_id
        ) VALUES (
            NEW.audit_log_id,
            'SYSTEM',
            'SECURITY_STATUS_CHANGE',
            jsonb_build_object(
                'detail_id', NEW.id,
                'old_sensitive', OLD.is_sensitive,
                'new_sensitive', NEW.is_sensitive,
                'old_encrypted', OLD.is_encrypted,
                'new_encrypted', NEW.is_encrypted,
                'change_time', CURRENT_TIMESTAMP
            ),
            FALSE,
            FALSE,
            NEW.tenant_id
        );
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 创建更新触发器
CREATE TRIGGER tr_audit_detail_after_update
    AFTER UPDATE ON audit_log_detail
    FOR EACH ROW
    EXECUTE FUNCTION audit_detail_after_update_trigger();

-- =====================================================
-- 创建扩展功能
-- =====================================================

-- 全文搜索配置（如果需要）
-- CREATE INDEX idx_audit_detail_fulltext ON audit_log_detail 
-- USING gin(to_tsvector('english', detail_value::TEXT));

-- 创建物化视图用于复杂统计（可选）
-- CREATE MATERIALIZED VIEW mv_audit_detail_daily_stats AS
-- SELECT 
--     DATE(created_time) as stat_date,
--     detail_type,
--     COUNT(*) as daily_count,
--     SUM(CASE WHEN is_sensitive THEN 1 ELSE 0 END) as sensitive_count
-- FROM audit_log_detail 
-- GROUP BY DATE(created_time), detail_type
-- ORDER BY stat_date DESC, detail_type;

-- 创建刷新物化视图的函数
-- CREATE OR REPLACE FUNCTION refresh_audit_detail_stats()
-- RETURNS VOID AS $$
-- BEGIN
--     REFRESH MATERIALIZED VIEW mv_audit_detail_daily_stats;
-- END;
-- $$ LANGUAGE plpgsql;

-- =====================================================
-- 性能优化建议
-- =====================================================

-- 1. 定期分析表统计信息
-- ANALYZE audit_log_detail;

-- 2. 监控表大小和索引使用情况
-- SELECT 
--     schemaname,
--     tablename,
--     pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size,
--     pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) as table_size,
--     pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename) - pg_relation_size(schemaname||'.'||tablename)) as index_size
-- FROM pg_tables 
-- WHERE tablename = 'audit_log_detail';

-- 3. 监控JSONB查询性能
-- SELECT 
--     query,
--     calls,
--     total_time,
--     mean_time
-- FROM pg_stat_statements 
-- WHERE query LIKE '%audit_log_detail%' 
-- ORDER BY total_time DESC;

-- =====================================================
-- 数据完整性检查
-- =====================================================

-- 检查孤立的详情记录
-- SELECT COUNT(*) as orphaned_details
-- FROM audit_log_detail d
-- LEFT JOIN audit_log l ON d.audit_log_id = l.id
-- WHERE l.id IS NULL;

-- 检查敏感数据的加密状态
-- SELECT 
--     COUNT(*) as total_sensitive,
--     SUM(CASE WHEN is_encrypted THEN 1 ELSE 0 END) as encrypted_count,
--     ROUND(SUM(CASE WHEN is_encrypted THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as encryption_rate
-- FROM audit_log_detail 
-- WHERE is_sensitive = TRUE;

-- 检查JSON格式有效性
-- SELECT COUNT(*) as invalid_json_count
-- FROM audit_log_detail 
-- WHERE detail_value IS NOT NULL 
--   AND NOT (detail_value::TEXT ~ '^[\s]*[\{\[].*[\}\]][\s]*$');

-- =====================================================
-- 权限设置
-- =====================================================

-- 创建审计详情专用角色（可选）
-- CREATE ROLE audit_detail_role;
-- GRANT SELECT, INSERT ON audit_log_detail TO audit_detail_role;
-- GRANT USAGE ON SEQUENCE audit_log_detail_id_seq TO audit_detail_role;
-- GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO audit_detail_role;

-- =====================================================
-- 初始化完成提示
-- =====================================================

SELECT 'audit_log_detail 表创建完成！' AS message,
       '包含JSONB支持、索引、视图、函数、触发器' AS features,
       '支持PostgreSQL高级特性和JSON查询优化' AS capabilities;