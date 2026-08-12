package nus.iss.smartcart.backend.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// AUTHOR: Htet Nandar(Grace)
// One tile in the dashboard's "Gender Split" summary.
@Getter
@AllArgsConstructor
public class GenderCountDto {
    private String gender;
    private long count;
    private double percentage;
}
