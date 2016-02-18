import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender

import static ch.qos.logback.classic.Level.INFO
import static ch.qos.logback.classic.Level.OFF

def appenderList = ["CONSOLE"]

jmxConfigurator()

println "="*80
println """
    LOGGING FILE      : logback.groovy
"""
println "="*80

logger('org.springframework', OFF)
logger('org.springframework.boot', OFF)
logger('org.springframework.web.servlet', OFF)
logger('org.springframework.security.web', OFF)
logger('org.springframework.context.support', OFF)
logger("info.developerblog", INFO)

appender("CONSOLE", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%-4r [%thread] %-5level %X{request_id} - %msg%n"
    }
}

root(INFO, appenderList)