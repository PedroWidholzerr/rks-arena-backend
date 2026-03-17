package com.rks.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerDTO {
    private String id;
    private String nickname;
    private String tag;
    private String teamId;
    private Boolean isCaptain;
    private String notes;
}
