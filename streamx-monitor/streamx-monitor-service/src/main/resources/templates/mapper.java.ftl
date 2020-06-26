package ${package.Mapper};

import ${package.}.${entity};
import ${superMapperClassPackage};

/**
* @author ${author}
*/
<#if kotlin>
    interface ${table.mapperName} : ${superMapperClass}<${entity}>
<#else>
    public interface ${table.mapperName} extends ${superMapperClass}<${entity}> {

    }
</#if>
