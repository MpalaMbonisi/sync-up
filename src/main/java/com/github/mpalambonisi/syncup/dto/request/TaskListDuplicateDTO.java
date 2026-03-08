package com.github.mpalambonisi.syncup.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskListDuplicateDTO {
    private String newTitle; // Optional - if not provided, auto generate with "(Copy)" suffix
}
