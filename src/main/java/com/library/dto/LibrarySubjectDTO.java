package com.library.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibrarySubjectDTO {
    private Long id;
    private Long libraryId;
    private String name;
    private Long bookCount;
    private LocalDateTime createdAt;
}
