package io.renren.modules.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.renren.common.exception.RRException;
import io.renren.common.utils.DateUtils;
import io.renren.common.utils.PageUtils;
import io.renren.common.utils.Query;
import io.renren.modules.application.dao.ApplicationDao;
import io.renren.modules.application.entity.ApplicationEntity;
import io.renren.modules.application.form.ApplicationForm;
import io.renren.modules.application.form.ApplicationQueryForm;
import io.renren.modules.application.service.ApplicationService;
import io.renren.modules.visitor.service.VisitorService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 入校申请服务实现类
 * 包含管理员后台功能和用户API功能
 */
@Service("applicationService")
public class ApplicationServiceImpl extends ServiceImpl<ApplicationDao, ApplicationEntity> implements ApplicationService {

    @Autowired
    private VisitorService visitorService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String realName = (String) params.get("realName");
        String phone = (String) params.get("phone");
        Integer status = null;
        if (params.get("status") != null && StringUtils.isNotBlank(params.get("status").toString())) {
            status = Integer.parseInt(params.get("status").toString());
        }
        String startDateStr = (String) params.get("startDate");
        String endDateStr = (String) params.get("endDate");

        Date startDate = null;
        Date endDate = null;
        if (StringUtils.isNotBlank(startDateStr)) {
            startDate = DateUtils.stringToDate(startDateStr, "yyyy-MM-dd");
        }
        if (StringUtils.isNotBlank(endDateStr)) {
            endDate = DateUtils.stringToDate(endDateStr, "yyyy-MM-dd");
        }

        IPage<ApplicationEntity> page = this.page(
                new Query<ApplicationEntity>().getPage(params, "id", false),
                new QueryWrapper<ApplicationEntity>()
                        .eq("deleted", 0)
                        .like(StringUtils.isNotBlank(realName), "visitor_name", realName)
                        .like(StringUtils.isNotBlank(phone), "phone", phone)
                        .eq(status != null, "status", status)
                        .ge(startDate != null, "entry_date", startDate)
                        .le(endDate != null, "entry_date", endDate)
        );

