package de.tum.in.www1.artemis.web.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A DTO representing the testdata course.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record TestdataCourseDTO(String title, String shortName) {
}
