package merits.funskills.pureland.model;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Set;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

import lombok.extern.log4j.Log4j2;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;
import static merits.funskills.pureland.model.PlayListUtils.splitByCamelCase;

@Log4j2
public class GenerateModel {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String modelFile = "configuration/model.json";

    private JsonNode getModel() throws Exception {
        try (FileReader fileReader = new FileReader(modelFile)) {
            return objectMapper.readTree(fileReader);
        }
    }

    private void writeModel(JsonNode model) throws Exception {
        JsonNode current = getModel();
        if (model.equals(current)) {
            log.info("Model unchanged, skip writing.");
            return;
        }
        log.info("Writing new model to {}", modelFile);
        try (FileWriter fileWriter = new FileWriter(modelFile)) {
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(fileWriter, model);
        }
    }

    @Test
    public void populateNames() throws Exception {
        JsonNode model = getModel();
        ArrayNode types = (ArrayNode) model.get("interactionModel").get("languageModel").get("types");
        ArrayNode intents = (ArrayNode) model.get("interactionModel").get("languageModel").get("intents");
        ArrayNode dialogIntents = (ArrayNode) model.get("interactionModel").get("dialog").get("intents");
        ObjectNode customNameIntent = findByName(intents, "CustomNameIntent");
        ArrayNode dialogCustomNameSlots = (ArrayNode) findByName(dialogIntents, "CustomNameIntent").get("slots");

        NameMapping.getNames().forEach(name -> {
            try {
                processName(name, types, customNameIntent, dialogCustomNameSlots);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        writeModel(model);
    }

    private void processName(String name, ArrayNode types, ObjectNode customNameIntent,
        ArrayNode dialogCustomNameSlots) throws IOException {
        ObjectNode thisType = findByName(types, name);
        if (thisType == null) {
            ObjectNode objectNode = instance.objectNode();
            objectNode.set("name", instance.textNode(name));
            objectNode.set("value", objectMapper.readTree("[\n"
                + "            {\n"
                + "              \"name\": {\n"
                + "                \"value\": \"" + Joiner.on(" ").join(splitByCamelCase(name)) + "\"\n"
                + "              }\n"
                + "            }\n"
                + "          ]"));
            types.add(objectNode);
            log.info("Adding new type: {}", objectNode);
        }
        if (findByName(dialogCustomNameSlots, name) == null) {
            dialogCustomNameSlots.add(
                objectMapper.readTree("{\n"
                    + "              \"name\": \"" + name + "\",\n"
                    + "              \"type\": \"" + name + "\",\n"
                    + "              \"elicitationRequired\": false,\n"
                    + "              \"confirmationRequired\": false\n"
                    + "            }")
            );
            log.info("Adding dialog slot");
        }
        ArrayNode customNameSlots = (ArrayNode) customNameIntent.get("slots");
        ArrayNode customNameSamples = (ArrayNode) customNameIntent.get("samples");
        if (findByName(customNameSlots, name) == null) {
            customNameSlots.add(objectMapper.readTree("{\n"
                + "              \"name\": \"" + name + "\",\n"
                + "              \"type\": \"" + name + "\"\n"
                + "            }"));
            log.info("Adding intent slot");
        }
        Set<String> currentSamples = Sets.newHashSet();
        customNameSamples.forEach(s -> currentSamples.add(s.textValue()));
        Arrays.stream(new String[] { "", "play ", "uphold " }).forEach(s -> {
            String sample = s + "{" + name + "}";
            if (!currentSamples.contains(sample)) {
                customNameSamples.add(sample);
            }
        });
    }

    private ObjectNode findByName(ArrayNode parent, String name) {
        for (JsonNode node : parent) {
            if (node.get("name").textValue().equals(name)) {
                return (ObjectNode) node;
            }
        }
        return null;
    }
}
