<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <!-- Console appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] trace_id=%mdc{trace_id} span_id=%mdc{span_id} %level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
        <labels>
            compose_service=${loki.compose.service:space}
        </labels>
        <url>${loki.url}</url>
        <batchSize>100</batchSize>
        <queueSize>1000</queueSize>
        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
            <layout class="ch.qos.logback.classic.PatternLayout">
                <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] trace_id=%mdc{trace_id} span_id=%mdc{span_id} %level %logger{36} - %msg%n</pattern>
            </layout>
        </encoder>

        <format>
            <label>
                <pattern>compose_service=space,level=%level,logger=%logger{20}</pattern>
            </label>
            <message>
                <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] ${LOG_LEVEL_PATTERN} %logger{36} - %msg%n</pattern>
            </message>
        </format>
    </appender>

    <!-- Root logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="LOKI"/>
    </root>

</configuration>
