package com.streamxhub.monitor.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.streamxhub.monitor.base.converter.TimeConverter;
import com.wuwenze.poi.annotation.Excel;
import com.wuwenze.poi.annotation.ExcelField;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("t_menu")
@Excel("菜单信息表")
public class Menu implements Serializable {

    private static final long serialVersionUID = 7187628714679791771L;

    public static final String TYPE_MENU = "0";

    public static final String TYPE_BUTTON = "1";

    public static final String DISPLAY_SHOW = "1";

    public static final String DISPLAY_NONE = "0";


    @TableId(value = "MENU_ID", type = IdType.AUTO)
    private Long menuId;

    private Long parentId;

    @NotBlank(message = "{required}")
    @Size(max = 20, message = "{noMoreThan}")
    @ExcelField(value = "名称")
    private String menuName;

    @Size(max = 50, message = "{noMoreThan}")
    @ExcelField(value = "地址")
    private String path;

    @Size(max = 100, message = "{noMoreThan}")
    @ExcelField(value = "对应Vue组件")
    private String component;

    @Size(max = 50, message = "{noMoreThan}")
    @ExcelField(value = "权限")
    private String perms;

    @ExcelField(value = "图标")
    private String icon;

    @NotBlank(message = "{required}")
    @ExcelField(value = "类型", writeConverterExp = "0=按钮,1=菜单")
    private String type;

    private Double orderNum;

    @ExcelField(value = "创建时间", writeConverter = TimeConverter.class)
    private Date createTime;

    @ExcelField(value = "修改时间", writeConverter = TimeConverter.class)
    private Date modifyTime;

    @NotBlank(message = "{required}")
    @ExcelField(value = "是否显示", writeConverterExp = "0=不显示,1=显示")
    private String display;

    private transient String createTimeFrom;
    private transient String createTimeTo;

}
