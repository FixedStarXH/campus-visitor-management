package io.renren.modules.ers.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 爽约规则配置 DTO
 */
@Data
public class NoShowRuleDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 迟到多少分钟计为爽约 */
    private Integer lateMinutes;
    /** 累计多少次爽约进入黑名单 */
    private Integer maxNoShowCount;
    /** 进入黑名单的天数 */
    private Integer blacklistDays;
}
