package io.github.rosestack.spring.boot.xxljob.client;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.rosestack.core.util.JsonUtils;
import io.github.rosestack.spring.boot.xxljob.client.model.*;
import io.github.rosestack.spring.boot.xxljob.config.XxlJobProperties;
import io.github.rosestack.spring.boot.xxljob.exception.XxlJobException;
import java.net.URI;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class XxlJobClient {
    private final RestTemplate restTemplate;
    private final XxlJobProperties props;

    public XxlJobClient(RestTemplate restTemplate, XxlJobProperties props) {
        this.restTemplate = Objects.requireNonNull(restTemplate, "RestTemplate 不能为空");
        this.props = Objects.requireNonNull(props, "XxlJobProperties 不能为空");
    }

    private String baseUrl() {
        return props.getAdminAddresses();
    }

    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500))
    public XxlJobInfo addJob(XxlJobInfo job) {
        Objects.requireNonNull(job, "任务信息不能为空");
        try {
            return postForXxlContent(baseUrl() + "/jobinfo/add", toForm(job), XxlJobInfo.class);
        } catch (Exception e) {
            throw new XxlJobException("添加任务失败: " + job.getExecutorHandler(), e);
        }
    }

    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500))
    public XxlJobInfo updateJob(XxlJobInfo job) {
        Objects.requireNonNull(job, "任务信息不能为空");
        try {
            return postForXxlContent(baseUrl() + "/jobinfo/update", toForm(job), XxlJobInfo.class);
        } catch (Exception e) {
            throw new XxlJobException("更新任务失败: " + job.getExecutorHandler(), e);
        }
    }

    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500))
    public void removeJob(long id) {
        MultiValueMap<String, String> f = new LinkedMultiValueMap<>();
        f.add("id", String.valueOf(id));
        try {
            postForXxlContent(baseUrl() + "/jobinfo/remove", f, Void.class);
        } catch (Exception e) {
            throw new XxlJobException("删除任务失败: " + id, e);
        }
    }

    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500))
    public void startJob(long id) {
        MultiValueMap<String, String> f = new LinkedMultiValueMap<>();
        f.add("id", String.valueOf(id));
        try {
            postForXxlContent(baseUrl() + "/jobinfo/start", f, Void.class);
        } catch (Exception e) {
            throw new XxlJobException("启动任务失败: " + id, e);
        }
    }

    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500))
    public void stopJob(long id) {
        MultiValueMap<String, String> f = new LinkedMultiValueMap<>();
        f.add("id", String.valueOf(id));
        try {
            postForXxlContent(baseUrl() + "/jobinfo/stop", f, Void.class);
        } catch (Exception e) {
            throw new XxlJobException("停止任务失败: " + id, e);
        }
    }

    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500))
    public XxlJobInfoPage pageJobInfo(long jobGroupId, String executorHandler) {
        LinkedMultiValueMap<String, String> f = new LinkedMultiValueMap<>();
        f.add("jobGroup", String.valueOf(jobGroupId));
        if (executorHandler != null) f.add("executorHandler", executorHandler);
        try {
            return postForXxlContent(baseUrl() + "/jobinfo/pageList", f, XxlJobInfoPage.class);
        } catch (Exception e) {
            throw new XxlJobException("查询任务列表失败: " + jobGroupId, e);
        }
    }

    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500))
    public XxlJobInfo getJob(long id) {
        LinkedMultiValueMap<String, String> f = new LinkedMultiValueMap<>();
        f.add("id", String.valueOf(id));
        try {
            return postForXxlContent(baseUrl() + "/jobinfo/loadById", f, XxlJobInfo.class);
        } catch (Exception e) {
            throw new XxlJobException("获取任务详情失败: " + id, e);
        }
    }

    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500))
    public XxlJobGroup addJobGroup(String appname) {
        Objects.requireNonNull(appname, "应用名称不能为空");
        LinkedMultiValueMap<String, String> f = new LinkedMultiValueMap<>();
        f.add("appname", appname);
        try {
            return postForXxlContent(baseUrl() + "/jobgroup/save", f, XxlJobGroup.class);
        } catch (Exception e) {
            throw new XxlJobException("添加执行器组失败: " + appname, e);
        }
    }

    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500))
    public XxlJobGroupPage pageJobGroup(String appName) {
        MultiValueMap<String, String> f = new LinkedMultiValueMap<>();
        if (appName != null) f.add("appname", appName);
        try {
            return postForXxlContent(baseUrl() + "/jobgroup/pageList", f, XxlJobGroupPage.class);
        } catch (Exception e) {
            throw new XxlJobException("查询执行器组列表失败: " + appName, e);
        }
    }

    // 新增：带内容泛型的解析，返回完整响应（表单提交）
    private <T> XxlRestResponse<T> postForXxlResponse(
            String url, MultiValueMap<String, String> form, Class<T> contentClass) {
        log.debug("调用 XXL-Job API: {}", url);

        RequestEntity<MultiValueMap<String, String>> req = RequestEntity.post(URI.create(url))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(form);

        ResponseEntity<String> respEntity;
        try {
            respEntity = restTemplate.exchange(req, String.class);
        } catch (HttpClientErrorException e) {
            throw new XxlJobException("XXL-Job API 客户端错误: " + e.getMessage(), e);
        } catch (HttpServerErrorException e) {
            throw new XxlJobException("XXL-Job API 服务器错误: " + e.getMessage(), e);
        } catch (ResourceAccessException e) {
            throw new XxlJobException("XXL-Job API 连接失败: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new XxlJobException("XXL-Job API 调用异常: " + e.getMessage(), e);
        }

        ObjectMapper mapper = JsonUtils.getObjectMapper();
        try {
            JavaType type = mapper.getTypeFactory().constructParametricType(XxlRestResponse.class, contentClass);
            XxlRestResponse<T> resp = mapper.readValue(respEntity.getBody(), type);
            if (resp == null) {
                throw new XxlJobException("XXL-Job API 返回空响应");
            }
            Integer code = resp.getCode();
            if (code == null || code != 200) {
                String errorMsg = resp.getMsg() != null ? resp.getMsg() : "未知错误";
                throw new XxlJobException("XXL-Job API 调用失败: " + errorMsg);
            }
            log.debug("XXL-Job API 调用成功: {}", url);
            return resp;
        } catch (XxlJobException e) {
            throw e;
        } catch (Exception e) {
            throw new XxlJobException("XXL-Job API 响应解析失败: " + e.getMessage(), e);
        }
    }

    // 新增：直接返回 content 内容（表单提交）
    private <T> T postForXxlContent(String url, MultiValueMap<String, String> form, Class<T> contentClass) {
        XxlRestResponse<T> r = postForXxlResponse(url, form, contentClass);
        return r.getContent();
    }

    @SuppressWarnings("unchecked")
    private static MultiValueMap<String, String> toForm(XxlJobInfo obj) {
        try {
            return JsonUtils.fromString(JsonUtils.toString(obj), MultiValueMap.class);
        } catch (Exception e) {
            throw new XxlJobException("任务信息序列化失败: " + e.getMessage(), e);
        }
    }
}
