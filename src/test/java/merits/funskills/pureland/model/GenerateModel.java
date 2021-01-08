package merits.funskills.pureland.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;
import static merits.funskills.pureland.model.PlayListUtils.splitByCamelCase;
import static org.junit.Assert.assertTrue;

@Log4j2
public class GenerateModel {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String modelFile = "configuration/model.json";
    private static final String manifestFile = "skill.json";
    private static final String descriptionFile = "configuration/description.txt";
    private static final String htmlIndex = "configuration/purelandhelp.html";
    private static final String[] LOCALES = new String[]{
            "en-US",
            "en-CA",
            "en-IN",
            "en-AU",
            "en-GB"

    };

    private JsonNode getModel() throws Exception {
        try (FileReader fileReader = new FileReader(modelFile)) {
            return objectMapper.readTree(fileReader);
        }
    }

    private JsonNode getManifest() throws Exception {
        try (FileReader fileReader = new FileReader(manifestFile)) {
            return objectMapper.readTree(fileReader);
        }
    }

    private String getDescription() throws Exception {
        try (FileReader fileReader = new FileReader(descriptionFile, StandardCharsets.UTF_8)) {
            return IOUtils.toString(fileReader);
        }
    }

    private void writeJson(JsonNode newJson, JsonNode current, String dest) throws Exception {
        if (newJson.equals(current)) {
            log.info("Json file not unchanged, skip writing.");
            return;
        }
        log.info("Writing new json to {}", dest);
        try (FileWriter fileWriter = new FileWriter(dest)) {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(fileWriter, newJson);
        }
    }

    @Test
    public void updateManifest() {
        try {
            JsonNode manifest = getManifest();
            String description = getDescription();
            assertTrue("Description is max to 4000 characters.", description.length() <= 4000);
            log.info("Description length: {}", description.length());
            ObjectNode locales = (ObjectNode) manifest.get("manifest").get("publishingInformation").get("locales");
            Iterator<String> fieldNames = locales.fieldNames();
            while (fieldNames.hasNext()) {
                String locale = fieldNames.next();
                ObjectNode localeNode = (ObjectNode) locales.get(locale);
                String currentDescription = localeNode.get("description").textValue();
                if (!currentDescription.equals(description)) {
                    log.info("Updating {} description", locale);
                    localeNode.set("description", instance.textNode(description));
                }
            }
            writeJson(manifest, getManifest(), manifestFile);
        } catch (Exception error) {
            log.warn("This is not actual test, don't fail!", error);
        }
    }

    @Test
    public void updateModel() {
        try {
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
            writeJson(model, getModel(), modelFile);
            for (String locale : LOCALES) {
                FileUtils.copyFile(new File(modelFile), new File("interactionModel/custom/" + locale + ".json"));
            }
        } catch (Exception error) {
            log.warn("This is not actual test, don't fail!", error);
        }
    }

    private void processName(String name, ArrayNode types, ObjectNode customNameIntent,
                             ArrayNode dialogCustomNameSlots) throws IOException {
        ObjectNode thisType = findByName(types, name);
        if (thisType == null) {
            ObjectNode objectNode = instance.objectNode();
            objectNode.set("name", instance.textNode(name));
            objectNode.set("values", objectMapper.readTree("[\n"
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
        Arrays.stream(new String[]{"", "play ", "uphold "}).forEach(s -> {
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

    @Test
    public void generateIndex() {
        try {
            Document document = Jsoup.parse(new File(htmlIndex), StandardCharsets.UTF_8.toString());
            Element listBody = document.getElementById("list_body");
            for (PlayList pl : PlayListUtils.getPublicLists()) {
                int listNum = pl.getListNumber();
                Element currentElement = listBody.getElementById("tr" + pl.getListNumber());
                String listDescription = "";
                String references = "";
                String langs = Joiner.on(", ").join(
                        pl.getTags().stream()
                                .filter(tag -> tag.isContent() || tag.isLanguage())
                                .filter(tag -> tag != Tag.AllLanguages)
                                .sorted()
                                .collect(Collectors.toList()));
                if (currentElement != null) {
                    listDescription = currentElement.getElementById("des" + pl.getListNumber()).html();
                    references = currentElement.getElementById("ref" + pl.getListNumber()).html();
                }
                String html = String.format(
                        "<td id='num%s'>%s</td> <td>%s</td> <td>%s</td> <td id='des%s'>%s</td> <td id='ref%s'>%s</td>",
                        listNum, listNum, pl.getText(), langs, listNum, listDescription, listNum, references);
                Element element = new Element("tr");
                element.attr("id", "tr" + listNum);
                element.html(html);
                if (currentElement == null) {
                    listBody.appendChild(element);
                } else {
                    currentElement.replaceWith(element);
                }
            }
            try (FileWriter fw = new FileWriter(htmlIndex)) {
                fw.write(document.outerHtml());
            }
        } catch (Exception error) {
            log.warn("This is not actual test, don't fail!", error);
        }
    }
}
