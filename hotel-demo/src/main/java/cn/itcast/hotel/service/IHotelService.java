package cn.itcast.hotel.service;

import cn.itcast.hotel.params.PageResult;
import cn.itcast.hotel.params.SearchParams;
import cn.itcast.hotel.pojo.Hotel;
import com.baomidou.mybatisplus.extension.service.IService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface IHotelService extends IService<Hotel> {
    PageResult search(SearchParams searchParams) throws IOException;

    Map<String, List<String>> getFilters(SearchParams searchParams);

    List<String> getSuggestion(String key);

    void deleteById(Long id);

    void insertById(Long id);
}
