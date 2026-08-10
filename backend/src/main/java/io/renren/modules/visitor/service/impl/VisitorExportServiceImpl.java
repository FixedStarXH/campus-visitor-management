package io.renren.modules.visitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.renren.modules.application.entity.ApplicationEntity;
import io.renren.modules.application.dao.ApplicationDao;
import io.renren.modules.visitor.service.VisitorExportService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service("visitorExportService")
public class VisitorExportServiceImpl extends ServiceImpl<ApplicationDao, ApplicationEntity>
        implements VisitorExportService {

    private static final String[] HEADERS = {
            "申请编号", "访客姓名", "手机号", "到访单位", "预约日期",
            "开始时间", "结束时间", "入校事由", "陪同人数", "审批状态"
    };

    @Override
    public void exportExcel(Integer status, String startTime, String endTime,
                           HttpServletResponse response) throws java.io.IOException {
        QueryWrapper<ApplicationEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);

        if (status != null) {
            wrapper.eq("status", status);
        }
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge("create_time", startTime);
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le("create_time", endTime + " 23:59:59");
        }

        wrapper.orderByDesc("create_time");
        List<ApplicationEntity> list = this.list(wrapper);

        writeExcel(list, response, "访客入校申请记录");
    }

    @Override
    public void exportUserExcel(Long userId, HttpServletResponse response) throws java.io.IOException {
        QueryWrapper<ApplicationEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);
        wrapper.eq("visitor_id", userId);
        wrapper.orderByDesc("create_time");

        List<ApplicationEntity> list = this.list(wrapper);
        writeExcel(list, response, "个人入校申请记录");
    }

    private void writeExcel(List<ApplicationEntity> list, HttpServletResponse response, String fileNamePrefix) throws java.io.IOException {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("访客入校申请记录");

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdfTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        int rowNum = 1;
        for (ApplicationEntity app : list) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(app.getApplicationNo() != null ? app.getApplicationNo() : "");
            row.createCell(1).setCellValue(app.getVisitorName() != null ? app.getVisitorName() : "");
            row.createCell(2).setCellValue(app.getPhone() != null ? app.getPhone() : "");
            row.createCell(3).setCellValue(app.getVisitUnit() != null ? app.getVisitUnit() : "");
            row.createCell(4).setCellValue(app.getEntryDate() != null ? sdf.format(app.getEntryDate()) : "");
            row.createCell(5).setCellValue(app.getEntryStartTime() != null ? sdfTime.format(app.getEntryStartTime()) : "");
            row.createCell(6).setCellValue(app.getEntryEndTime() != null ? sdfTime.format(app.getEntryEndTime()) : "");
            row.createCell(7).setCellValue(app.getReason() != null ? app.getReason() : "");
            row.createCell(8).setCellValue(app.getCompanionCount() != null ? app.getCompanionCount() : 0);
            row.createCell(9).setCellValue(getStatusText(app.getStatus()));
        }

        for (int i = 0; i < HEADERS.length; i++) {
            sheet.autoSizeColumn(i);
        }

        String fileName = fileNamePrefix + "_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedFileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition",
                "attachment;filename=\"" + encodedFileName + "\";filename*=UTF-8''" + encodedFileName);

        OutputStream out = response.getOutputStream();
        workbook.write(out);
        out.flush();
        out.close();
        workbook.close();
    }

    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待审批";
            case 1: return "已通过";
            case 2: return "已拒绝";
            case 3: return "已取消";
            case 4: return "已爽约";
            case 5: return "已完成";
            default: return "未知";
        }
    }
}