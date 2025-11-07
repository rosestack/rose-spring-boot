package io.github.rosestack.spring.boot.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.rosestack.spring.boot.audit.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {}
