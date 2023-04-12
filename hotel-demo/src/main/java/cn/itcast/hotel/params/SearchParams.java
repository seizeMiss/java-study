package cn.itcast.hotel.params;

import lombok.Data;

@Data
public class SearchParams {
    private String key;
    private Integer page;
    private Integer size;
    private String brand;
    private String city;
    private Integer minPrice;
    private Integer maxPrice;
    private String starName;
    private String sortBy;

    private String location;
}
