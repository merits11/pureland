package merits.funskills.pureland.model;

import static merits.funskills.pureland.model.Tag.Type.Access;
import static merits.funskills.pureland.model.Tag.Type.Composition;
import static merits.funskills.pureland.model.Tag.Type.Content;
import static merits.funskills.pureland.model.Tag.Type.Language;
import static merits.funskills.pureland.model.Tag.Type.ListType;
import static merits.funskills.pureland.model.Tag.Type.Navigation;
import static merits.funskills.pureland.model.Tag.Type.Sort;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Tag {

    //Content
    Sutra(Content),
    Music(Content),
    Chanting(Content),
    DharmaTalk(Content),
    Pop(Content),
    BabyStuff(Content),

    //Sort
    SortByKey(Sort),
    SortByTimeAsc(Sort),
    SortByTimeAndKeyAsc(Sort),
    SortByTimeDesc(Sort),
    SortByEtag(Sort),

    //Access
    Private(Access),

    FastForward(Navigation),
    //
    Virtual(ListType),
    Combine(Composition),

    //Lang
    Chinese(Language),
    English(Language),
    AllLanguages(Language),
    None(Language);

    @Getter
    private final Type tagType;

    public static List<Tag> TAG(Tag... tags) {
        Set<Tag> tagSet = Sets.newHashSet(tags);
        tagSet.add(AllLanguages);
        return tagSet.stream().collect(Collectors.toList());
    }

    public boolean isContent() {
        return tagType == Content;
    }

    public boolean isLanguage() {
        return tagType == Language;
    }

    public boolean isAccess() {
        return tagType == Access;
    }

    public boolean isComposition() {
        return tagType == Composition;
    }

    public boolean isSort() {
        return tagType == Sort;
    }

    enum Type {
        Content,
        Language,
        Access,
        Sort,
        Composition,
        Navigation,
        ListType;
    }
}
