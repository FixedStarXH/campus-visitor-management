package io.renren.modules.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import io.renren.common.utils.PageUtils;
import io.renren.modules.application.entity.ApplicationEntity;
import io.renren.modules.application.form.ApplicationForm;
import io.renren.modules.application.form.ApplicationQueryForm;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 入校申请服务接口
 */
public interface ApplicationService extends IService<ApplicationEntity> {

    // ==================== 管理员后台功能 ====================

    /**
     * 分页查询入校申请列表
     */
    PageUtils queryPage(Map<String, Object> params);

    /**
     * 获取申请详情
     */
    ApplicationEntity getDetail(Long id);

    /**
     * 审批通过
     */
    void approve(Long id, Long approverId);

    /**
     * 审批拒绝
     */
    void reject(Long id, String reason, Long approverId);

    /**
     * 批量审批通过
     */
    void batchApprove(List<Long> ids, String remark, Long approverId);

    /**
     * 统计待审批申请数量
     */
    Integer getPendingCount();

    /**
     * 批量审批拒绝
     */
    void batchReject(List<Long> ids, String reason, Long approverId);

    /**
     * 待审批申请列表
     */
    PageUtils queryPendingPage(Map<String, Object> params);

    /**
     * 单个删除申请（软删除）
     */
    void delete(Long id);

    /**
     * 批量删除申请（软删除）
     */
    void batchDelete(List<Long> ids);

    // ==================== 用户API功能 ====================

    /**
     * 提交入校申请
     */
    Long submit(ApplicationForm form, Long visitorId);

    /**
     * 查询申请列表（分页）
     */
    Map<String, Object> listPage(ApplicationQueryForm form, Long visitorId);

    /**
     * 查询申请详情
     */
    ApplicationEntity getDetailById(Long id, Long visitorId);

    /**
     * 取消入校申请
     */
    boolean cancel(Long id, Long visitorId);

    /**
     * 标记爽约
     */
    void markNoShow(Long id, Long operatorId);
}
