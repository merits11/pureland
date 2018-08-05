package merits.funskills.pureland.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import merits.funskills.pureland.model.PlayItem;
import merits.funskills.pureland.model.PlayList;
import merits.funskills.pureland.model.PlayListUtils;
import merits.funskills.pureland.model.Tag;

import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;
import static merits.funskills.pureland.model.PlayListUtils.getListsByTags;
import static merits.funskills.pureland.model.Tag.Virtual;

@Log4j2
@RequiredArgsConstructor
public class PlayListManager {

    private static Map<PlayList, List<PlayItem>> library = new HashMap<>();
    private static final long HALF_MB = 512 * 1024;
    private static final int MIN_LIST_SIZE = 4;

    private final AmazonS3 s3Client;

    public void updateLibrary() {
        log.debug("Updating cache...");
        PlayListUtils.getPublicNonVirtualLists().forEach(pl -> {
            library.put(pl, fetchListItems(pl));
        });
        PlayListUtils.getListsByTags(Lists.newArrayList(Virtual), null).forEach(pl -> {
            library.put(pl, buildVirtualList(pl));
        });
        log.debug("Cache was updated.");
    }

    public List<PlayItem> getListItems(final PlayList playList) {
        if (!library.containsKey(playList)) {
            if (playList.isTagged(Virtual)) {
                library.put(playList, buildVirtualList(playList));
            } else {
                library.put(playList, fetchListItems(playList));
            }
        }
        return library.get(playList);
    }

    private boolean validS3Item(final S3ObjectSummary s3ObjectSummary) {
        return s3ObjectSummary.getSize() >= HALF_MB;
    }

    private List<PlayItem> fetchListItems(final PlayList playList) {
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
            .withBucketName(PlayListUtils.getBucket())
            .withPrefix(PlayListUtils.getListPattern(playList));
        ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);
        List<S3ObjectSummary> s3Results = new ArrayList<>();
        while (objectListing != null) {
            s3Results.addAll(
                objectListing.getObjectSummaries().stream()
                    .filter(this::validS3Item)
                    .collect(Collectors.toList())
            );
            if (!objectListing.isTruncated()) {
                break;
            }
            listObjectsRequest.setMarker(objectListing.getMarker());
            objectListing = s3Client.listObjects(listObjectsRequest);
        }
        log.debug("{} items loaded from S3 for play list {}", s3Results.size(), playList);
        sort(s3Results, playList.getTags().stream().filter(Tag::isSort).findAny());
        log.debug("Play list with the order:\n {}", Joiner.on(" -> \n").join(
            s3Results.stream().map(S3ObjectSummary::getKey).collect(Collectors.toList())
        ));
        List<PlayItem> items = toPlayItems(s3Results, playList);
        while (items.size() > 0 && items.size() < MIN_LIST_SIZE) {
            List<PlayItem> replica = Lists.newArrayList();
            int seq = items.size();
            for (int i = 0; i < items.size(); i++) {
                PlayItem newItem = items.get(i).toBuilder().seqNo(seq + i).build();
                replica.add(newItem);
            }
            items.addAll(replica);
            log.debug("{} items extended to {}", playList, items.size());
        }
        return items;
    }

    private List<PlayItem> toPlayItems(final List<S3ObjectSummary> s3ObjectSummaries,
        final PlayList playList) {
        final List<PlayItem> resultPlayItems = new ArrayList<>();
        for (int i = 0; i < s3ObjectSummaries.size(); i++) {
            resultPlayItems.add(PlayItem.fromS3Object(
                s3ObjectSummaries.get(i),
                playList,
                i
            ));
        }
        return resultPlayItems;
    }

    private List<PlayItem> buildVirtualList(final PlayList playList) {
        Preconditions.checkState(playList.isTagged(Virtual));
        List<Tag> applicableTags = playList.getTags().stream()
            .filter(t -> t.isContent() || t.isLanguage())
            .collect(Collectors.toList());
        Tag composition = playList.getTags().stream()
            .filter(Tag::isComposition)
            .findAny().get();
        Optional<Tag> sortTag = playList.getTags().stream()
            .filter(Tag::isSort)
            .findAny();
        List<S3ObjectSummary> resultListItems = new ArrayList<>();
        List<PlayList> taggedLists = getListsByTags(applicableTags, ImmutableList.of(Virtual));
        switch (composition) {
            case Combine:
                for (PlayList tagList : taggedLists) {
                    resultListItems.addAll(
                        getListItems(tagList).stream()
                            .map(PlayItem::getS3ObjectSummary)
                            .collect(Collectors.toList())
                    );
                }
                break;
        }
        resultListItems = dedupe(resultListItems);
        sort(resultListItems, sortTag);
        return toPlayItems(resultListItems, playList);
    }

    private List<S3ObjectSummary> dedupe(final List<S3ObjectSummary> s3ObjectSummaries) {
        Set<String> eTags = Sets.newHashSet();
        List<S3ObjectSummary> resultList = Lists.newArrayList();
        for (S3ObjectSummary s3ObjectSummary : s3ObjectSummaries) {
            if (!eTags.contains(s3ObjectSummary.getETag())) {
                eTags.add(s3ObjectSummary.getETag());
                resultList.add(s3ObjectSummary);
            }
        }
        return resultList;
    }

    private void sort(final List<S3ObjectSummary> s3ObjectSummaries,
        final Optional<Tag> sortTagOptional) {
        if (!sortTagOptional.isPresent()) {
            return;
        }
        switch (sortTagOptional.get()) {
            case SortByTimeDesc:
                s3ObjectSummaries.sort(comparing(S3ObjectSummary::getLastModified, reverseOrder()));
                break;
            case SortByTimeAsc:
                s3ObjectSummaries.sort(comparing(S3ObjectSummary::getLastModified));
                break;
            case SortByEtag:
                s3ObjectSummaries.sort(comparing(S3ObjectSummary::getETag));
                break;
            case SortByKey:
                s3ObjectSummaries.sort(comparing(S3ObjectSummary::getKey));
                break;
            default:
                s3ObjectSummaries.sort(comparing(S3ObjectSummary::getKey));
                break;
        }
    }
}
