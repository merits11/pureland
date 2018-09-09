package merits.funskills.pureland.model;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Token {

    //4b19fd6f-887e-4580-98a3-827af31bff06,Ten,0
    private static final Pattern TOKEN_FORMAT=Pattern.compile("[\\w-]+,\\w+,\\d+.*");

    private String uuid;
    private String listName;
    private int listSequence;
    private Integer repeatSeq;

    public String getStreamToken() {
        if (repeatSeq == null) {
            return String.format("%s,%s,%d",
                    uuid,
                    listName,
                    listSequence);
        }
        return String.format("%s,%s,%d,%d",
                uuid,
                listName,
                listSequence,
                repeatSeq);
    }

    public static Token fromPlayState(final PlayState playState) {
        return new TokenBuilder()
                .uuid(playState.getToken())
                .listName(playState.getCurrentList())
                .listSequence(playState.getCurrentSeq())
                .repeatSeq(playState.getRepeatSeq())
                .build();
    }

    public static Token newToken(final PlayItem playItem) {
        UUID uuid = UUID.randomUUID();
        return new TokenBuilder()
                .listName(playItem.getListName().toString())
                .listSequence(playItem.getSeqNo())
                .uuid(uuid.toString())
                .build();
    }

    public static Token fromStreamToken(@NonNull final String streamToken) {
        String[] parts = streamToken.split(",");
        int length = parts.length;
        Preconditions.checkState(length >= 3, "Not a valid token " + streamToken);
        return new TokenBuilder()
                .uuid(parts[0])
                .listName(parts[1])
                .listSequence(Integer.valueOf(parts[2]))
                .repeatSeq((length == 3) ? null : Integer.valueOf(parts[3]))
                .build();
    }

    public static boolean isValidToken(final String streamToken) {
        if (StringUtils.isBlank(streamToken)) {
            return false;
        }
        Matcher matcher = TOKEN_FORMAT.matcher(streamToken);
        return matcher.matches();
    }
}
