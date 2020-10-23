package com.streamxhub.console.core.service;

import com.streamxhub.console.core.entity.Note;

public interface NoteBookService {
    void submit(Note note) throws Exception;

    void submit2(Note note);
}
