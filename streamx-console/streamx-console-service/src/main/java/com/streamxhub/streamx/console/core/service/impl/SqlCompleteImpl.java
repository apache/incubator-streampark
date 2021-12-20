package com.streamxhub.streamx.console.core.service.impl;

import com.streamxhub.streamx.console.core.entity.FstTree;
import com.streamxhub.streamx.console.core.service.SqlComplete;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author john
 * @time 2021.12.20
 */
@Slf4j
@Service
public class SqlCompleteImpl implements SqlComplete {
    private static final Set<Character> blackSet = new HashSet<Character>() {{
        add(' ');
        add(';');
    }};
    @Autowired
    FstTree fstTree;


    @Override
    public List<String> getComplete(String sql) {
        // 空格不需要提示
        if (sql.length() > 0 && blackSet.contains(sql.charAt(sql.length() - 1))) {
            return new ArrayList<>();
        }
        String[] temp = sql.split("\\s");
        return fstTree.getComplicate(temp[temp.length - 1]);
    }
}
