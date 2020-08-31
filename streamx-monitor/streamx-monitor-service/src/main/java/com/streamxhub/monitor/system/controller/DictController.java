package com.streamxhub.monitor.system.controller;

import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.streamxhub.monitor.base.annotation.Log;
import com.streamxhub.monitor.base.controller.BaseController;
import com.streamxhub.monitor.base.exception.AdminXException;
import com.streamxhub.monitor.system.entity.Dict;
import com.streamxhub.monitor.system.service.DictService;
import com.streamxhub.monitor.base.domain.RestRequest;
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

/**
 * @author benjobs
 */
@Slf4j
@Validated
@RestController
@RequestMapping("dict")
public class DictController extends BaseController {

    private String message;

    @Autowired
    private DictService dictService;

    @PostMapping("list")
    @RequiresPermissions("dict:view")
    public Map<String, Object> DictList(RestRequest request, Dict dict) {
        return getDataTable(this.dictService.findDicts(request, dict));
    }

    @Log("新增字典")
    @PostMapping("post")
    @RequiresPermissions("dict:add")
    public void addDict(@Valid Dict dict) throws AdminXException {
        try {
            this.dictService.createDict(dict);
        } catch (Exception e) {
            message = "新增字典成功";
            log.info(message, e);
            throw new AdminXException(message);
        }
    }

    @Log("删除字典")
    @DeleteMapping("delete")
    @RequiresPermissions("dict:delete")
    public void deleteDicts(@NotBlank(message = "{required}") String dictIds) throws AdminXException {
        try {
            String[] ids = dictIds.split(StringPool.COMMA);
            this.dictService.deleteDicts(ids);
        } catch (Exception e) {
            message = "删除字典成功";
            log.info(message, e);
            throw new AdminXException(message);
        }
    }

    @Log("修改字典")
    @PutMapping("update")
    @RequiresPermissions("dict:update")
    public void updateDict(@Valid Dict dict) throws AdminXException {
        try {
            this.dictService.updateDict(dict);
        } catch (Exception e) {
            message = "修改字典成功";
            log.info(message, e);
            throw new AdminXException(message);
        }
    }

    @PostMapping("export")
    @RequiresPermissions("dict:export")
    public void export(RestRequest request, Dict dict, HttpServletResponse response) throws AdminXException {
        try {
            List<Dict> dicts = this.dictService.findDicts(request, dict).getRecords();
            ExcelKit.$Export(Dict.class, response).downXlsx(dicts, false);
        } catch (Exception e) {
            message = "导出Excel失败";
            log.info(message, e);
            throw new AdminXException(message);
        }
    }
}
