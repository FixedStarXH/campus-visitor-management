package io.renren.modules.visitor.service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface VisitorExportService {
    void exportExcel(Integer status, String startTime, String endTime,
                     HttpServletResponse response) throws IOException;

    void exportUserExcel(Long userId, HttpServletResponse response) throws IOException;
}