package io.readyplz.readyplz.dto.summary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameCollectionItemDTO {
    private Long id;
    private String name;
    private boolean userHasGame;
}
