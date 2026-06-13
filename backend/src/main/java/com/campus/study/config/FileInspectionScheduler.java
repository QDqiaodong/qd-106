package com.campus.study.config;

import com.campus.study.service.FileInspectionService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FileInspectionScheduler {

    private static final Logger logger = LoggerFactory.getLogger(FileInspectionScheduler.class);

    @Resource
    private FileInspectionService fileInspectionService;

    @Value("${file.inspection.incremental.limit:100}")
    private int incrementalLimit;

    @Scheduled(cron = "${file.inspection.incremental.cron:0 0 */4 * * ?}")
    public void runIncrementalInspection() {
        logger.info("定时增量巡检任务启动");
        try {
            var result = fileInspectionService.runIncrementalInspection(incrementalLimit);
            logger.info("定时增量巡检完成: 批次={}, 检查={}, 异常={}",
                    result.get("batchId"), result.get("totalChecked"), result.get("exceptionCount"));
        } catch (Exception e) {
            logger.error("定时增量巡检任务执行异常", e);
        }
    }

    @Scheduled(cron = "${file.inspection.full.cron:0 30 2 * * ?}")
    public void runFullInspection() {
        logger.info("定时全量巡检任务启动");
        try {
            var result = fileInspectionService.runFullInspection();
            logger.info("定时全量巡检完成: 批次={}, 检查={}, 异常={}",
                    result.get("batchId"), result.get("totalChecked"), result.get("exceptionCount"));
        } catch (Exception e) {
            logger.error("定时全量巡检任务执行异常", e);
        }
    }
}
