package com.streamxhub.console.core.controller;


import com.streamxhub.console.core.entity.Note;
import com.streamxhub.console.core.service.NoteBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("flink/notebook")
public class NoteBookController {

    @Autowired
    private NoteBookService noteBookService;

    @PostMapping("submit")
    public void submit(Note Note) {
        noteBookService.submit(Note);
    }

    @PostMapping("submit2")
    public void submit2(Note Note) {
        noteBookService.submit2(Note);
    }

}
