package com.streamxhub.flink.monitor.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.streamxhub.flink.monitor.base.controller.BaseController;
import com.streamxhub.flink.monitor.base.exception.AdminXException;
import com.streamxhub.flink.monitor.system.entity.Test;
import com.streamxhub.flink.monitor.system.service.TestService;
import com.streamxhub.flink.monitor.base.domain.RestRequest;
import com.streamxhub.flink.monitor.base.domain.RestResponse;
import com.wuwenze.poi.ExcelKit;
import com.wuwenze.poi.handler.ExcelReadHandler;
import com.wuwenze.poi.pojo.ExcelErrorField;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * @author benjobs
 */
@Slf4j
@RestController
@RequestMapping("test")
public class TestController extends BaseController {

    private String message;

    @Autowired
    private TestService testService;

    @GetMapping
    public Map<String, Object> findTests(RestRequest request) {
        Page<Test> page = new Page<>(request.getPageNum(), request.getPageSize());
        return getDataTable(testService.page(page, null));
    }

    /**
     * 生成 Excel导入模板
     */
    @PostMapping("template")
    public void generateImportTemplate(HttpServletResponse response) {
        // 构建数据
        List<Test> list = new ArrayList<>();
        IntStream.range(0, 20).forEach(i -> {
            Test test = new Test();
            test.setField1("字段1");
            test.setField2(i + 1);
            test.setField3("apollo" + i + "@qq.com");
            list.add(test);
        });
        // 构建模板
        ExcelKit.$Export(Test.class, response).downXlsx(list, true);
    }

    /**
     * 导入Excel数据，并批量插入 T_TEST表
     */
    @PostMapping("import")
    public RestResponse importExcels(@RequestParam("file") MultipartFile file) throws AdminXException {
        try {
            if (file.isEmpty()) {
                throw new AdminXException("导入数据为空");
            }
            String filename = file.getOriginalFilename();
            if (!StringUtils.endsWith(filename, ".xlsx")) {
                throw new AdminXException("只支持.xlsx类型文件导入");
            }
            // 开始导入操作
            long beginTimeMillis = System.currentTimeMillis();
            final List<Test> data = Lists.newArrayList();
            final List<Map<String, Object>> error = Lists.newArrayList();
            ExcelKit.$Import(Test.class).readXlsx(file.getInputStream(), new ExcelReadHandler<Test>() {
                @Override
                public void onSuccess(int sheet, int row, Test test) {
                    // 数据校验成功时，加入集合
                    test.setCreateTime(new Date());
                    data.add(test);
                }

                @Override
                public void onError(int sheet, int row, List<ExcelErrorField> errorFields) {
                    // 数据校验失败时，记录到 error集合
                    error.add(ImmutableMap.of("row", row, "errorFields", errorFields));
                }
            });
            if (!data.isEmpty()) {
                // 将合法的记录批量入库
                this.testService.batchInsert(data);
            }
            long time = ((System.currentTimeMillis() - beginTimeMillis));
            ImmutableMap<String, Object> result = ImmutableMap.of(
                    "time", time,
                    "data", data,
                    "error", error
            );
            return new RestResponse().data(result);
        } catch (Exception e) {
            message = "导入Excel数据失败," + e.getMessage();
            log.info(message);
            throw new AdminXException(message);
        }
    }

    /**
     * 导出 Excel
     */
    @PostMapping("export")
    public void export(HttpServletResponse response) throws AdminXException {
        try {
            List<Test> list = this.testService.findTests();
            ExcelKit.$Export(Test.class, response).downXlsx(list, false);
        } catch (Exception e) {
            message = "导出Excel失败";
            log.info(message, e);
            throw new AdminXException(message);
        }
    }
}
