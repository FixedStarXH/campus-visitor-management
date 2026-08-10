package io.renren.modules.ers.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.renren.modules.ers.entity.EntryRecordEntity;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface EntryRecordService extends IService<EntryRecordEntity> {

    Map<String, Object> getTodayOverview();

    List<EntryRecordEntity> queryPage(Map<String, Object> params);

    void exportRecords(Map<String, Object> params, HttpServletResponse response) throws IOException;

    void exportApplications(Map<String, Object> params, HttpServletResponse response) throws IOException;
}