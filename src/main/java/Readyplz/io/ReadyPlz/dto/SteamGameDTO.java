package Readyplz.io.ReadyPlz.dto;

import Readyplz.io.ReadyPlz.domain.Game; 
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SteamGameDTO {
    @JsonAlias({"appid", "appId"})
    private Long appId;
    
    private String name;
    
    @JsonAlias({"header_image", "headerImage"})
    private String headerImage;
    @JsonAlias({"release_date", "releaseDate"})
    private String releaseDate;

    // DateTimeFormatter는 여러 번 생성할 필요 없이 재사용 가능합니다.
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM, uuuu", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_FORMATTER_ALT = DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH);

    /**
     * Steam API의 JsonNode로부터 DTO 객체를 생성합니다.
     * @param dataNode 게임 상세 정보가 담긴 JsonNode ("data" 객체)
     * @return SteamGameDTO 객체
     */
    public static SteamGameDTO fromJsonNode(JsonNode dataNode) {
        Long appId = dataNode.path("steam_appid").asLong();
        String name = dataNode.path("name").asText();
        String headerImage = dataNode.path("header_image").asText();
        String releaseDate = dataNode.path("release_date").path("date").asText();
        return new SteamGameDTO(appId, name, headerImage, releaseDate);
    }

    /**
     * DTO 객체를 DB에 저장할 Game 엔티티로 변환합니다.
     * 필드 타입과 이름 불일치를 여기서 해결합니다.
     * @return Game 엔티티 객체
     */
    public Game toEntity() {
        Integer appid = null;
        if (this.appId != null) {
            appid = this.appId.intValue();
        }

        LocalDate parsedDate = null;
        try {
            if (this.releaseDate != null && !this.releaseDate.isEmpty() && !"TBA".equalsIgnoreCase(this.releaseDate) && !"Coming Soon".equalsIgnoreCase(this.releaseDate)) {
                try {
                    parsedDate = LocalDate.parse(this.releaseDate, DATE_FORMATTER);
                } catch (DateTimeParseException ex) {
                    parsedDate = LocalDate.parse(this.releaseDate.replace(",", ""), DATE_FORMATTER_ALT);
                }
            }
        } catch (DateTimeParseException e) {
            log.warn("날짜 파싱 실패. AppID: {}, DateString: '{}'", this.appId, this.releaseDate);
        }

        return Game.builder()
                .appid(appid)
                .name(this.name)
                .headerImageUrl(this.headerImage)
                .releaseDate(parsedDate)
                .build();
    }
}