        return new PageUtils(page);
    }

    @Override
    public ApplicationEntity getDetail(Long id) {
        ApplicationEntity application = this.getById(id);
        if (application == null || application.getDeleted() == 1) {
            throw new RRException("申请不存在");
        }
        return application;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, Long approverId) {
        ApplicationEntity application = this.getById(id);
        if (application == null || application.getDeleted() == 1) {
            throw new RRException("申请不存在");
        }
        if (application.getStatus() != 0) {
            throw new RRException("申请状态异常，无法审批");
        }

        String entryCode = generateEntryCode();
        String qrCodePath = generateQrCode(entryCode);

        application.setStatus(1);
        application.setEntryCode(entryCode);
        application.setAttachmentUrl(qrCodePath);
        application.setApprovalUserId(approverId);
        application.setApprovalTime(new Date());
        application.setUpdateTime(new Date());

        this.updateById(application);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, String reason, Long approverId) {
        if (StringUtils.isBlank(reason)) {
            throw new RRException("拒绝原因不能为空");
        }

        ApplicationEntity application = this.getById(id);
        if (application == null || application.getDeleted() == 1) {
            throw new RRException("申请不存在");
        }
        if (application.getStatus() != 0) {
            throw new RRException("申请状态异常，无法拒绝");
        }

        application.setStatus(2);
        application.setRejectReason(reason);
        application.setApprovalUserId(approverId);
        application.setApprovalTime(new Date());
        application.setUpdateTime(new Date());

        this.updateById(application);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchApprove(List<Long> ids, String remark, Long approverId) {
        if (ids == null || ids.isEmpty()) {
            throw new RRException("请选择要审批的申请");
        }

        List<ApplicationEntity> applications = this.listByIds(ids);
        if (applications.size() != ids.size()) {
            throw new RRException("部分申请不存在");
        }

        for (ApplicationEntity application : applications) {
            if (application.getDeleted() == 1) {
                throw new RRException("部分申请已被删除");
            }
            if (application.getStatus() != 0) {
                throw new RRException("只能审批待审批状态的申请");
            }
        }

        baseMapper.batchUpdateStatus(ids, 1, approverId, remark);

        for (ApplicationEntity application : applications) {
            String entryCode = generateEntryCode();
            String qrCodePath = generateQrCode(entryCode);
            application.setStatus(1);
            application.setEntryCode(entryCode);
            application.setAttachmentUrl(qrCodePath);
            application.setUpdateTime(new Date());
            this.updateById(application);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ApplicationEntity application = this.getById(id);
        if (application == null || application.getDeleted() == 1) {
            throw new RRException("申请不存在");
        }

        List<Integer> allowedStatus = Arrays.asList(0,1,2, 3,4,5);
        if (!allowedStatus.contains(application.getStatus())) {
            throw new RRException("只能删除待审批或已取消状态的申请");
        }

        application.setDeleted(1);
        application.setUpdateTime(new Date());
        this.updateById(application);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new RRException("请选择要删除的申请");
        }

        List<ApplicationEntity> applications = this.listByIds(ids);
        if (applications.size() != ids.size()) {
            throw new RRException("部分申请不存在");
        }

        for (ApplicationEntity application : applications) {
            if (application.getDeleted() == 1) {
                throw new RRException("部分申请已被删除");
            }
        }

        for (ApplicationEntity application : applications) {
            application.setDeleted(1);
            application.setUpdateTime(new Date());
            this.updateById(application);
        }
    }

    @Override
    public Integer getPendingCount() {
        return baseMapper.countPendingApplications();
    }

    private String generateEntryCode() {
        return "EC" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    private String generateQrCode(String entryCode) {
        return "/qrcode/" + entryCode + ".png";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchReject(List<Long> ids, String reason, Long approverId) {
        if (ids == null || ids.isEmpty()) {
            throw new RRException("请选择要拒绝的申请");
        }
        if (StringUtils.isBlank(reason)) {
            throw new RRException("拒绝原因不能为空");
        }

        List<ApplicationEntity> applications = this.listByIds(ids);
        if (applications.size() != ids.size()) {
            throw new RRException("部分申请不存在");
        }

        for (ApplicationEntity application : applications) {
            if (application.getDeleted() == 1) {
                throw new RRException("部分申请已被删除");
            }
            if (application.getStatus() != 0) {
                throw new RRException("只能拒绝待审批状态的申请");
            }
        }

        for (ApplicationEntity application : applications) {
            application.setStatus(2);
            application.setRejectReason(reason);
            application.setApprovalUserId(approverId);
            application.setApprovalTime(new Date());
            application.setUpdateTime(new Date());
            this.updateById(application);
        }
    }

    @Override
    public PageUtils queryPendingPage(Map<String, Object> params) {
        String realName = (String) params.get("realName");
        String phone = (String) params.get("phone");
        String startDateStr = (String) params.get("startDate");
        String endDateStr = (String) params.get("endDate");

        Date startDate = null;
        Date endDate = null;
        if (StringUtils.isNotBlank(startDateStr)) {
            startDate = DateUtils.stringToDate(startDateStr, "yyyy-MM-dd");
        }
        if (StringUtils.isNotBlank(endDateStr)) {
            endDate = DateUtils.stringToDate(endDateStr, "yyyy-MM-dd");
        }

        IPage<ApplicationEntity> page = this.page(
                new Query<ApplicationEntity>().getPage(params),
                new QueryWrapper<ApplicationEntity>()
                        .eq("status", 0)
                        .eq("deleted", 0)
                        .like(StringUtils.isNotBlank(realName), "visitor_name", realName)
                        .like(StringUtils.isNotBlank(phone), "phone", phone)
                        .ge(startDate != null, "entry_date", startDate)
                        .le(endDate != null, "entry_date", endDate)
                        .orderByDesc("create_time")
        );

        return new PageUtils(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(ApplicationForm form, Long visitorId) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 7);
        Date maxDate = cal.getTime();

        if (form.getEntryDate().after(maxDate)) {
            throw new RRException("预约日期只能在未来7天内");
        }

        if (form.getEntryStartTime().after(form.getEntryEndTime())) {
            throw new RRException("预约开始时间不能晚于结束时间");
        }

        ApplicationEntity application = new ApplicationEntity();
        application.setVisitorId(visitorId);
        application.setVisitUnit(form.getVisitUnit());
        application.setVehiclePlate(form.getVehiclePlate());
        application.setVisitorName(form.getVisitorName());
        application.setPhone(form.getPhone());
        application.setEntryDate(form.getEntryDate());
        application.setSlotId(1L);
        application.setEntryStartTime(form.getEntryStartTime());
        application.setEntryEndTime(form.getEntryEndTime());
        application.setReason(form.getReason());
        application.setCompanionCount(form.getCompanionCount());
        application.setStatus(0);
        application.setCreateTime(new Date());

        String applicationNo = "APP" + System.currentTimeMillis();
        application.setApplicationNo(applicationNo);

        this.save(application);

        return application.getId();
    }

    @Override
    public Map<String, Object> listPage(ApplicationQueryForm form, Long visitorId) {
        QueryWrapper<ApplicationEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("visitor_id", visitorId);
        wrapper.eq("deleted", 0);

        System.out.println("=== listPage debug ===");
        System.out.println("visitorId: " + visitorId);
        System.out.println("form.getPageNum(): " + form.getPageNum());
        System.out.println("form.getPageSize(): " + form.getPageSize());
        System.out.println("form.getStartTime(): " + form.getStartTime());
        System.out.println("form.getEndTime(): " + form.getEndTime());

        if (form.getStatus() != null) {
            wrapper.eq("status", form.getStatus());
        }
        if (form.getStartTime() != null) {
            wrapper.ge("create_time", form.getStartTime());
        }
        if (form.getEndTime() != null) {
            wrapper.le("create_time", form.getEndTime());
        }

        wrapper.orderByDesc("create_time");

        Page<ApplicationEntity> page = new Page<>(form.getPageNum(), form.getPageSize());
        Page<ApplicationEntity> result = this.baseMapper.selectPage(page, wrapper);

        System.out.println("result.getTotal(): " + result.getTotal());
        System.out.println("result.getRecords(): " + result.getRecords());

        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getRecords());
        data.put("total", result.getTotal());
        data.put("pageNum", result.getCurrent());
        data.put("pageSize", result.getSize());
        data.put("pages", result.getPages());

        return data;
    }

    @Override
    public ApplicationEntity getDetailById(Long id, Long visitorId) {
        QueryWrapper<ApplicationEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id);
        wrapper.eq("visitor_id", visitorId);
        wrapper.eq("deleted", 0);

        return this.baseMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(Long id, Long visitorId) {
        ApplicationEntity application = getDetailById(id, visitorId);
        if (application == null) {
            return false;
        }

        if (application.getStatus() != 0) {
            return false;
        }

        application.setStatus(3);
        application.setCancelTime(new Date());

        return this.updateById(application);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markNoShow(Long id, Long operatorId) {
        ApplicationEntity application = this.getById(id);
        if (application == null || application.getDeleted() == 1) {
            throw new RRException("申请不存在");
        }
        if (application.getStatus() != 1) {
            throw new RRException("只能标记已通过状态的申请为爽约");
        }

        application.setStatus(4);
        application.setUpdateTime(new Date());
        this.updateById(application);
    }
}
