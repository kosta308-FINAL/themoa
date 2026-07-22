package com.weaone.themoa.common.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Logback에는 최소·최대 레벨을 함께 거는 표준 필터가 없어({@code LevelRangeFilter}는 Log4j2 전용),
 * info.log에서 ERROR를 제외하기 위해 직접 만든다(managelogging.md §2-3).
 */
public class InfoWarnLevelFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent event) {
        Level level = event.getLevel();
        if (level.isGreaterOrEqual(Level.INFO) && level.toInt() <= Level.WARN.toInt()) {
            return FilterReply.NEUTRAL;
        }
        return FilterReply.DENY;
    }
}
