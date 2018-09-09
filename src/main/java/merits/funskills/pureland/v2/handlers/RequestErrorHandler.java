package merits.funskills.pureland.v2.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;

import com.amazon.ask.dispatcher.exception.ExceptionHandler;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.services.Serializer;
import com.amazon.ask.util.JacksonSerializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import merits.funskills.pureland.v2.AudioPlayHelperV2;
import merits.funskills.pureland.v2.Speeches;

@Log4j2
@RequiredArgsConstructor
public class RequestErrorHandler implements ExceptionHandler {

    private Speeches speeches = Speeches.getSpeeches();
    private Serializer serializer = new JacksonSerializer();
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    private final AudioPlayHelperV2 playHelper;

    @Override
    public boolean canHandle(HandlerInput input, Throwable throwable) {
        return true;
    }

    @Override
    public Optional<Response> handle(HandlerInput input, Throwable throwable) {
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(byteOutputStream, StandardCharsets.UTF_8);
        try (PrintWriter printWriter = new PrintWriter(outputStreamWriter)) {
            printWriter.write("Time: " + new Date() + "\n");
            printWriter.write("Input: \n" + serializer.serialize(input.getRequestEnvelope()) + "\n");
            printWriter.write("Stack Trace:\n");
            throwable.printStackTrace(printWriter);
            printWriter.flush();
            putErrorLog(byteOutputStream);
        }
        log.error("Exception Error Happened: " + throwable.getMessage(), throwable);
        return input.getResponseBuilder()
            .withSpeech(speeches.get("error.exception"))
            .build();
    }

    private void putErrorLog(ByteArrayOutputStream byteOutputStream) {
        byte[] bytes = byteOutputStream.toByteArray();
        Date now = new Date();
        try {
            String contents = IOUtils.toString(bytes, "UTF-8");
            String logKey = "Errors/" + simpleDateFormat.format(now) + "/" + dateTimeFormat.format(now) + "-" +
                RandomStringUtils.randomAlphanumeric(4) + ".txt";
            playHelper.putObjectToCodeBucket(logKey, contents);
        } catch (IOException e) {
            log.error("Failed to put error log.", e);
        }

    }
}
