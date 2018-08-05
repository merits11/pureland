package merits.funskills.pureland.model;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomUtils;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import merits.funskills.pureland.utils.NumberWordConverter;

public class PlayListUtils {

    private static final String BUCKET = "purelandmusic";

    private static final int MAX_HELP_ITEMS = 5;

    private static final Map<String, PlayList> LIST_NAME_MAPPINGS = new HashMap<>();

    private static final NumberWordConverter CONVERTER = new NumberWordConverter();

    static {
        for (PlayList playList : PlayList.values()) {
            LIST_NAME_MAPPINGS.put(playList.toString().toLowerCase(), playList);
            String[] digitStrs = splitByCamelCase(playList.toString());
            if (digitStrs.length > 1) {
                String words = Joiner.on(" ").join(digitStrs).toLowerCase();
                LIST_NAME_MAPPINGS.put(words.toLowerCase(), playList);
            }
            if (playList.getListNumber() > 0) {
                LIST_NAME_MAPPINGS.put(String.valueOf(playList.getListNumber()), playList);
                LIST_NAME_MAPPINGS.put(CONVERTER.convert(playList.getListNumber(), "").toLowerCase(),
                    playList);
            }
        }
    }

    public static String getBucket() {
        return BUCKET;
    }

    public static String getListPattern(final PlayList list) {
        return list.toString() + "/";
    }

    public static PlayList getPlaylist(final String listName) {
        if (LIST_NAME_MAPPINGS.containsKey(listName.toLowerCase())) {
            return LIST_NAME_MAPPINGS.get(listName.toLowerCase());
        }
        return null;
    }

    public static String getNumberedListsDescription(final Tag langTag) {
        StringBuffer sb = new StringBuffer("Some of the examples: ");
        List<PlayList> playLists = getPublicPositiveLists().stream()
            .filter(p -> langTag == null || p.isTagged(langTag))
            .collect(Collectors.toList());
        List<PlayList> selected = Lists.newArrayList();
        for (int i = 0; i < MAX_HELP_ITEMS; i++) {
            int index = RandomUtils.nextInt(0, playLists.size());
            PlayList playList = playLists.get(index);
            playLists.remove(index);
            selected.add(playList);
        }
        selected.sort(Comparator.comparing(PlayList::getListNumber));
        selected.forEach(playList -> sb.append(String.format("List %d, %s. ", playList.getListNumber(),
            playList.getText())));
        return sb.toString();
    }

    public static String getCardText() {
        StringBuffer sb = new StringBuffer(
            "Say 'set language' to set your language. Ask me to play music, dharma talk, sutra, " +
                ", random or a list name/number below\n");
        List<PlayList> playLists = getPublicPositiveLists();
        sb.append("Language agnostic -> ");
        Consumer<PlayList> textAppender = pl -> sb.append(String.format("%d: %s ", pl.getListNumber(), pl.getText()));
        playLists.stream().filter(pl -> pl.isTagged(Tag.English) && pl.isTagged(Tag.Chinese))
            .forEach(textAppender);
        sb.append("\nEnglish -> ");
        playLists.stream().filter(pl -> pl.isTagged(Tag.English) && !pl.isTagged(Tag.Chinese))
            .forEach(textAppender);
        sb.append("\nChinese -> ");
        playLists.stream().filter(pl -> !pl.isTagged(Tag.English) && pl.isTagged(Tag.Chinese))
            .forEach(textAppender);
        // https://s3.amazonaws.com/purelandhosting/purelandhelp.html
        //https://bit.ly/2uz8Hdd or https://goo.gl/BAz9NE
        sb.append("\nMore info: https://bit.ly/2uz8Hdd");
        return sb.toString();
    }

    public static PlayList getListByTags(final List<Tag> tags, final List<PlayList> recentPlayed) {
        List<PlayList> applicableRecentPlayed = recentPlayed.stream()
            .filter(v -> v.isTagged(tags) && v.isAccessible(tags))
            .collect(Collectors.toList());

        List<PlayList> lists = Lists.newArrayList();
        for (PlayList playList : PlayList.values()) {
            if (!playList.isAccessible(tags)) {
                continue;
            }
            if (!playList.isTagged(tags)) {
                continue;
            }
            lists.add(playList);
        }
        if (lists.isEmpty()) {
            throw new RuntimeException("Unable to select a list with tags: " +
                Joiner.on(",").join(tags));
        }

        while (applicableRecentPlayed.size() > 0 && lists.size() > 1) {
            PlayList topRecentPlayed = applicableRecentPlayed.remove(0);
            lists.remove(topRecentPlayed);
        }

        int i = RandomUtils.nextInt(0, lists.size());
        return lists.get(i);
    }

    public static List<PlayList> getPublicPositiveLists() {
        return Arrays.stream(PlayList.values())
            .filter(pl -> pl.isAccessible() && pl.getListNumber() > 0)
            .collect(Collectors.toList());
    }

    public static List<PlayList> getPublicLists() {
        return Arrays.stream(PlayList.values())
            .filter(PlayList::isAccessible)
            .collect(Collectors.toList());
    }

    public static List<PlayList> getListsByTags(final List<Tag> tags, final List<Tag> excludeTags) {
        return Stream.of(PlayList.values())
            .filter(pl -> pl.isTagged(tags))
            .filter(pl -> (excludeTags == null || excludeTags.isEmpty()) || !pl.isTagged(excludeTags))
            .collect(Collectors.toList());
    }

    public static List<PlayList> getPublicNonVirtualLists() {
        return Arrays.stream(PlayList.values())
            .filter(PlayList::isAccessible)
            .filter(pl -> !pl.isTagged(Tag.Virtual))
            .collect(Collectors.toList());
    }

    public static String[] splitByCamelCase(final String s) {
        return s.split("(?<=.)(?=\\p{Lu})");
    }

}
