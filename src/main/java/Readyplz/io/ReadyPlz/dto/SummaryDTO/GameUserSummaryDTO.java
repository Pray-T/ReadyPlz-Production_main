package Readyplz.io.ReadyPlz.dto.SummaryDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
//여러 게임에 대해, 각 게임을 가진 회원 정보를 한 번의 쿼리로 조회를 위한 DTO, N+1방지를 위해 DTO프로젝션으로 사용되는 DTO클래스임.
public class GameUserSummaryDTO {
    private Long gameId;
    private Long memberId;
    private String username;
    private String nickname;
    private String country;
}


