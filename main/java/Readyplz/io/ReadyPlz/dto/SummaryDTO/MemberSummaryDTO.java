package Readyplz.io.ReadyPlz.dto.SummaryDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
//특정 게임을 소유한 사용자 목록 조회를 위한 DTO, N+1방지를 위해 DTO프로젝션으로 사용되는 DTO클래스임.
public class MemberSummaryDTO {
    private Long id;
    private String username;
    private String nickname;
    private String country;
}


