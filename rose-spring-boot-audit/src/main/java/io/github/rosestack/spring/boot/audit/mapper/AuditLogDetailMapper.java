package io.github.rosestack.spring.boot.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.rosestack.spring.boot.audit.entity.AuditLogDetail;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogDetailMapper extends BaseMapper<AuditLogDetail> {}
