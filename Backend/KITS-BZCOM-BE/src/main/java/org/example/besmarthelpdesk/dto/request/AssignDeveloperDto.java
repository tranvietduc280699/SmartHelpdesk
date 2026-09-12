package org.example.besmarthelpdesk.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignDeveloperDto {
    /**
     * Optional developer ID. If null, the system automatically runs the auto-assignment algorithm.
     */
    private UUID developerId;
}
