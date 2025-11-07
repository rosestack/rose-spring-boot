package io.github.rosestack.spring.boot.xxljob.client.model;

import lombok.Data;

@Data
public class XxlRestResponse<T> {

    private T content;

    private String msg;

    private Integer code;
}
