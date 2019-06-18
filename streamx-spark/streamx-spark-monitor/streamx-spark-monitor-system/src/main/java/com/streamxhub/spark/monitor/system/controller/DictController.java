package com.streamxhub.spark.monitor.system.controller;

import com.streamxhub.spark.monitor.common.annotation.Log;
import com.streamxhub.spark.monitor.common.controller.BaseController;
import com.streamxhub.spark.monitor.common.domain.QueryRequest;
import com.streamxhub.spark.monitor.common.exception.StreamXException;
import com.streamxhub.spark.monitor.system.domain.Dict;
import com.streamxhub.spark.monitor.system.service.DictService;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.wuwenze.poi.ExcelKit;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("dict")
public class DictController extends BaseController {

    private String message;

    @Autowired
    private DictService dictService;

    @GetMapping
    @RequiresPermissions("dict:view")
    public Map<String, Object> DictList(QueryRequest request, Dict dict) {
        return getDataTable(this.dictService.findDicts(request, dict));
    }

    @Log("新增字典")
    @PostMapping
    @RequiresPermissions("dict:add")
    public void addDict(@Valid Dict dict) throws StreamXException {
        try {
            this.dictService.createDict(dict);
        } catch (Exception e) {
            message = "新增字典成功";
            log.error(message, e);
            throw new StreamXException(message);
        }
    }

    @Log("删除字典")
    @DeleteMapping("/{dictIds}")
    @RequiresPermissions("dict:delete")
    public void deleteDicts(@NotBlank(message = "{required}") @PathVariable String dictIds) throws StreamXException {
        try {
            String[] ids = dictIds.split(StringPool.COMMA);
            this.dictService.deleteDicts(ids);
        } catch (Exception e) {
            message = "删除字典成功";
            log.error(message, e);
            throw new StreamXException(message);
        }
    }

    @Log("修改字典")
    @PutMapping
    @RequiresPermissions("dict:update")
    public void updateDict(@Valid Dict dict) throws StreamXException {
        try {
            this.dictService.updateDict(dict);
        } catch (Exception e) {
            message = "修改字典成功";
            log.error(message, e);
            throw new StreamXException(message);
        }
    }

    @PostMapping("excel")
    @RequiresPermissions("dict:export")
    public void export(QueryRequest request, Dict dict, HttpServletResponse response) throws StreamXException {
        try {
            List<Dict> dicts = this.dictService.findDicts(request, dict).getRecords();
            ExcelKit.$Export(Dict.class, response).downXlsx(dicts, false);
        } catch (Exception e) {
            message = "导出Excel失败";
            log.error(message, e);
            throw new StreamXException(message);
        }
    }
}
