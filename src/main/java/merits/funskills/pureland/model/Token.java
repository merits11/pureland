package merits.funskills.pureland.model;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Token {

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
}
