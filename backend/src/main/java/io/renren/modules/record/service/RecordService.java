package io.renren.modules.record.service;

import io.renren.modules.record.form.RecordQueryForm;

import java.util.Map;

public interface RecordService {
    Map<String, Object> listPage(RecordQueryForm form, Long visitorId);
}