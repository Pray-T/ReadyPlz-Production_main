package Readyplz.io.ReadyPlz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
 

@Data
@NoArgsConstructor
@AllArgsConstructor
//클라이언트에게 전달할 최종 DTO입니다.
public class SteamGameDetailDTO {
    private Integer appId;
    private String name;
    private Integer releaseYear;
    private String headerImageUrl;
}
