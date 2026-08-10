package io.renren.modules.record.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.renren.modules.application.entity.ApplicationEntity;
import io.renren.modules.record.dao.RecordDao;
import io.renren.modules.record.form.RecordQueryForm;
import io.renren.modules.record.service.RecordService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service("recordService")
public class RecordServiceImpl extends ServiceImpl<RecordDao, ApplicationEntity>
        implements RecordService {

    // 查询入校记录（已完成状态的申请）
    @Override
    public Map<String, Object> listPage(RecordQueryForm form, Long visitorId) {
        QueryWrapper<ApplicationEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("visitor_id", visitorId);
        wrapper.eq("deleted", 0);
        wrapper.eq("status", 1);

        if (form.getStartTime() != null) {
            wrapper.ge("create_time", form.getStartTime());
        }
        if (form.getEndTime() != null) {
            wrapper.le("create_time", form.getEndTime());
        }

        wrapper.orderByDesc("create_time");

        Page<ApplicationEntity> page = new Page<>(form.getPageNum(), form.getPageSize());
        Page<ApplicationEntity> result = this.baseMapper.selectPage(page, wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getRecords());
        data.put("total", result.getTotal());
        data.put("pageNum", result.getCurrent());
        data.put("pageSize", result.getSize());
        data.put("pages", result.getPages());

        return data;
    }
}