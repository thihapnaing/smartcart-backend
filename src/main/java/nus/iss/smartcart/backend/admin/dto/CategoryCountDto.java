package nus.iss.smartcart.backend.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// AUTHOR: Htet Nandar(Grace)
// One bar in the dashboard's "By Category" breakdown.
@Getter
@AllArgsConstructor
public class CategoryCountDto {
    private String categoryName;
    private long count;
}
