package com.streamxhub.console.core.service.impl;

import com.streamxhub.console.core.entity.Note;
import com.streamxhub.console.core.service.NoteBookService;
import com.streamxhub.repl.flink.interpreter.FlinkInterpreter;
import lombok.extern.slf4j.Slf4j;
import org.apache.zeppelin.display.AngularObjectRegistry;
import org.apache.zeppelin.interpreter.*;
import org.apache.zeppelin.interpreter.remote.RemoteInterpreterEventClient;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import static org.mockito.Mockito.mock;

import java.util.Properties;
import java.util.concurrent.Executors;

/**
 * @author benjobs
 */
@Slf4j
@Service("noteBookService")
public class NoteBookServiceImpl implements NoteBookService {

    private Properties properties = new Properties();

    @PostConstruct
    public void initProperty() {
        properties.setProperty("repl.out", "true");
        properties.setProperty("scala.color", "true");
        properties.setProperty("flink.execution.mode", "local");
    }

    @Override
    public void submit(Note note) {
        Executors.newSingleThreadExecutor().submit(() -> {
            FlinkInterpreter interpreter = new FlinkInterpreter(properties);
            InterpreterGroup interpreterGroup = new InterpreterGroup();
            interpreter.setInterpreterGroup(interpreterGroup);
            try {
                interpreter.open();
                AngularObjectRegistry angularObjectRegistry = new AngularObjectRegistry("flink", null);
                InterpreterContext context = InterpreterContext.builder()
                        .setParagraphId("paragraphId")
                        .setAngularObjectRegistry(angularObjectRegistry)
                        .setIntpEventClient(mock(RemoteInterpreterEventClient.class))
                        .setInterpreterOut(new InterpreterOutput(null))
                        .build();
                InterpreterContext.set(context);
                InterpreterResult result = interpreter.interpret(note.getSourceCode(), context);
                System.out.println(context.out.toString());
                assert InterpreterResult.Code.SUCCESS.equals(result.code());
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                if (interpreter != null) {
                    try {
                        interpreter.close();
                    } catch (InterpreterException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void submit2(Note note) {

    }
}
