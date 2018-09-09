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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.amazon.ask.dispatcher.exception.ExceptionHandler;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import merits.funskills.pureland.v2.AudioPlayHelperV2;
import merits.funskills.pureland.v2.Speeches;

@Log4j2
@RequiredArgsConstructor
public class RequestErrorHandler implements ExceptionHandler {

    private static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new JavaTimeModule());
    }

    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    private final AudioPlayHelperV2 playHelper;

    private Speeches speeches = Speeches.getSpeeches();

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
            printWriter.write("\nInput:\n" + serialize(input.getRequestEnvelope()) + "\n");
            printWriter.write("\nStack Trace:\n");
            throwable.printStackTrace(printWriter);
            printWriter.flush();
            putErrorLog(byteOutputStream);
        }
        log.error("Exception Error Happened: " + throwable.getMessage(), throwable);
        return input.getResponseBuilder()
            .withSpeech(speeches.get("error.exception"))
            .build();
    }

    private <T> String serialize(T t) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(t);
        } catch (JsonProcessingException e) {
            return t.toString();
        }
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
