package merits.funskills.pureland.model;

import org.apache.commons.lang3.StringUtils;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Builder(toBuilder = true)
@EqualsAndHashCode
@DynamoDBTable(tableName = "TO_BE_REPLACED")
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor
public class UserSetting {

    private String userId;
    private String language;
    private String lastHeardVersion;
    private Integer heardTimes;

    @DynamoDBHashKey(attributeName = "token")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @DynamoDBAttribute
    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @DynamoDBAttribute
    public String getLastHeardVersion() {
        return lastHeardVersion;
    }

    @DynamoDBAttribute
    public Integer getHeardTimes() {
        return heardTimes;
    }

    public void setHeardTimes(Integer heardTimes) {
        this.heardTimes = heardTimes;
    }

    public void setLastHeardVersion(String lastHeardVersion) {
        this.lastHeardVersion = lastHeardVersion;
    }

    public Tag languageTag() {
        if (language != null && StringUtils.isNotEmpty(language)) {
            return Tag.valueOf(language);
        }
        return null;
    }
}
