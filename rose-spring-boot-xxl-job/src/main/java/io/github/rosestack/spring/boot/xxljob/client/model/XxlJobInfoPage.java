package io.github.rosestack.spring.boot.xxljob.client.model;

import java.util.List;
import lombok.Data;

@Data
public class XxlJobInfoPage {

    private Long recordsFiltered;

    private Long recordsTotal;

    private List<XxlJobInfo> data;
}
