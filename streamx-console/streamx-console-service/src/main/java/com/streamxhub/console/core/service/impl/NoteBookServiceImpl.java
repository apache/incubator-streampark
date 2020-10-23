package com.streamxhub.console.core.service.impl;

import com.streamxhub.console.core.entity.Note;
import com.streamxhub.console.core.service.NoteBookService;
import com.streamxhub.repl.flink.interpreter.FlinkInterpreter;
import lombok.extern.slf4j.Slf4j;
import org.apache.zeppelin.display.AngularObjectRegistry;
import org.apache.zeppelin.interpreter.*;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Properties;
import static org.junit.Assert.assertEquals;

@Slf4j
@Service("noteBookService")
public class NoteBookServiceImpl implements NoteBookService {

    private FlinkInterpreter interpreter;
    private AngularObjectRegistry angularObjectRegistry;

    @PostConstruct
    public void setUp() throws InterpreterException {
        Properties prop = new Properties();
        prop.setProperty("zeppelin.flink.printREPLOutput", "true");
        prop.setProperty("zeppelin.flink.scala.color", "false");
        prop.setProperty("flink.execution.mode", "local");
        prop.setProperty("local.number-taskmanager", "4");
        interpreter = new FlinkInterpreter(prop);
        InterpreterGroup interpreterGroup = new InterpreterGroup();
        interpreter.setInterpreterGroup(interpreterGroup);
        interpreter.open();
        angularObjectRegistry = new AngularObjectRegistry("flink", null);
    }

    private InterpreterContext getInterpreterContext() {
        InterpreterContext context = InterpreterContext.builder()
                .setParagraphId("paragraphId")
                .setInterpreterOut(new InterpreterOutput(null))
                .setAngularObjectRegistry(angularObjectRegistry)
                //.setIntpEventClient(mock(RemoteInterpreterEventClient.class))
                .build();
        InterpreterContext.set(context);
        return context;
    }

    @Override
    public void submit(Note note) throws Exception {
        InterpreterContext context = getInterpreterContext();
        InterpreterResult result = interpreter.interpret(note.getSourceCode(), context);
        System.out.println(context.out.toString());
        assertEquals(InterpreterResult.Code.SUCCESS, result.code());
    }

    @Override
    public void submit2(Note note) {

    }
}
