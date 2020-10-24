package com.streamxhub.console.core.service;

import com.streamxhub.console.core.entity.Note;

/**
 * @author benjobs
 */
public interface NoteBookService {
    /**
     *
     * @param note
     * @throws Exception
     */
    void submit(Note note) throws Exception;

    void submit2(Note note);
}
